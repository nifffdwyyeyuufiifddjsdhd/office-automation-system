
import re, pathlib, json, importlib.util, sys

root = pathlib.Path(r"D:\VSC\Employment Project\office-automation-system")
spec = importlib.util.spec_from_file_location("gen", root / "scripts/gen-api-docs.py")
# 不执行生成脚本（会写文件），直接复用它的正则与解析逻辑：改为解析已生成的 markdown 更稳妥
def norm(p):
    p = p.split("?")[0]
    p = re.sub(r'\${[^}]*}', '<var>', p)
    p = re.sub(r'\{[^}]*\}', '<var>', p)
    p = re.sub(r':\w+', '<var>', p)
    p = re.sub(r'/+$', '', p)
    return p

java_dir = root / "OfficeAutomationSystemAPI/src/main/java/com/officeautomationsystemapi/controller"
backend = []   # (method, path, controller, fn)
map_re = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\b')
for f in sorted(java_dir.rglob("*.java")):
    text = f.read_text(encoding="utf-8")
    head = text.split("public class")[0]
    prefix = ""
    for pm in re.finditer(r'@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]*)"', head):
        prefix = pm.group(1)
    for m in map_re.finditer(text):
        am = re.match(r'@\w+Mapping\s*(\([^)]*\))?', text[m.start():m.start()+200])
        sub = ""
        if am and am.group(1):
            q = re.search(r'"([^"]*)"', am.group(1))
            sub = q.group(1) if q else ""
        # 函数名：本段内 public 后面第一个 (
        seg = text[m.start(): m.start() + 1500]
        fm = re.search(r'\bpublic\s+[^;{=]*?\b(\w+)\s*\(', seg)
        backend.append((m.group(1).upper(), norm(prefix + sub), f.stem, fm.group(1) if fm else ""))

web_api = root / "OfficeAutomationSystemWeb/src/api"
frontend = []  # (method, path, file, fn)
for f in sorted(web_api.rglob("*.ts")):
    text = f.read_text(encoding="utf-8")
    for m in re.finditer(r'request\.(get|post|put|delete|patch)\s*(?:<[\s\S]*?>)?\s*\(', text):
        # 取第一个实参（到顶层逗号为止），处理 '/x/' + id 这种拼接
        i = m.end()
        depth, quote, j = 0, None, i
        while j < len(text):
            ch = text[j]
            if quote:
                if ch == quote:
                    quote = None
            elif ch in "\"'\x60":
                quote = ch
            elif ch in "([{":
                depth += 1
            elif ch in ")]}":
                if depth == 0:
                    break
                depth -= 1
            elif ch == "," and depth == 0:
                break
            j += 1
        first = text[i:j].strip()
        # 按顶层 + 切成若干段：引号串取内容，变量段记 <var>，最后拼起来
        terms, cur, quote, depth = [], "", None, 0
        for ch in first:
            if quote:
                cur += ch
                if ch == quote:
                    quote = None
                continue
            if ch in "\"'\x60":
                quote = ch; cur += ch; continue
            if ch in "([{":
                depth += 1
            elif ch in ")]}":
                depth -= 1
            if ch == "+" and depth == 0:
                terms.append(cur.strip()); cur = ""
            else:
                cur += ch
        terms.append(cur.strip())
        parts = []
        for t in terms:
            if not t:
                continue
            if re.fullmatch(r'[\x60"\'][^\x60"\']*[\x60"\']', t):
                parts.append(t[1:-1])
            elif t.startswith("/") and t.endswith("/"):
                parts.append(t)
            else:
                parts.append("<var>")
        raw = "".join(parts)
        before = text[:m.start()]
        nm = ""
        for fm in re.finditer(r'export\s+(?:async\s+)?function\s+(\w+)\s*\(|export\s+const\s+(\w+)\s*=', before):
            nm = fm.group(1) or fm.group(2) or ""
        frontend.append((m.group(1).upper(), norm("/api" + raw), f.stem, nm))

bk = {}
for meth, path, ctrl, fn in backend:
    bk.setdefault((meth, path), []).append((ctrl, fn))
ft = {}
for meth, path, f, fn in frontend:
    ft.setdefault((meth, path), []).append((f, fn))

only_back = [(k, v) for k, v in bk.items() if k not in ft]
only_front = [(k, v) for k, v in ft.items() if k not in bk]

lines = []
lines.append("# 接口对账表（后端 Controller ↔ 前端 api 封装）")
lines.append("")
lines.append("由 `scripts/check-api-coverage.py` 生成：把后端 @*Mapping 路径与前端 request.* 调用路径都归一化（`{id}/${id}/:id → <var>`）后按「方法 + 路径」比对。")
lines.append("")
lines.append("统计：后端 %d 条（去重后 %d）、前端 %d 条（去重后 %d）；**只后端有 %d 条，只前端有 %d 条**。" % (
    len(backend), len(bk), len(frontend), len(ft), len(only_back), len(only_front)))
lines.append("")
lines.append("## 一、只后端有：前端还没封装")
lines.append("")
lines.append("这些接口不是错误。残余的基本是刻意直连的：导出类走 `fetch` 拿二进制（要校验 content-type）；图片/附件直接给 `<img src>` 或下载链接（带不上 token，走免登录接口）；运维触发端点（`/attendance/settle`、`/flow/instances/urge-timeout`）由定时任务或管理员手点；浏览计数（`/announcements/{id}/reads`）由服务端自增。新写页面时先在这里找回已有能力，别重复造。")
lines.append("")
lines.append("| 方法 | 路径 | 定义在 Controller | 处理函数 |")
lines.append("| --- | --- | --- | --- |")
for (meth, path), v in sorted(only_back):
    lines.append("| %s | `%s` | %s | %s |" % (meth, path, ", ".join(sorted({c for c, _ in v})), ", ".join(sorted({f for _, f in v}))))
lines.append("")
lines.append("## 二、只前端有：后端没有对应路径（重点核对）")
lines.append("")
lines.append("可能是前端调错路径、后端改名后没同步，也可能是路径里带了 `/api` 前缀以外的写法导致误报，逐条确认。")
lines.append("")
lines.append("| 方法 | 路径 | 来自 api 文件 | 函数 |")
lines.append("| --- | --- | --- | --- |")
for (meth, path), v in sorted(only_front):
    lines.append("| %s | `%s` | %s | %s |" % (meth, path, ", ".join(sorted({f for f, _ in v})), ", ".join(sorted({n for _, n in v}))))
lines.append("")
(root / "docs/接口对账.md").write_text("\n".join(lines), encoding="utf-8")
print("backend", len(backend), "unique", len(bk), "| frontend", len(frontend), "unique", len(ft))
print("only backend", len(only_back), "only frontend", len(only_front))
for k, v in sorted(only_front)[:30]:
    print("  FRONT-ONLY", k, v)
