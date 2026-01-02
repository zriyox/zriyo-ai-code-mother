package com.zriyo.aicodemother.core.parser;

public interface CodeParser<T> {
    /**
     * 解析代码为对象
     *
     * @param code 代码
     * @return 对象
     */
    T parseCode(String code);
}
