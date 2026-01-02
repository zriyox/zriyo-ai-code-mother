/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}"
    ],
    theme: {
        extend: {
            colors: {
                primary: "#0066CC",
                secondary: "#86868B",
            },
            // 👇 定义所有你在 CSS 中用到的自定义断点
            screens: {
                // 手机端：最大宽度 767px（典型手机）
                mobile: { max: '767px' },

                // 平板端：768px ~ 1023px
                tablet: { min: '768px', max: '1023px' },

                // 桌面端：最小宽度 1024px（含笔记本、台式机）
                desktop: { min: '1024px' },

                // 可选：如果你还想保留或补充默认断点（其实 extend 不会覆盖默认值，所以 sm/md/lg 等依然可用）
                // 无需重复写 sm/md/lg，Tailwind 会自动合并
            }
        },
    },
    plugins: [],
}
