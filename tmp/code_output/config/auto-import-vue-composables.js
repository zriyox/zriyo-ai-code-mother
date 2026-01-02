// plugins/autoImportVueComposables.js
import MagicString from 'magic-string'

const COMPOSABLES = ['ref', 'reactive', 'watch', 'computed', 'onMounted', 'onUnmounted', 'onBeforeMount']

export function autoImportVueComposables() {
    return {
        name: 'auto-import-vue-composables',
        transform(code, id) {
            // 只处理 .vue 文件中的 <script setup>
            if (!id.endsWith('.vue')) return null

            // 检查是否已有 <script setup>
            const scriptSetupMatch = code.match(/<script\s+setup[^>]*>([\s\S]*?)<\/script>/i)
            if (!scriptSetupMatch) return null

            const scriptContent = scriptSetupMatch[1]
            const hasExistingImportFromVue = /import\s+{[^}]*}\s+from\s+['"]vue['"]/.test(scriptContent)

            // 如果已经手动导入了 vue，跳过（避免冲突）
            if (hasExistingImportFromVue) return null

            // 检查是否使用了任何组合函数
            const usedComposables = COMPOSABLES.filter(name =>
                new RegExp(`\\b${name}\\s*\\(`).test(scriptContent)
            )

            if (usedComposables.length === 0) return null

            // 构建 import 语句
            const importStmt = `import { ${usedComposables.join(', ')} } from 'vue'\n`

            // 使用 MagicString 安全插入到 <script setup> 顶部
            const s = new MagicString(code)
            const insertPos = scriptSetupMatch.index + '<script setup>'.length
            s.prependLeft(insertPos, '\n' + importStmt)

            return {
                code: s.toString(),
                map: s.generateMap({ hires: true })
            }
        }
    }
}
