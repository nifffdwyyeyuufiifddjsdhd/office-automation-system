// 富文本消毒：公告正文要用 v-html 渲染，先过一遍白名单
// 解析成 DOM 后按白名单重建，去掉脚本、事件属性和危险协议

const ALLOWED_TAGS = new Set([
    'p', 'br', 'div', 'span', 'strong', 'b', 'em', 'i', 'u', 's', 'del', 'ins', 'sub', 'sup', 'mark', 'small',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'pre', 'code', 'hr',
    'ul', 'ol', 'li', 'dl', 'dt', 'dd',
    'a', 'img',
    'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td', 'caption', 'colgroup', 'col'
])

// 这些标签整段丢弃（含内容），不能只删标签
const DROP_TAGS = new Set([
    'script', 'style', 'iframe', 'frame', 'frameset', 'object', 'embed', 'applet',
    'form', 'input', 'button', 'select', 'option', 'textarea', 'link', 'meta', 'base', 'svg', 'math'
])

const ALLOWED_ATTRS = new Set([
    'href', 'src', 'alt', 'title', 'target', 'rel',
    'width', 'height', 'colspan', 'rowspan', 'align', 'valign',
    'class', 'style'
])

// 只放这几个协议，javascript:、data:text/html 等一律清空
function safeUrl(value: string): boolean {
    const v = value.trim().toLowerCase().replace(/[\u0000-\u0020]/g, '')
    if (v.startsWith('javascript:') || v.startsWith('vbscript:')) return false
    if (v.startsWith('data:')) return /^data:image\/(png|jpe?g|gif|webp|bmp);base64,/.test(v)
    return true
}

function sanitizeStyle(value: string): string {
    const bad = /expression\s*\(|javascript:|vbscript:|url\s*\(\s*['"]?\s*(javascript|vbscript|data:text)/i
    if (bad.test(value)) return ''
    return value.replace(/\n/g, ' ').slice(0, 400)
}

function cleanNode(node: Node, out: Node[]) {
    if (node.nodeType === Node.TEXT_NODE) {
        out.push(document.createTextNode(node.nodeValue || ''))
        return
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return
    const el = node as HTMLElement
    const tag = el.tagName.toLowerCase()
    if (DROP_TAGS.has(tag)) return
    if (!ALLOWED_TAGS.has(tag)) {
        // 不在白名单的标签（如 font、center）拆掉外壳保留文字
        el.childNodes.forEach(child => cleanNode(child, out))
        return
    }
    const copy = document.createElement(tag)
    for (const attr of Array.from(el.attributes)) {
        const name = attr.name.toLowerCase()
        if (!ALLOWED_ATTRS.has(name)) continue      // 去掉 onerror、onclick 这类事件属性
        if (name === 'href' || name === 'src') {
            if (!safeUrl(attr.value)) continue
        }
        if (name === 'style') {
            const style = sanitizeStyle(attr.value)
            if (style) copy.setAttribute('style', style)
            continue
        }
        copy.setAttribute(name, attr.value)
    }
    if (tag === 'a') {
        copy.setAttribute('rel', 'noopener noreferrer')
        if (copy.getAttribute('target')) copy.setAttribute('target', '_blank')
    }
    const children: Node[] = []
    el.childNodes.forEach(child => cleanNode(child, children))
    children.forEach(child => copy.appendChild(child))
    out.push(copy)
}

// 消毒富文本，返回值可直接交给 v-html
// 非浏览器环境（SSR、单测）返回空串
export function sanitizeHtml(html?: string | null): string {
    if (!html) return ''
    if (typeof document === 'undefined') return ''
    const doc = new DOMParser().parseFromString(html, 'text/html')
    const nodes: Node[] = []
    doc.body.childNodes.forEach(child => cleanNode(child, nodes))
    const box = document.createElement('div')
    nodes.forEach(node => box.appendChild(node))
    return box.innerHTML
}

export default sanitizeHtml
