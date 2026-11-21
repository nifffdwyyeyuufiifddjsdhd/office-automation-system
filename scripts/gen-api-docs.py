# -*- coding: utf-8 -*-
"""扫描 OA 项目源码，生成《后端接口文档》《前端接口文档》。

后端文档每条接口给出：定义文件与行号、方法+完整路径、处理函数、入参、权限码、行为、
涉及的数据库表（英文表名+中文表名）、返回类型，并**贴出该接口方法的完整源码（带行号）与逐段讲解**。
前端文档同样贴出每个封装函数的源码与讲解。
表名推断链路：Controller 直接用的 Mapper/Repository、它调用的 Service（实现类或
单类 @Service）方法内部实际用到的 Mapper/Repository、方法体内写死的 SQL。
"""
import re
import pathlib

ROOT = pathlib.Path(r"D:\VSC\Employment Project\office-automation-system")
BASE = ROOT / "OfficeAutomationSystemAPI/src/main/java/com/officeautomationsystemapi"
WEB = ROOT / "OfficeAutomationSystemWeb/src/api"
DOCS = ROOT / "docs"
TICK = chr(96)
SEC = chr(167)


def balanced(s, i, open_ch, close_ch):
    depth = 0
    quote = None
    esc = False
    while i < len(s):
        ch = s[i]
        if quote:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == quote:
                quote = None
        else:
            if ch in "\"'":
                quote = ch
            elif ch == open_ch:
                depth += 1
            elif ch == close_ch:
                depth -= 1
                if depth == 0:
                    return i + 1
        i += 1
    return -1


def split_args(s):
    out, start, i = [], 0, 0
    quote = None
    esc = False
    depth = 0
    while i < len(s):
        ch = s[i]
        if quote:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == quote:
                quote = None
        else:
            if ch in "\"'":
                quote = ch
            elif ch in "<([{":
                depth += 1
            elif ch in ">)]}":
                depth -= 1
            elif ch == "," and depth == 0:
                out.append(s[start:i].strip())
                start = i + 1
        i += 1
    if s[start:].strip():
        out.append(s[start:].strip())
    return out


def ann_attrs(s, i):
    j = i
    while j < len(s) and s[j] in " \t\r\n":
        j += 1
    if j < len(s) and s[j] == "(":
        end = balanced(s, j, "(", ")")
        return s[j + 1:end - 1], end
    return "", i


def parse_param(a):
    a = re.sub(r"\s+", " ", a).strip()
    if not a:
        return None
    kind = ""
    attrs = ""
    rest = a
    if a.startswith("@"):
        m = re.match(r"@(\w+)", a)
        kind = m.group(1)
        attrs, end = ann_attrs(a, m.end())
        rest = a[end:].strip()
    if rest.startswith("final "):
        rest = rest[6:]
    toks = rest.split(" ")
    if len(toks) < 2:
        return None
    typ = " ".join(toks[:-1])
    var = toks[-1]
    if kind in ("RequestBody", "ModelAttribute") or not kind:
        return kind or "POJO", var, typ, "JSON 体" if kind == "RequestBody" else "表单/查询对象"
    nm = re.search(r'(?:value|name)\s*=\s*"([^"]*)"', attrs)
    name = nm.group(1) if nm else var
    req = "可选" if re.search(r"required\s*=\s*false", attrs) else "必填"
    dft = re.search(r'defaultValue\s*=\s*"([^"]*)"', attrs)
    if "MultipartFile" in typ:
        desc = "multipart 文件 " + req
    elif kind == "PathVariable":
        desc = "路径参数 " + req
    elif kind == "RequestParam":
        desc = "查询/表单 " + req
        if dft and dft.group(1):
            desc += "，默认 " + dft.group(1)
    else:
        desc = kind
    return kind, name, typ, desc


def method_body(text, start):
    po = text.find("(", start)
    if po < 0:
        return None
    close = balanced(text, po, "(", ")")
    if close < 0:
        return None
    bo = text.find("{", close)
    if bo < 0:
        return None
    be = balanced(text, bo, "{", "}")
    if be < 0:
        return None
    return bo + 1, be - 1


def parse_fields(text):
    f = {}
    for m in re.finditer(r"@(?:Autowired|Resource)\s*(?:\([^)]*\))?\s*(?:private\s+|protected\s+|public\s+)?([A-Z][\w]*)\s+(\w+)\s*;", text):
        f[m.group(2)] = m.group(1)
    return f


def parse_methods(text):
    out = {}
    for mm in re.finditer(r"\b(?:public|private|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\]., ]+\s+(\w+)\s*\(", text):
        name = mm.group(1)
        if name in ("if", "for", "while", "switch", "catch", "return", "new"):
            continue
        r = method_body(text, mm.end() - 1)
        if r:
            out[name] = text[r[0]:r[1]]
    return out


entity_table = {}
for f in (BASE / "entity").glob("*.java"):
    t = f.read_text(encoding="utf-8")
    m = re.search(r'@TableName\(\s*"([^"]*)"', t)
    entity_table[f.stem] = m.group(1) if m else f.stem

repo_entity = {}
for f in (BASE / "repository").glob("*.java"):
    t = f.read_text(encoding="utf-8")
    m = re.search(r"interface\s+\w+\s+extends\s+[\w.]+<([\w.]+)", t)
    if m:
        repo_entity[f.stem] = entity_table.get(m.group(1), m.group(1))

CN = {
 "users": "用户表", "roles": "角色表", "user_roles": "用户角色关联表", "menus": "菜单与权限码表",
 "role_permissions": "角色权限关联表", "user_permissions": "用户级授权表", "organizations": "部门表",
 "positions": "职位表", "titles": "职称表", "page_controls": "控件权限配置表",
 "flow_definitions": "流程定义表", "flow_steps": "流程步骤表", "flow_step_transitions": "步骤流转线表",
 "flow_instances": "流程实例表", "flow_approvals": "流程审批轨迹表", "flow_delegates": "审批委托表",
 "attendance_records": "考勤打卡记录表", "attendance_makeup": "补签申请表", "attendance_appeals": "考勤申诉表",
 "attendance_outing": "外出打卡表", "shift_templates": "班次模板表", "shift_schedules": "排班表", "holidays": "节假日表",
 "worklogs": "工作日志表", "worklog_comments": "日志评论表", "worklog_categories": "日志类别表",
 "worklog_supplement_approvals": "日志补写审批表", "worklog_view_permissions": "日志查看授权表", "attachments": "附件表",
 "projects": "项目表", "project_members": "项目成员表", "tasks": "任务表", "task_members": "任务参与人表",
 "task_comments": "任务评论表", "task_operation_logs": "任务操作日志表",
 "announcements": "公告表", "announcement_categories": "公告分类表", "announcement_category_roles": "公告分类可见角色表",
 "announcement_reads": "公告已读记录表", "notification_records": "站内通知表", "notification_templates": "通知模板表",
 "user_notification_preferences": "通知偏好表", "system_params": "系统参数表", "nav_images": "导航图片表",
 "system_config": "考勤配置表", "payment_records": "付款记录表", "expense_categories": "费用类别表",
 "recruitment_positions": "招聘岗位表", "recruitment_candidates": "招聘候选人表", "user_handovers": "离职交接表",
 "user_third_party_bindings": "第三方账号绑定表", "login_logs": "登录日志表", "operation_logs": "操作日志表",
 "exception_logs": "异常日志表", "online_users": "在线用户表", "password_history": "历史密码表",
 "user_groups": "用户群组表", "user_group_members": "群组成员表", "tool_downloads": "工具下载表",
}
ALL_TABLES = set(entity_table.values()) | {
    "attendance_outing", "shift_templates", "shift_schedules", "worklog_supplement_approvals", "system_config"}


def cn(t):
    return CN.get(t, "未登记")


SELF_OPS = re.compile(
    r"\b(baseMapper|lambdaQuery|lambdaUpdate)\b"
    r"|\b(?:this\.)?(save|saveBatch|remove|removeById|removeByIds|update|updateById|getById|page|list|listByIds"
    r"|count|getOne|selectById|selectList|selectOne)\s*\("
)
SQL_REF = re.compile(r"\b(?:FROM|INTO|UPDATE|JOIN)\s+([A-Za-z_]{3,})", re.I)
CRUD_NAMES = {"list", "listByIds", "save", "saveBatch", "saveOrUpdate", "updateById", "getById",
              "removeById", "removeByIds", "page", "count", "getOne"}

# 权限校验、数据范围、审计这类服务每个请求都会碰，它们读的表单独归为「横切」
INFRA_CLASS = re.compile(r"^(Permission|DataScope|ProjectScope|Audit|Sensitive)")


def is_infra_cls(name):
    return bool(INFRA_CLASS.search(name or ""))


CLASSES = {}
MEMO = {}


def register(name, info):
    if name and name not in CLASSES:
        CLASSES[name] = info


def build_classes():
    for d, ent_sub in ((BASE / "service" / "impl", "_impl"), (BASE / "service", "_svc"), (BASE / "controller", "_ctl")):
        for f in sorted(d.glob("*.java")):
            t = f.read_text(encoding="utf-8")
            info = {"fields": parse_fields(t), "methods": parse_methods(t), "entity": None}
            m = re.search(r"extends\s+ServiceImpl<[\w.]+,\s*(\w+)>", t)
            if m:
                info["entity"] = entity_table.get(m.group(1))
            register(f.stem, info)
            if ent_sub == "_impl":
                im = re.search(r"\bclass\s+\w+[^{]*?\bimplements\s+([\w,\s]+?)\s*\{", t)
                if im:
                    for itf in im.group(1).split(","):
                        itf = itf.split("<")[0].strip()
                        register(itf, info)
    for stem, table in repo_entity.items():
        register(stem, {"fields": {}, "methods": {}, "entity": None, "repo_entity": table})


build_classes()


def merge_table(dst, table, infra):
    if table not in dst:
        dst[table] = infra
    else:
        dst[table] = dst[table] and infra


def merge_into(dst, src):
    for t, inf in src.items():
        merge_table(dst, t, inf)


def method_tables(cls, member, depth=0, stack=(), via_infra=False):
    key = (id(cls), member, via_infra)
    if key in MEMO:
        return MEMO[key]
    if key in stack or depth > 5:
        return {}
    sub = cls["methods"].get(member)
    if sub is None:
        return {}
    out = body_tables(sub, cls, depth + 1, stack + (key,), via_infra)
    if cls.get("entity") and SELF_OPS.search(sub):
        merge_table(out, cls["entity"], via_infra)
    MEMO[key] = out
    return out


def body_tables(body, cls, depth=0, stack=(), via_infra=False):
    out = {}
    if not body or depth > 5:
        return out
    fields = cls["fields"] if cls else {}
    for var, member in re.findall(r"\b(\w+)\.(\w+)\s*\(", body):
        typ = fields.get(var)
        if not typ:
            continue
        target = CLASSES.get(typ)
        if target is None:
            continue
        flag = via_infra or is_infra_cls(typ)
        if target.get("repo_entity"):
            merge_table(out, target["repo_entity"], flag)
        else:
            sub = method_tables(target, member, depth, stack, flag)
            merge_into(out, sub)
            # ServiceImpl 继承来的通用 CRUD（list/save/getById...）没在实现类里声明，按实体补上
            if target.get("entity") and member in CRUD_NAMES:
                merge_table(out, target["entity"], flag)
    if cls:
        for member in re.findall(r"(?<![\w.$])(\w+)\s*\(", body):
            if member in fields:
                continue
            if member in cls["methods"]:
                merge_into(out, method_tables(cls, member, depth, stack, via_infra))
    for mm in SQL_REF.finditer(body):
        name = mm.group(1).lower()
        if name in ALL_TABLES:
            merge_table(out, name, via_infra)
    return out


def ann_start(text, pos):
    """从方法签名往前吃掉连续的注解行，得到这个接口在文件里的起始位置"""
    start = text.rfind("\n", 0, pos) + 1
    while start > 0:
        prev_end = start - 1
        pstart = text.rfind("\n", 0, prev_end) + 1
        line = text[pstart:prev_end]
        if line.strip().startswith("@"):
            start = pstart
        else:
            break
    return start


def split_blocks(code, first_line):
    """按 §// 中文注释§ 和空行把方法体切段，每段带真实行号范围"""
    lines = code.splitlines()
    blocks, cur = [], {"head": "", "start": None, "lines": []}
    for i, ln in enumerate(lines):
        s = ln.strip()
        num = first_line + i
        if s.startswith("//"):
            if cur["lines"]:
                cur["end"] = num - 1
                blocks.append(cur)
                cur = {"head": "", "start": None, "lines": []}
            text = s.lstrip("/ ").strip()
            cur["head"] = (cur["head"] + " " + text).strip() if cur["head"] else text
            if cur["start"] is None:
                cur["start"] = num
            continue
        if not s:
            if cur["lines"]:
                cur["end"] = num - 1
                blocks.append(cur)
                cur = {"head": "", "start": None, "lines": []}
            continue
        if cur["start"] is None:
            cur["start"] = num
        cur["lines"].append(ln)
    if cur["lines"]:
        cur["end"] = first_line + len(lines) - 1
        blocks.append(cur)
    for b in blocks:
        if not b["lines"]:
            b["end"] = b["start"]
        b["text"] = "\n".join(b["lines"])
    return [b for b in blocks if b["lines"] or b["head"]]


SKIP_CALLS = {"if", "for", "while", "switch", "catch", "return", "new", "throw", "synchronized"}
# 静态工具类调用没有"字段.方法"形式，单独认一下，免得讲解读起来像什么都没做
STATIC_HINTS = [
    (r"Files\.(?:copy|write|createDirectories)", "把内容落盘写文件"),
    (r"new XSSFWorkbook|new SXSSFWorkbook", "用 POI 新建 Excel 工作簿"),
    (r"getOutputStream\(", "把文件写进响应流（前端按 blob 收）"),
    (r"JwtUtil\.", "签发或校验 token"),
    (r"passwordEncoder\.|BCrypt", "口令哈希比对"),
    (r"new Page<|selectPage\(", "交给 MyBatis-Plus 分页插件查一页"),
    (r"LocalDate|LocalDateTime", "取当前日期时间"),
]


def ops_of(txt, fields, clsname=None, fn=None):
    """把一段代码翻译成人话：判断了什么、调了谁、碰哪张表、失败怎么返回"""
    calls, seen = [], set()
    for m in re.finditer(r"\b(\w+)\.(\w+)\s*\(", txt):
        var, mem = m.group(1), m.group(2)
        typ = fields.get(var)
        if not typ:
            continue
        tgt = CLASSES.get(typ) or {}
        if tgt.get("repo_entity"):
            t = tgt["repo_entity"]
            label = "§%s.%s§（读/写 %s，%s）" % (var, mem, t, cn(t))
        else:
            label = "§%s.%s§" % (var, mem)
        if label in seen:
            continue
        seen.add(label)
        calls.append(label)
    flags = []
    conds = []
    for m in re.finditer(r"\bif\s*\(", txt):
        po = m.end() - 1
        close = balanced(txt, po, "(", ")")
        if close > po:
            c = re.sub(r"\s+", " ", txt[po + 1: close - 1]).strip()
            conds.append(c[:60])
    if conds:
        flags.append("判断 " + "、".join("§%s§" % c for c in conds[:2]))
    for nm in re.findall(r'createSheet\(\s*"([^"]{0,20})', txt):
        flags.append("新建工作表「%s」" % nm)
    for pat, phrase in STATIC_HINTS:
        if re.search(pat, txt) and phrase not in flags:
            flags.append(phrase)
    # 本类的私有辅助方法（抢单、判身份这类）也点出来，别让讲解看着像没调用
    own = (CLASSES.get(clsname) or {}).get("methods", {}) if clsname else {}
    for m in re.finditer(r"(?<![\w.])(\w+)\s*\(", txt):
        mem = m.group(1)
        if mem in SKIP_CALLS or mem in fields or mem not in own or mem == fn:
            continue
        label = "本类 §%s§" % mem
        if label not in seen:
            seen.add(label)
            calls.append(label)
    errs = re.findall(r'Result\.error\(\s*"([^"]{0,24})', txt)
    if errs:
        flags.append("出错直接 return §Result.error§（" + "、".join("「%s」" % e for e in errs[:3]) + "）")
    th = re.search(r'throw\s+new\s+BusinessException\s*\(\s*"([^"]{0,40})', txt)
    if th:
        flags.append("不满足就抛 §BusinessException(\"%s…\")§，由全局异常处理转成 Result.error" % th.group(1))
    if re.search(r"\bResult\.success\(", txt) and not errs:
        flags.append("最后 return §Result.success§")
    if len(calls) > 5:
        calls = calls[:5] + ["…共 %d 处调用" % len(seen)]
    return "；".join(calls + flags)


def numbered(code, first_line):
    """给源码每行加上真实文件行号，方便和讲解里的 L123 对上"""
    out = []
    for i, ln in enumerate(code.splitlines()):
        out.append("%4d| %s" % (first_line + i, ln.rstrip()))
    return out


def chain_of(body, clsname, fn, fields):
    """这个接口往下调了哪些 Service 方法（只到第一层，够用来跳读）"""
    parts, seen = ["%s.%s" % (clsname, fn)], set()
    for m in re.finditer(r"\b(\w+)\.(\w+)\s*\(", body):
        var, mem = m.group(1), m.group(2)
        typ = fields.get(var)
        tgt = CLASSES.get(typ or "")
        if not tgt or tgt.get("repo_entity"):
            continue
        key = (typ, mem)
        if key in seen:
            continue
        seen.add(key)
        parts.append("%s.%s" % (typ, mem))
    if len(parts) > 6:
        parts = parts[:6] + ["…"]
    return parts


def explain(code, first_line, fields, clsname=None, fn=None):
    """逐段讲解：行号范围 + 那行注释 + 这段实际做了什么"""
    out = []
    for b in split_blocks(code, first_line):
        ops = ops_of(b["text"], fields, clsname, fn)
        head = ("**%s**：" % b["head"]) if b["head"] else ""
        if not head and not ops:
            continue
        span = "L%d" % b["start"] if b["start"] == b["end"] else "L%d–L%d" % (b["start"], b["end"])
        out.append("- %s　%s%s" % (span, head, ops))
    return out


DEFINE_RE = re.compile(r"@(Get|Post|Put|Delete|Patch)Mapping\b")
controllers = []
for f in sorted((BASE / "controller").glob("*.java")):
    text = f.read_text(encoding="utf-8")
    head = text.split("public class")[0]
    prefix = ""
    for pm in re.finditer(r'@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]*)"', head):
        prefix = pm.group(1)
    fields = parse_fields(text)
    cls = CLASSES.get(f.stem)
    hits = list(DEFINE_RE.finditer(text))
    rows = []
    prev_end = 0
    for i, m in enumerate(hits):
        seg_end = hits[i + 1].start() if i + 1 < len(hits) else len(text)
        block = text[m.start(): seg_end]
        attrs, _ = ann_attrs(block, len(m.group(0)))
        q = re.search(r'"([^"]*)"', attrs or "")
        paren = q.group(1) if q else ""
        perm = ""
        pm2 = re.search(r"@RequirePermission\b", block)
        if pm2:
            a2, _ = ann_attrs(block, pm2.end())
            perm = " ".join(re.findall(r'"([^"]*)"', a2))
        note = ""
        for line in reversed(text[prev_end:m.start()].rstrip().splitlines()[-4:]):
            s = line.strip()
            if s.startswith("//"):
                note = s.lstrip("/ ").strip()
                break
            if s.startswith("@") or s.startswith("*") or s.startswith("/*"):
                continue
            break
        prev_end = m.start()
        ret, fn, params, body = "", "", [], ""
        base = 0
        method_end = 0
        fm = re.search(r"\bpublic\s+", block)
        if fm:
            sig = block[fm.end():]
            base = m.start() + fm.end()
            po = sig.find("(")
            if po > 0:
                decl = re.sub(r"\s+", " ", sig[:po]).strip()
                parts = decl.split(" ")
                if len(parts) >= 2:
                    ret = " ".join(parts[:-1]).strip()
                    fn = parts[-1].strip()
                close = balanced(sig, po, "(", ")")
                for a in split_args(sig[po + 1: close - 1]):
                    p = parse_param(a)
                    if p and "HttpServletRequest" not in p[2] and "HttpServletResponse" not in p[2]:
                        params.append(p)
                bo = sig.find("{", close)
                if bo > 0:
                    be = balanced(sig, bo, "{", "}")
                    if be > 0:
                        body = sig[bo + 1: be - 1]
                        method_end = base + be
        head_abs = ann_start(text, m.start())
        if not method_end:
            method_end = m.start() + len(block)
        code = text[head_abs:method_end].rstrip()
        res = body_tables(body, cls)
        rows.append({"method": m.group(1).upper(), "path": prefix + paren, "fn": fn, "ret": ret,
                     "perm": perm, "note": note, "params": params, "body": body, "code": code,
                     "start_line": text.count("\n", 0, head_abs) + 1,
                     "end_line": text.count("\n", 0, method_end) + 1,
                     "tables": {t for t, inf in res.items() if not inf},
                     "infra": {t for t, inf in res.items() if inf}})
    if rows:
        controllers.append({"file": str(f.relative_to(ROOT)).replace("\\", "/"), "name": f.stem,
                            "prefix": prefix, "rows": rows, "fields": fields})

MODULES = [
 ("一、认证与个人中心", ["AuthController", "UserBindingController"]),
 ("二、组织、用户与权限", ["OrganizationController", "UsersController", "PositionsController", "TitlesController",
                    "UserGroupsController", "RoleController"]),
 ("三、流程引擎", ["FlowDefinitionController", "FlowInstanceController", "FlowDelegateController"]),
 ("四、考勤", ["AttendanceController"]),
 ("五、工作日志", ["WorklogController"]),
 ("六、任务与项目", ["TaskController", "ProjectsController"]),
 ("七、公告与通知", ["AnnouncementController", "NotificationController", "NotificationAdminController", "PublicController"]),
 ("八、财务", ["PaymentController", "ExpenseCategoryController", "FinanceStatController"]),
 ("九、招聘", ["RecruitmentController"]),
 ("十、报表", ["ReportController"]),
 ("十一、系统管理", ["SystemController", "LoginLogController", "OnlineUserController"]),
]
by_name = {c["name"]: c for c in controllers}
ordered, placed = [], set()
for title, names in MODULES:
    lst = [by_name[n] for n in names if n in by_name]
    placed |= set(names)
    if lst:
        ordered.append((title, lst))
rest = [c for c in controllers if c["name"] not in placed]
if rest:
    ordered.append(("十二、其他", rest))


def behavior_of(note, body, method, tables):
    if note:
        return note
    ops = []
    if re.search(r"XSSFWorkbook|createSheet", body):
        ops.append("导出 Excel")
    if re.search(r"MultipartFile", body):
        ops.append("接收上传文件")
    if re.search(r"selectPage|\.page\(|new Page<", body):
        ops.append("分页查询")
    if re.search(r"\.insert\(", body):
        ops.append("新增")
    if re.search(r"\.updateById\(|\.update\(", body):
        ops.append("更新")
    if re.search(r"\.deleteById\(|\.delete\(", body):
        ops.append("删除")
    if not ops:
        ops = {"GET": ["查询"], "POST": ["提交/新增"], "PUT": ["更新"], "DELETE": ["删除"]}.get(method, ["处理"])
    tgt = "（" + cn(next(iter(tables))) + "）" if len(tables) == 1 else ""
    return "、".join(ops) + tgt


def fmt_tables(ts, infra=None):
    body = "、".join("%s（%s）" % (t, cn(t)) for t in sorted(ts)) if ts else ""
    extra = "、".join("%s（%s）" % (t, cn(t)) for t in sorted(infra or ()))
    if body and extra:
        return body + "<br>另读（权限/范围/审计）：" + extra
    if body:
        return body
    if extra:
        return "仅横切表：" + extra
    return "-"


out = []
out.append("# OA 办公系统 · 后端接口文档")
out.append("")
out.append("> 生成脚本：§ scripts/gen-api-docs.py §　｜　扫描范围：§ OfficeAutomationSystemAPI/src/main/java/com/officeautomationsystemapi/controller/*.java §　｜　刷新：§ python scripts\\gen-api-docs.py §")
out.append("")
out.append("每个 Controller 先给一张**接口一览表**（定义文件+行号、请求方法+完整路径、处理函数、入参、权限码、行为、涉及的数据库表、返回类型），再逐个接口贴**带行号的完整源码**并写**逐段讲解**：每段说明这段代码取了什么、判了什么、写了哪张表。改完 Controller 重跑脚本即可刷新。")
out.append("")
out.append("## 通用约定")
out.append("")
out.append("- 完整路径已含 § /api § 前缀（§ application.yml § 的 context-path）。除免登录白名单外都要带 § Authorization: Bearer <token> §；白名单见 § config/WebMvcConfig.java § 的 § excludePathPatterns §。")
out.append("- 统一返回 § { code, message, data } §；分页接口的 § data § 多为 § { list, total, pageNum, pageSize } §。")
out.append("- 权限码来自 § @RequirePermission §，多个时**任一命中即可**；§ role_code='admin' § 直接放行。")
out.append("- 入参写法：§ 注解 名称 类型（说明） §；§ 查询/表单 § 是 query 或 form-data，§ JSON 体 § 是 @RequestBody，§ 路径参数 § 是 @PathVariable。")
out.append("- 「涉及表」是静态推断：Controller 直接调用的 Mapper/Repository + 它所调 Service 方法内部实际访问的表 + 方法体内写死的 SQL，三者取并集；权限校验、数据范围、审计这类每请求都会跑的链路读到的表单独列在「另读」里，不算本接口的业务表。纯文件流接口（上传、下载图片）确实不碰表。")
out.append("- 表格行序与源码里接口的定义顺序一致（不按路径排序），方便对照文件逐条核对。")
out.append("")
out.append("## 读代码前的几个套路")
out.append("")
out.append("下面这些写法在整个后端反复出现，先认下来，后面看任何一段源码都不会卡。")
out.append("")
out.append("**1. 四层各管一件事**：§controller§ 只负责收参数、调 Service、拼返回值；§service/impl§ 放业务规则（能不能做、要不要抛异常、加不加事务）；§repository§ 是 MyBatis-Plus 的 §BaseMapper§，方法名就是 SQL 的意思（§selectById§、§selectPage§、§insert§）；§entity§ 一个类对应一张表，§vo§ 是给前端看的对象（常比实体多几个字段，比如部门名、角色名）。")
out.append("")
out.append("**2. 返回值永远是 §Result§**：§Result.success(data)§ 包成 §{code:200, message, data}§，§Result.error(\"原因\")§ 包成失败。前端拦截器看到 code 不是 200 就直接弹提示，所以业务失败不需要 Controller 写 try-catch，抛异常更省事。")
out.append("")
out.append("§§§java")
out.append("return Result.success(vo);          // 成功，data 里放返回体")
out.append("return Result.error(\"文件为空\");    // 失败，message 直接弹给用户")
out.append("§§§")
out.append("")
out.append("**3. 权限是注解 + 拦截器**：§@RequirePermission(\"user:add\")§ 只做标记，真正校验在 §interceptor/LoginInterceptor.java§ —— 先验 token，再查这个人的角色权限和用户级授权有没有命中码；role_code 是 admin 的直接放行。所以文档里的「权限码」就是 §menus§ 表里的 §permission_code§。")
out.append("")
out.append("**4. 当前登录人从 request 里拿**：拦截器把 §userId§ 塞进 request attribute，Controller 用 §RequestUser.id(request)§ 取，不要自己解 token。")
out.append("")
out.append("§§§java")
out.append("Long uid = RequestUser.id(request);   // 当前登录人主键")
out.append("§§§")
out.append("")
out.append("**5. 参数四种收法**：§@RequestBody§ 收 JSON 体；§@RequestParam§ 收 query 或表单字段；§@PathVariable§ 收路径里的 §{id}§；§MultipartFile§ 收上传文件。文档「入参」里的「查询/表单」「JSON 体」「路径参数」就是从这些注解来的。")
out.append("")
out.append("**6. 业务错误抛 §BusinessException§**：§exception/GlobalExceptionHandler.java§ 统一接住，转成 §Result.error(400, message)§，并且把异常信息换成安全文案，不会把 SQL 或表名漏给前端。")
out.append("")
out.append("§§§java")
out.append("if (user == null) {")
out.append("    throw new BusinessException(\"用户不存在\");   // 前端会看到这句提示")
out.append("}")
out.append("§§§")
out.append("")
out.append("**7. 条件查询靠 Wrapper 拼**：§new QueryWrapper<Users>().eq(\"status\", 1).like(\"name\", keyword)§ 相当于 §WHERE status = 1 AND name LIKE %keyword%§。看到 §lambdaQuery()§ 是同一个东西的 lambda 写法，§Users::getStatus§ 指的就是 §status§ 列。")
out.append("")
out.append("**8. 分页交给插件**：方法参数收 §pageNum/pageSize§，Service 里 §new Page<>(pageNum, pageSize)§ 传给 §selectPage§，返回的 §Page§ 自带 §records/total/current/size§，所以分页接口的返回类型常见 §Result<Page<UserVO>>§。")
out.append("")
out.append("**9. 事务加在 Service 方法上**：§@Transactional(rollbackFor = Exception.class)§ 表示这个方法里任何一步抛异常，前面的写库全部回滚。Controller 里看不到事务注解，不代表没有。")
out.append("")
out.append("**10. 写操作会自动留痕**：§interceptor/AuditLogInterceptor.java§ 对非 GET 请求统一往 §operation_logs§ 写一条（谁、哪个模块、哪个接口、IP、成败），所以新增/修改/删除接口的方法体里看不到手写日志。")
out.append("")
out.append("**11. 导出接口直接写响应流**：返回 §void§、参数带 §HttpServletResponse§ 的，基本都是导出 Excel —— 用 §XSSFWorkbook§ 建表、§response.getOutputStream()§ 写出，前端必须按 blob 收。")
out.append("")
out.append("**12. 手机号、邮箱会被脱敏**：§service/SensitiveDataService.java§ 按「本人、或持 §user:sensitive:view§ 码的人看明文，其他人打码」处理，所以同一接口不同人看到的手机号可能不一样。")
out.append("")
out.append("> 每条接口下面的**逐段讲解**是按 §// 中文注释§ 切段的：先给真实行号范围，再给那句注释，再列出这段代码实际调了谁、碰了哪张表、什么条件下抛异常。代码块本身带行号，可以直接对着 IDE 找。")
out.append("")
total = sum(len(c["rows"]) for c in controllers)
out.append("## 总览：%d 个接口 / %d 个 Controller" % (total, len(controllers)))
out.append("")
out.append("| 模块 | Controller | 源文件 | 类级前缀 | 接口数 |")
out.append("| --- | --- | --- | --- | --- |")
for title, lst in ordered:
    for c in lst:
        out.append("| %s | § %s § | § %s § | § %s § | %d |" % (title, c["name"], c["file"].split("/")[-1], c["prefix"] or "-", len(c["rows"])))
out.append("")
for title, lst in ordered:
    out.append("## " + title)
    out.append("")
    for c in lst:
        out.append("### " + c["name"])
        out.append("")
        out.append("源文件：§ %s §（%d 个接口）%s" % (
            c["file"], len(c["rows"]),
            ("　类级前缀：§ " + c["prefix"] + " §") if c["prefix"] else ""))
        out.append("")
        out.append("**接口一览**")
        out.append("")
        out.append("| # | 接口 | 处理函数 | 行号 | 入参 | 权限码 | 行为 | 涉及表（英文/中文） | 返回 |")
        out.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        for idx, r in enumerate(c["rows"], 1):
            ps = "<br>".join("%s %s § %s §（%s）" % (p[0], p[1], p[2], p[3]) for p in r["params"]) or "-"
            b = behavior_of(r["note"], r["body"], r["method"], r["tables"])
            out.append("| %d | %s § %s § | § %s § | L%d–L%d | %s | %s | %s | %s | § %s § |" % (
                idx, r["method"], r["path"] or "-", r["fn"] or "-", r["start_line"], r["end_line"],
                ps, r["perm"] or "-", b, fmt_tables(r["tables"], r["infra"]), (r["ret"] or "-")[:40]))
        out.append("")
        out.append("**逐个接口：源码 + 讲解**")
        out.append("")
        for idx, r in enumerate(c["rows"], 1):
            b = behavior_of(r["note"], r["body"], r["method"], r["tables"])
            out.append("#### %d. %s § %s §" % (idx, r["method"], r["path"] or "-"))
            out.append("")
            out.append("- **所在文件**：§ %s:%d-%d §" % (c["file"], r["start_line"], r["end_line"]))
            out.append("- **处理函数**：§ %s §　**返回**：§ %s §" % (r["fn"] or "-", (r["ret"] or "-")))
            ps2 = "；".join("%s %s § %s §（%s）" % (p[0], p[1], p[2], p[3]) for p in r["params"]) or "无"
            out.append("- **入参**：%s" % ps2)
            if r["perm"]:
                out.append("- **权限码**：§ %s §（多个时命中任一即可，admin 直接放行）" % r["perm"])
            else:
                out.append("- **权限码**：无，登录即可访问")
            out.append("- **涉及表**：%s" % fmt_tables(r["tables"], r["infra"]))
            out.append("- **一句话**：" + b)
            ch = chain_of(r["body"], c["name"], r["fn"], c["fields"])
            if len(ch) > 1:
                out.append("- **往下调**：" + " → ".join("§ %s §" % x if x != "…" else "…" for x in ch))
            out.append("")
            out.append("§§§java")
            out.extend(numbered(r["code"] or "// 没取到源码", r["start_line"]))
            out.append("§§§")
            out.append("")
            ex = explain(r["code"] or "", r["start_line"], c["fields"], c["name"], r.get("fn"))
            if ex:
                out.append("**逐段讲解**")
                out.append("")
                out.extend(ex)
                out.append("")
out.append("## 附录：全部数据表（英文 / 中文）")
out.append("")
out.append("| 英文表名 | 中文表名 |")
out.append("| --- | --- |")
for t in sorted(ALL_TABLES):
    out.append("| § %s § | %s |" % (t, cn(t)))
out.append("")
(DOCS / "接口文档-后端.md").write_text("\n".join(out).replace(SEC, TICK), encoding="utf-8")

def split_top(expr, delim):
    out, cur, depth, quote, esc = [], "", 0, None, False
    for ch in expr:
        if quote:
            cur += ch
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == quote:
                quote = None
            continue
        if ch in "\"'\u0060":
            quote = ch
            cur += ch
        elif ch in "([{":
            depth += 1
            cur += ch
        elif ch in ")]}":
            depth -= 1
            cur += ch
        elif ch == delim and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    out.append(cur)
    return out


def first_arg(expr):
    return split_top(expr, ",")[0]


def build_url(expr):
    parts = []
    for p in split_top(expr, "+"):
        p = p.strip()
        if not p:
            continue
        if len(p) >= 2 and p[0] in "\"'\u0060" and p[-1] == p[0]:
            s = re.sub(r"\$\{[^}]*\}", "{v}", p[1:-1])
            parts.append(s)
        else:
            parts.append("{v}")
    return "".join(parts)


def ts_end(text, i):
    """i 指向参数右括号之后，返回函数结束位置（含右花括号/分号）"""
    n = len(text)
    j = i
    while j < n and text[j] in " \t\r\n":
        j += 1
    if j < n and text[j] == ":":
        while j < n and text[j] not in "{=":
            j += 1
    if text[j: j + 2] == "=>":
        j += 2
        while j < n and text[j] in " \t\r\n":
            j += 1
    if j < n and text[j] == "{":
        e = balanced(text, j, "{", "}")
        return e if e > 0 else n
    # 箭头函数表达式体：到本行末尾或分号为止（不写分号的写法很常见）
    k, d = j, 0
    while k < n:
        ch = text[k]
        if ch in "([{":
            d += 1
        elif ch in ")]}":
            d -= 1
        elif d <= 0 and (ch == ";" or ch == chr(10)):
            return k + 1 if ch == ";" else k
        k += 1
    return n


def ts_head(text, pos):
    """把紧挨在函数上面的 // 注释一起带上"""
    out = pos
    while True:
        ls = text.rfind(chr(10), 0, out - 1) + 1
        line = text[ls: out].strip()
        if line.startswith("//") or line.startswith("/*") or line.startswith("*"):
            out = ls
        else:
            break
        if ls == 0:
            break
    return out


def fe_call(txt):
    """在一段前端代码里找 request.xxx<T>(url, opts)，返回 (方法, 路径, 调用参数原文)"""
    m = re.search(r"request\.(get|post|put|delete|patch)", txt)
    if not m:
        return None
    i = m.end()
    while i < len(txt) and txt[i] in " \t":
        i += 1
    if i < len(txt) and txt[i] == "<":
        e = balanced(txt, i, "<", ">")
        if e > 0:
            i = e
    while i < len(txt) and txt[i] in " \t\r\n":
        i += 1
    if i >= len(txt) or txt[i] != "(":
        return (m.group(1).upper(), "", "")
    close = balanced(txt, i, "(", ")")
    args = txt[i + 1: close - 1] if close > 0 else ""
    return (m.group(1).upper(), build_url(first_arg(args)), args)


def fe_explain(code, first_line, ret):
    """前端封装函数的逐段讲解：请求方法+路径、query/body、泛型含义"""
    out = []
    for b in split_blocks(code, first_line):
        txt = b["text"]
        items = []
        call = fe_call(txt)
        if call:
            mm, url, cargs = call
            opt = ""
            if re.search(r"\bparams\b", cargs):
                pm = re.search(r"params:\s*\{([^}]*)\}", cargs)
                if pm and pm.group(1).strip():
                    keys = [k.split(":")[0].strip() for k in pm.group(1).split(",") if k.strip()]
                    opt = "，query 参数 " + "、".join("§%s§" % k for k in keys[:6])
                elif re.search(r"\{\s*params\s*\}", cargs):
                    opt = "，把入参 §params§ 整个对象交给 axios 当 query 拼到地址后面"
                else:
                    opt = "，query 参数走 §params§"
            elif re.search(r"multipart|new FormData", cargs):
                opt = "，请求体是 §multipart/form-data§ 表单（不是 JSON）"
            elif re.search(r"\bdata\b", cargs):
                dm = re.search(r",\s*data\s*[,)]", cargs)
                opt = ("，请求体直接把 §data§ 交给 axios 序列化成 JSON" if dm
                       else "，请求体走 §data§")
            items.append("请求 §%s %s§%s" % (mm, url, opt))
        else:
            iv = re.findall(r"\\$\\{(\\w+)\\}", txt)
            if iv:
                items.append("地址里的 " + "、".join("§%s§" % v for v in iv[:4]) + " 由入参拼进来")
        if re.search(r"new FormData", txt):
            items.append("用 §new FormData()§ 组装表单，§form.append(\"file\", file)§ 把文件挂上去")
        if re.search(r"Content-Type[\"']?\s*:\s*[\"']multipart", txt):
            items.append("header 里写明 §multipart/form-data§，后端才按文件流解析")
        if ret and re.search(r"request\.\w+\s*<", txt):
            items.append("泛型 §%s§ 只是给 TS 标注的返回类型，运行时拿到的是后端 §data§ 那一层" % ret)
        if b["head"]:
            items.append("**%s**" % b["head"])
        if not items:
            continue
        rng = "L%d" % b["start"] if b["start"] == b["end"] else "L%d–L%d" % (b["start"], b["end"])
        out.append("- %s　%s" % (rng, "；".join(items)))
    return out


req_re = re.compile("request\\.(get|post|put|delete|patch)\\s*(<)?")
frontend = []
for f in sorted(WEB.glob("*.ts")):
    text = f.read_text(encoding="utf-8")
    rows = []
    for m in req_re.finditer(text):
        i = m.end()
        gen = ""
        if m.group(2):
            end = balanced(text, m.start() + len(m.group(0)) - 1, "<", ">")
            if end > 0:
                gen = text[m.start() + len(m.group(0)): end - 1]
                i = end
        cm = re.compile(r"\s*\(").match(text, i)
        if not cm:
            continue
        close = balanced(text, cm.end() - 1, "(", ")")
        call_args = text[cm.end(): close - 1]
        url = build_url(first_arg(call_args))
        method = m.group(1).upper()
        name, args = "", ""
        found = None
        for fm in re.finditer(r"export\s+(?:async\s+)?function\s+(\w+)\s*\(|export\s+const\s+(\w+)\s*=\s*(?:async\s*)?\(", text[:m.start()]):
            found = fm
        if found:
            name = found.group(1) or found.group(2) or ""
            op = found.end() - 1
            ce = balanced(text, op, "(", ")")
            args = re.sub(r"\s+", " ", text[op + 1: ce - 1]).strip() if ce > 0 else ""
        ret = ""
        if gen:
            top, d, cur = [], 0, ""
            for ch in gen:
                if ch == "<":
                    d += 1
                elif ch == ">":
                    d -= 1
                if ch == "," and d == 0:
                    top.append(cur.strip())
                    cur = ""
                else:
                    cur += ch
            top.append(cur.strip())
            ret = top[-1]
        how = "multipart/form-data 表单" if re.search(r"multipart|new FormData", call_args) else (
            "查询参数 params" if re.search(r"\bparams\b", call_args) else (
            "JSON body" if re.search(r"\bdata\b", call_args) else (
                "无参数，靠路径里的占位符" if method in ("GET", "DELETE") else "参数直接拼进地址")))
        note = ""
        for line in reversed(text[:m.start()].splitlines()[-4:]):
            s = line.strip()
            if s.startswith("//"):
                note = s.lstrip("/ ").strip()
                break
            if s.startswith("export") or s.startswith("}") or s.startswith("import"):
                break
        code, sline, eline = "", 0, 0
        if found:
            st = ts_head(text, found.start())
            ce2 = balanced(text, found.end() - 1, "(", ")")
            en = ts_end(text, ce2 if ce2 > 0 else found.end())
            code = text[st:en].rstrip()
            sline = text.count(chr(10), 0, st) + 1
            eline = text.count(chr(10), 0, en) + 1
        ret = re.sub(r"//[^" + chr(10) + r"]*", "", ret)
        ret = re.sub(r"\s+", " ", ret).strip()
        if len(ret) > 150:
            ret = ret[:150] + "…"
        rows.append({"fn": name, "method": method, "url": url, "args": args, "ret": ret,
                     "how": how, "note": note, "code": code,
                     "start_line": sline, "end_line": eline})
    if rows:
        frontend.append({"file": str(f.relative_to(ROOT)).replace("\\", "/"), "name": f.stem, "rows": rows})


def norm(p):
    p = p.split("?")[0].strip()
    p = re.sub(r"\$\{[^}]+\}", "{v}", p)
    p = re.sub(r"\{[^}]+\}", "{v}", p)
    if not p.startswith("/"):
        p = "/" + p
    return p


be_map = {}
for c in controllers:
    for r in c["rows"]:
        k = norm(r["path"])
        for key in ((r["method"], k), ("", k)):
            biz, infra = be_map.setdefault(key, (set(), set()))
            biz.update(r["tables"])
            infra.update(r["infra"])


def fe_tables(method, url):
    p = norm(url)
    cands = [p, "/api" + p if not p.startswith("/api/") else p[4:]]
    for c in cands:
        hit = be_map.get((method, c)) or be_map.get(("", c))
        if hit and (hit[0] or hit[1]):
            return set(hit[0]), set(hit[1])
    suffix = []
    for (m2, k), ts in be_map.items():
        if m2 not in (method, ""):
            continue
        if k.endswith(p) or p.endswith(k):
            suffix.append(ts)
    if len(suffix) == 1:
        return set(suffix[0][0]), set(suffix[0][1])
    return set(), set()


FE_MODULES = [
 ("一、认证与个人中心", ["auth"]),
 ("二、组织、用户与权限", ["user", "role"]),
 ("三、流程引擎", ["flow"]),
 ("四、考勤", ["attendance"]),
 ("五、工作日志", ["worklog"]),
 ("六、任务与项目", ["task", "project"]),
 ("七、公告与通知", ["announcement", "notification"]),
 ("八、财务", ["finance"]),
 ("九、招聘", ["recruitment"]),
 ("十、报表", ["report"]),
 ("十一、系统管理", ["system", "online", "loginLog"]),
]
fe_by_name = {c["name"]: c for c in frontend}
fe_ordered, fplaced = [], set()
for title, names in FE_MODULES:
    lst = [fe_by_name[n] for n in names if n in fe_by_name]
    fplaced |= set(names)
    if lst:
        fe_ordered.append((title, lst))
frest = [c for c in frontend if c["name"] not in fplaced]
if frest:
    fe_ordered.append(("十二、其他", frest))

fo = []
fo.append("# OA 办公系统 · 前端接口文档")
fo.append("")
fo.append("> 生成脚本：§ scripts/gen-api-docs.py §　｜　扫描范围：§ OfficeAutomationSystemWeb/src/api/*.ts §　｜　刷新：§ python scripts\\gen-api-docs.py §")
fo.append("")
fo.append("每条封装给出：**所在 api 文件与行号、函数名、请求方法+路径、入参（TS 签名原文）、传参方式、行为、对应后端接口读写的数据库表（英文表名+中文表名）、返回 data 类型，并附完整源码与逐段讲解**。")
fo.append("")
fo.append("## 通用约定")
fo.append("")
fo.append("- 所有函数最终走 § src/utils/request.ts § 的 axios 实例：§ baseURL=/api §、自动带 token；失败按后端 § message § 弹提示，401 清登录态跳登录页；成功时直接把 § data § 交给调用方。")
fo.append("- 路径是相对 § baseURL § 的部分，完整地址是 § /api § 加该路径；路径里的模板占位符由调用方传入。")
fo.append("- 「入参」列是 TS 函数签名原文；「传参方式」标明走 query 还是 JSON body。")
fo.append("- 「涉及表」由同一路径的后端接口反查得到，反映这次前端调用最终会读写的表。")
fo.append("")
fo.append("## 读代码前的几个套路")
fo.append("")
fo.append("**1. 这里的 §request§ 不是 axios 本体，是包装过的实例**（§ src/utils/request.ts §）：baseURL 写死 §/api §，请求拦截器自动加 §Authorization: Bearer token§，响应拦截器判断后端 §Result.code===200§ 才把 §data§ 交出来。")
fo.append("")
fo.append("§§§ts")
fo.append("import request from \"@/utils/request\"   // 全项目共用这一个实例")
fo.append("§§§")
fo.append("")
fo.append("**2. 泛型写两个参数，只有第二个有用**：§request.get<any, PageResult<X>>(...)§ 里第一个 §any§ 是 axios 自己的响应类型，第二个才是函数返回给你的类型；类型只是给编辑器看的，运行时拿到的就是后端 §data§ 那一层，不用再写 §.data.data§。")
fo.append("")
fo.append("**3. params 走 query，data 走 JSON body**：")
fo.append("")
fo.append("§§§ts")
fo.append("// 拼成 /api/finance/payments/records?pageNum=1&pageSize=10")
fo.append("request.get<any, PageResult<Item>>(\"/finance/payments/records\", { params: { pageNum: 1, pageSize: 10 } })")
fo.append("")
fo.append("// 请求体是 JSON：{\"username\":\"admin\",\"password\":\"***\"}")
fo.append("request.post<any, LoginResult>(\"/auth/login\", data)")
fo.append("§§§")
fo.append("")
fo.append("**4. 路径里的占位符由调用方拼**：TS 模板字符串里 §${id}§ 是运行时插值，不是构建变量；本文档表格统一记成 §{v}§，实际调用时把真实 id 填进去，比如 §/announcements/12/read§。")
fo.append("")
fo.append("**5. 出错不用自己判**：后端返回 §code!==200§ 时拦截器已经弹过 §message§ 并 reject，所以封装函数里只有一行 §return§，调用方用 §try/catch§ 或 §.catch§ 接住即可；401 会清掉 sessionStorage 并跳 §#/login§。")
fo.append("")
fo.append("**6. interface 描述的是 data 的形状**：§PaymentRecordItem§ 这类接口字段与后端 VO 一一对应，只是为了让 §v-for§ 和表格列有类型提示，不影响运行时。")
fo.append("")
tf = sum(len(c["rows"]) for c in frontend)
fo.append("## 总览：%d 个封装函数 / %d 个 api 文件" % (tf, len(frontend)))
fo.append("")
fo.append("| 模块 | api 文件 | 函数数 |")
fo.append("| --- | --- | --- |")
for title, lst in fe_ordered:
    for c in lst:
        fo.append("| %s | § %s § | %d |" % (title, c["file"].split("/")[-1], len(c["rows"])))
fo.append("")
for title, lst in fe_ordered:
    fo.append("## " + title)
    fo.append("")
    for c in lst:
        fo.append("### " + c["name"])
        fo.append("")
        fo.append("源文件：§ %s §" % c["file"])
        fo.append("")
        fo.append("#### 接口一览")
        fo.append("")
        fo.append("| # | 函数 | 接口 | 行号 | 入参 | 传参方式 | 行为 | 涉及表（英文/中文） | 返回 data |")
        fo.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        for idx, r in enumerate(c["rows"], 1):
            biz, infra = fe_tables(r["method"], r["url"])
            b = behavior_of(r["note"], "", r["method"], set())
            rng = ("L%d–L%d" % (r["start_line"], r["end_line"])) if r["start_line"] else "-"
            fo.append("| %d | § %s § | %s § %s § | %s | %s | %s | %s | %s | § %s § |" % (
                idx, r["fn"] or "-", r["method"], r["url"] or "-", rng,
                ("§ " + r["args"] + " §") if r["args"] else "-", r["how"],
                r["note"] or b, fmt_tables(biz, infra), r["ret"] or "-"))
        fo.append("")
        fo.append("#### 逐个函数：源码 + 讲解")
        fo.append("")
        for idx, r in enumerate(c["rows"], 1):
            biz, infra = fe_tables(r["method"], r["url"])
            fo.append("##### %d. §%s§" % (idx, r["fn"] or "-"))
            fo.append("")
            fo.append("- **所在文件**：§%s:%d-%d§" % (c["file"], r["start_line"], r["end_line"]))
            fo.append("- **接口**：%s §%s§　**返回 data**：§%s§" % (
                r["method"], r["url"] or "-", r["ret"] or "void"))
            fo.append("- **入参**：%s　**传参方式**：%s" % (
                ("§" + r["args"] + "§") if r["args"] else "无", r["how"]))
            fo.append("- **涉及表**：%s" % fmt_tables(biz, infra))
            fo.append("")
            if r["code"]:
                fo.append("§§§ts")
                fo.extend(numbered(r["code"], r["start_line"]))
                fo.append("§§§")
            ex = fe_explain(r["code"] or "", r["start_line"], r["ret"])
            if ex:
                fo.append("")
                fo.append("**逐段讲解**")
                fo.extend(ex)
            fo.append("")
(DOCS / "接口文档-前端.md").write_text("\n".join(fo).replace(SEC, TICK), encoding="utf-8")

print("backend:", len(controllers), "controllers,", total, "interfaces")
print("frontend:", len(frontend), "files,", tf, "functions")
print("backend endpoints without table:", sum(1 for c in controllers for r in c["rows"] if not r["tables"]))
print("backend endpoints only infra:", sum(1 for c in controllers for r in c["rows"] if not r["tables"] and r["infra"]))
print("frontend functions without table:", sum(1 for c in frontend for r in c["rows"] if not any(fe_tables(r["method"], r["url"]))))
print("classes registered:", len(CLASSES))
