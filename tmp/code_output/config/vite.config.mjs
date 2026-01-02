// vite.config.mjs
import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import {resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

// 兼容 ES 模块的 __dirname 路径处理
const __filename = fileURLToPath(import.meta.url);
const __dirname = resolve(__filename, '..');

export default defineConfig({
    plugins: [
        vue({
            template: {
                compilerOptions: {
                    // ✅ 核心逻辑：在编译模板时，给所有原生 HTML 标签注入 data-source 属性
                    // 这样即使在渲染后的 HTML 中，AI 也能通过 DOM 属性直接看到该标签属于哪个 .vue 文件
                    nodeTransforms: [
                        (node, context) => {
                            // 1: ELEMENT, tagType 0: ELEMENT (非组件)
                            if (
                                node.type === 1 &&
                                node.tagType === 0 &&
                                context.filename &&
                                context.filename.includes(resolve(__dirname, 'src'))
                            ) {
                                // 避免重复注入
                                const hasDataSource = node.props.some(
                                    (prop) => prop.name === 'data-source'
                                );
                                if (hasDataSource) return;

                                // 转换路径格式为相对路径，如 src/components/Header.vue
                                let relativePath = context.filename
                                    .replace(resolve(__dirname, 'src'), 'src')
                                    .replace(/\\/g, '/'); // 统一 Unix 路径分隔符

                                if (relativePath.includes('?')) {
                                    relativePath = relativePath.split('?')[0];
                                }

                                // 将属性推入节点
                                node.props.push({
                                    type: 6, // ATTRIBUTE
                                    name: 'data-source',
                                    value: {
                                        type: 2, // TEXT
                                        content: relativePath,
                                        loc: node.loc,
                                    },
                                    loc: node.loc,
                                });
                            }
                        },
                    ],
                },
            },
        }),
        AutoImport({
            imports: ['vue'],
            dts: true,
        }),
    ],

    // 指定缓存目录，避免在 Docker 挂载目录下产生冲突
    cacheDir: './.vite-cache',

    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },

    server: {
        host: '0.0.0.0',
        hmr: false, // 诊断模式建议关闭热更新，防止进程占用
        open: false,
    },

    build: {
        outDir: 'dist',
        emptyOutDir: true, // 构建前自动清空 dist
        // ✅ 关键：开启 Source Map 映射
        sourcemap: 'inline', // 使用 inline 模式可防止静态服务器漏掉 .map 文件，确保报错必现源码文件名
        minify: false,       // 诊断模式禁用压缩，保证报错行号与源码对齐
        cssCodeSplit: false, // 禁用 CSS 拆分，简化诊断路径
        rollupOptions: {
            output: {
                // 强制固定文件名，防止哈希导致静态服务器缓存失效
                entryFileNames: `assets/[name].js`,
                chunkFileNames: `assets/[name].js`,
                assetFileNames: `assets/[name].[ext]`,
                manualChunks: undefined,
            },
        },
    },

    // 强制每次构建重新扫描依赖，解决 dist 不更新的顽疾
    optimizeDeps: {
        force: true,
    },

    css: {
        // CSS 报错也要映射回源码
        devSourcemap: true,
    },

    clearScreen: false,
});
