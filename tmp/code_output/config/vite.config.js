// vite.config.js
import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
import {resolve} from 'path';
import {autoImportVueComposables} from './auto-import-vue-composables.js';

/**
 * Vite 配置文件 —— 支持诊断模式（无混淆、可读错误）
 *
 * 使用方式：
 *   - 生产构建：npm run build                → 启用压缩，用于部署
 *   - 诊断构建：npm run build -- --mode diagnose  → 禁用压缩，保留原始变量名，便于 AI 定位错误
 */
export default defineConfig(({ mode }) => {
    // 判断是否为诊断模式
    const isDiagnoseMode = mode === 'diagnose';

    return {
        plugins: [
            vue(),
            autoImportVueComposables(), // 自动注入 Vue 组合式 API
        ],

        // 🔥 防止多项目共享 node_modules 时缓存冲突
        cacheDir: './.vite-cache',

        resolve: {
            alias: {
                // 设置 @ 指向 src，便于 AI 生成标准路径
                '@': resolve(__dirname, 'src'),
            },
        },

        server: {
            host: '0.0.0.0',
            hmr: true,
            open: false, // 服务器环境禁止自动打开浏览器
        },

        build: {
            outDir: 'dist',
            emptyOutDir: true,

            // ✅ 关键：始终开启 source map，便于反解析
            sourcemap: true,

            // ✅ 核心修复：诊断模式下完全禁用压缩和变量名混淆
            minify: isDiagnoseMode ? false : 'esbuild',

            // 可选增强：即使使用 terser（如果切换），也禁用 mangling
            // terserOptions 仅在 minify: 'terser' 时生效，但显式声明更安全
            terserOptions: isDiagnoseMode
                ? {
                    mangle: false,     // 不混淆变量名
                    compress: false,   // 不压缩逻辑
                }
                : undefined,

            rollupOptions: {
                output: {
                    manualChunks: undefined, // 不分包，简化调试
                },
            },
        },

        css: {
            devSourcemap: true, // 开发/诊断时 CSS 也保留 source map
        },

        // 防止清屏，确保 Java 能捕获完整日志
        clearScreen: false,
    };
});
