package com.zriyo.aicodemother.util;

import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.cglib.beans.BeanCopier;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * BeanCopyUtil（CGLIB + 深拷贝版）
 *
 * - 高性能：主拷贝使用 CGLIB BeanCopier（cached）
 * - 深拷贝：支持 List/Set/Map/Array/Bean（递归）
 * - 保持原有 API 风格与方法名（可直接替换）
 *
 * 注意：
 * - 如果集合元素为接口或父类类型，deep copy 会尝试按运行时类型拷贝
 * - 循环引用没有做保护（若需要，可加入引用记录表）
 */
public class BeanCopyUtil {

    /**
     * copierCache 缓存：key = sourceClassName + "->" + targetClassName
     * value = BiConsumer<Object source, Object target>，用于执行拷贝
     */
    private static final Map<String, BiConsumer<Object, Object>> copierCache = new ConcurrentHashMap<>();

    /**
     * 字段缓存：提高反射字段查找性能
     */
    private static final Map<Class<?>, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();

    /* -----------------------
     * 公共 API（与原版方法名一致）
     * ----------------------- */

    /**
     * 批量转换并可对每个结果做后处理（postProcessor）
     */
    public static <S, T> List<T> convertToList(List<S> sourceList, Class<T> targetClass, Consumer<T> postProcessor) {
        if (sourceList == null) return Collections.emptyList();
        return sourceList.stream()
                .map(s -> {
                    T target = copy(s, targetClass);
                    if (postProcessor != null && target != null) postProcessor.accept(target);
                    return target;
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建并返回目标对象的拷贝
     */
    @SneakyThrows
    public static <S, T> T copy(S source, Class<T> targetClass) {
        if (source == null) return null;
        T target = targetClass.getDeclaredConstructor().newInstance();
        copy(source, target);
        return target;
    }

    /**
     * 拷贝到已有对象（保留原方法签名）
     */
    public static <S, T> void copy(S source, T target) {
        copy(source, target, null);
    }

    /**
     * 增加一个支持后处理的重载（内部使用）
     */
    public static <S, T> void copy(S source, T target, Consumer<T> postProcessor) {
        if (source == null || target == null) return;
        getOrCreateCopier(source.getClass(), target.getClass()).accept(source, target);
        if (postProcessor != null) postProcessor.accept(target);
    }

    /**
     * 列表拷贝
     */
    public static <S, T> List<T> copyList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) return Collections.emptyList();
        return sourceList.stream().map(s -> copy(s, targetClass)).collect(Collectors.toList());
    }

    /**
     * Set 拷贝
     */
    public static <S, T> Set<T> copySet(Set<S> sourceSet, Class<T> targetClass) {
        if (sourceSet == null) return Collections.emptySet();
        return sourceSet.stream().map(s -> copy(s, targetClass)).collect(Collectors.toSet());
    }

    /**
     * Map value 拷贝（key 保留原样）
     */
    public static <K, V, T> Map<K, T> copyMap(Map<K, V> sourceMap, Class<T> targetClass) {
        if (sourceMap == null) return Collections.emptyMap();
        Map<K, T> targetMap = new HashMap<>();
        for (Map.Entry<K, V> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), copy(entry.getValue(), targetClass));
        }
        return targetMap;
    }

    /* -----------------------
     * Copier 生成与缓存（内部）
     * ----------------------- */

    private static BiConsumer<Object, Object> getOrCreateCopier(Class<?> sourceClass, Class<?> targetClass) {
        String key = sourceClass.getName() + "->" + targetClass.getName();
        return copierCache.computeIfAbsent(key, k -> {
            try {
                return generateCopier(sourceClass, targetClass);
            } catch (Exception e) {
                throw new RuntimeException("生成拷贝器失败，源类：" + sourceClass.getName() + "，目标类：" + targetClass.getName(), e);
            }
        });
    }

    /**
     * 生成 copier：
     * - 使用 CGLIB BeanCopier 做主拷贝（字段名相同且类型可赋值的场景）
     * - 之后特别处理集合、Map、数组字段进行深拷贝（递归）
     */
    private static BiConsumer<Object, Object> generateCopier(Class<?> sourceClass, Class<?> targetClass) {
        // CGLIB BeanCopier（fast）
        final BeanCopier beanCopier = BeanCopier.create(sourceClass, targetClass, false);

        // 预取字段映射（name -> Field）以便快速处理集合/数组/Map 字段
        final Map<String, Field> sourceFields = getAllFields(sourceClass);
        final Map<String, Field> targetFields = getAllFields(targetClass);

        // 构造并返回 BiConsumer
        return (source, target) -> {
            // 1) 先用 BeanCopier 做快速浅拷贝（基本类型、同名引用等）
            beanCopier.copy(source, target, null);

            // 2) 处理需要深拷贝的字段（集合、数组、Map、数组）
            for (Map.Entry<String, Field> entry : targetFields.entrySet()) {
                String fieldName = entry.getKey();
                Field tField = entry.getValue();

                // 如果目标字段是 static 或 final，跳过（final 写入会失败）
                if (Modifier.isStatic(tField.getModifiers())) continue;

                Field sField = sourceFields.get(fieldName);
                if (sField == null) continue;

                try {
                    sField.setAccessible(true);
                    tField.setAccessible(true);
                    Object sValue = sField.get(source);
                    if (sValue == null) {
                        // 保持 beanCopier 的行为（如果 beanCopier 已经把值覆盖了，我们不再改写 null）
                        continue;
                    }

                    Class<?> tType = tField.getType();
                    // 如果目标字段是集合/数组/Map 或目标类型与源类型不同但为复杂对象，进行深拷贝
                    if (isArray(sValue.getClass()) || isCollection(sValue.getClass()) || sValue instanceof Map) {
                        Object deep = deepCopyValue(sValue, tType);
                        tField.set(target, deep);
                    } else {
                        // 如果目标类型非原始兼容并且是 Bean 类型（非基本/包装/String），递归 copy
                        if (!isDirectlyAssignable(sField.getType(), tType) && !isSimpleValueType(sValue.getClass())) {
                            // 递归 copy：将 sValue 拷贝为 tType 类型
                            Object nested = copy(sValue, tType);
                            tField.set(target, nested);
                        }
                        // 否则，beanCopier 已经完成拷贝
                    }
                } catch (Throwable ex) {
                    // 避免单字段拷贝异常影响整体（记录或忽略）
                    // 这里选择忽略具体字段的异常以保证鲁棒性
                }
            }
        };
    }

    /* -----------------------
     * 深拷贝工具
     * ----------------------- */

    /**
     * 深拷贝入口：支持数组 / List / Set / Map / Bean（递归）
     * targetType 表示目标字段类型（用于数组/集合元素类型推断时有帮助）
     */
    @SuppressWarnings("unchecked")
    private static Object deepCopyValue(Object srcValue, Class<?> targetType) {
        if (srcValue == null) return null;

        Class<?> srcClass = srcValue.getClass();

        // 数组
        if (isArray(srcClass)) {
            int len = Array.getLength(srcValue);
            Class<?> compType = srcClass.getComponentType();
            Object newArr = Array.newInstance(compType, len);
            for (int i = 0; i < len; i++) {
                Object elem = Array.get(srcValue, i);
                Array.set(newArr, i, deepCopyValue(elem, compType));
            }
            return newArr;
        }

        // List
        if (srcValue instanceof List<?>) {
            List<?> srcList = (List<?>) srcValue;
            List<Object> dst = new ArrayList<>(srcList.size());
            for (Object e : srcList) {
                if (e == null) {
                    dst.add(null);
                } else if (isSimpleValueType(e.getClass())) {
                    dst.add(e);
                } else {
                    // 递归 copy 元素为其运行时类型
                    dst.add(copy(e, e.getClass()));
                }
            }
            return dst;
        }

        // Set
        if (srcValue instanceof Set<?>) {
            Set<?> srcSet = (Set<?>) srcValue;
            Set<Object> dst = new LinkedHashSet<>(srcSet.size());
            for (Object e : srcSet) {
                if (e == null) {
                    dst.add(null);
                } else if (isSimpleValueType(e.getClass())) {
                    dst.add(e);
                } else {
                    dst.add(copy(e, e.getClass()));
                }
            }
            return dst;
        }

        // Map
        if (srcValue instanceof Map<?, ?>) {
            Map<?, ?> srcMap = (Map<?, ?>) srcValue;
            Map<Object, Object> dst = new HashMap<>(srcMap.size());
            for (Map.Entry<?, ?> en : srcMap.entrySet()) {
                Object k = en.getKey();
                Object v = en.getValue();
                Object nk = (k == null || isSimpleValueType(k.getClass())) ? k : copy(k, k.getClass());
                Object nv = (v == null || isSimpleValueType(v.getClass())) ? v : copy(v, v.getClass());
                dst.put(nk, nv);
            }
            return dst;
        }

        // 简单类型（Number/String/Boolean/Enum/Date/Character）
        if (isSimpleValueType(srcClass)) {
            return srcValue;
        }

        // 其它非简单 Bean -> 递归拷贝为 targetType（如果 targetType 可实例化）
        try {
            Class<?> actualTarget = targetType != null ? targetType : srcClass;
            // 若 targetType 是接口或 abstract，则优先使用源对象运行时类型
            if (actualTarget.isInterface() || Modifier.isAbstract(actualTarget.getModifiers())) {
                actualTarget = srcClass;
            }
            return copy(srcValue, actualTarget);
        } catch (Exception e) {
            // 回退：如果递归失败，返回原对象（保持原样 - 不能保证完全深拷贝）
            return srcValue;
        }
    }

    /* -----------------------
     * 反射与帮助方法
     * ----------------------- */

    private static Map<String, Field> getAllFields(Class<?> clazz) {
        return FIELDS_CACHE.computeIfAbsent(clazz, c -> {
            Map<String, Field> map = new LinkedHashMap<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                Field[] fields = current.getDeclaredFields();
                for (Field f : fields) {
                    // 跳过静态字段
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    map.putIfAbsent(f.getName(), f);
                }
                current = current.getSuperclass();
            }
            return map;
        });
    }

    private static boolean isArray(Class<?> c) {
        return c.isArray();
    }

    private static boolean isCollection(Class<?> c) {
        return Collection.class.isAssignableFrom(c);
    }

    private static boolean isSimpleValueType(Class<?> c) {
        return c.isPrimitive()
                || Number.class.isAssignableFrom(c)
                || Boolean.class == c
                || Character.class == c
                || CharSequence.class.isAssignableFrom(c)
                || Date.class.isAssignableFrom(c)
                || Enum.class.isAssignableFrom(c);
    }

    private static boolean isDirectlyAssignable(Class<?> src, Class<?> target) {
        if (target.isAssignableFrom(src)) return true;
        // 允许基本类型与包装类型相互赋值（int <-> Integer 等）
        if ((src == Integer.class && target == int.class) || (src == int.class && target == Integer.class)) return true;
        if ((src == Long.class && target == long.class) || (src == long.class && target == Long.class)) return true;
        if ((src == Boolean.class && target == boolean.class) || (src == boolean.class && target == Boolean.class)) return true;
        if ((src == Double.class && target == double.class) || (src == double.class && target == Double.class)) return true;
        if ((src == Float.class && target == float.class) || (src == float.class && target == Float.class)) return true;
        if ((src == Short.class && target == short.class) || (src == short.class && target == Short.class)) return true;
        if ((src == Byte.class && target == byte.class) || (src == byte.class && target == Byte.class)) return true;
        if ((src == Character.class && target == char.class) || (src == char.class && target == Character.class)) return true;
        return false;
    }

    /* -----------------------
     * 测试代码（保留）
     * ----------------------- */

    @Data
    public static class User {
        private Long id;
        private String name;
        private int age;
        private String[] tags;
        private List<Address> addresses;
    }

    @Data
    public static class Address {
        private String city;
        private String street;
    }

    public static void main(String[] args) {
        User user1 = new User();
        user1.setId(123L);
        user1.setName("张三");
        user1.setAge(18);
        user1.setTags(new String[] {"a","b"});
        Address a1 = new Address();
        a1.setCity("Beijing"); a1.setStreet("No.1");
        Address a2 = new Address();
        a2.setCity("Shanghai"); a2.setStreet("No.2");
        user1.setAddresses(Arrays.asList(a1, a2));

        // 拷贝
        User copy = BeanCopyUtil.copy(user1, User.class);
        System.out.println("copy = " + copy);

        // 转换列表
        List<User> list = BeanCopyUtil.copyList(Collections.singletonList(user1), User.class);
        System.out.println(list);
    }
}
