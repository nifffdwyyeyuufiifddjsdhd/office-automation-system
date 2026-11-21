// 公告正文的模板与主题
// 正文只留结构标签，后端 Jsoup 会清掉 style、class、align
// 模板里只写标题、段落、列表、表格，样式交给 4 套主题

export interface AnnouncementTheme {
    key: string
    name: string
    desc: string
}

// 主题名存进 announcements.theme，渲染时给容器加上 ann-theme-xxx
export const ANNOUNCEMENT_THEMES: AnnouncementTheme[] = [
    { key: 'standard', name: '标准公文', desc: '白底、蓝色竖线标题，日常公告用它' },
    { key: 'notice', name: '通知通告', desc: '米黄底、居中橙色标题，适合放假、停水停电类通知' },
    { key: 'academic', name: '培训学习', desc: '浅蓝底、圆角引用块，适合培训、分享、活动' },
    { key: 'finance', name: '制度文件', desc: '浅灰底、墨绿标题、双线表格，适合制度、办法、规定' }
]

export function themeClass(theme?: string | null): string {
    const key = theme || 'standard'
    const exist = ANNOUNCEMENT_THEMES.some(t => t.key === key)
    return 'ann-body ann-theme-' + (exist ? key : 'standard')
}

export interface AnnouncementTemplate {
    key: string
    name: string
    desc: string
    html: string
}

// 6 个固定骨架，点一下铺好结构，只改文字
export const ANNOUNCEMENT_TEMPLATES: AnnouncementTemplate[] = [
    {
        key: 'notice',
        name: '一般通知',
        desc: '标题 + 称呼 + 事项 + 特此通知',
        html: '<h2>关于××的通知</h2>'
            + '<p>各位同事：</p>'
            + '<p>为××，现将有关事项通知如下：</p>'
            + '<ol>'
            + '<li>时间：××年××月××日</li>'
            + '<li>地点：××</li>'
            + '<li>要求：××</li>'
            + '</ol>'
            + '<p>请相关人员按时参加，如有疑问请联系××。</p>'
            + '<p>特此通知。</p>'
            + '<p>××部门</p>'
            + '<p>××年××月××日</p>'
    },
    {
        key: 'meeting',
        name: '会议通知',
        desc: '带会议信息表格',
        html: '<h2>关于召开××会议的通知</h2>'
            + '<p>各部门：</p>'
            + '<p>经研究决定召开××会议，具体安排如下：</p>'
            + '<table><tbody>'
            + '<tr><th>会议时间</th><td>××年××月××日 09:00</td></tr>'
            + '<tr><th>会议地点</th><td>××会议室</td></tr>'
            + '<tr><th>参会人员</th><td>各部门负责人</td></tr>'
            + '<tr><th>会议议题</th><td>1. ××；2. ××</td></tr>'
            + '</tbody></table>'
            + '<p>请参会人员提前十分钟入场，手机调至静音。</p>'
            + '<p>××办公室</p>'
    },
    {
        key: 'appointment',
        name: '人事任免',
        desc: '岗位调整、任命类',
        html: '<h2>关于××等同志职务调整的通知</h2>'
            + '<p>各部门：</p>'
            + '<p>经公司研究决定：</p>'
            + '<ul>'
            + '<li>××同志任××部门××岗位，免去其原××职务；</li>'
            + '<li>××同志任××部门××岗位。</li>'
            + '</ul>'
            + '<p>以上调整自××年××月××日起执行。</p>'
            + '<p>人事部</p>'
    },
    {
        key: 'policy',
        name: '制度发布',
        desc: '章节式条款，适合办法、规定',
        html: '<h2>××管理办法</h2>'
            + '<h3>一、适用范围</h3>'
            + '<p>本办法适用于公司全体员工。</p>'
            + '<h3>二、主要要求</h3>'
            + '<ol>'
            + '<li>××；</li>'
            + '<li>××；</li>'
            + '<li>××。</li>'
            + '</ol>'
            + '<h3>三、生效日期</h3>'
            + '<p>本办法自××年××月××日起施行，由××部门负责解释。</p>'
    },
    {
        key: 'training',
        name: '培训活动',
        desc: '带报名信息的表格',
        html: '<h2>××培训报名通知</h2>'
            + '<p>为提升员工××能力，公司组织专题培训：</p>'
            + '<table><tbody>'
            + '<tr><th>培训时间</th><td>××月××日 14:00-16:00</td></tr>'
            + '<tr><th>培训地点</th><td>××培训室</td></tr>'
            + '<tr><th>主讲人</th><td>××</td></tr>'
            + '<tr><th>报名方式</th><td>××月××日前联系××报名</td></tr>'
            + '</tbody></table>'
            + '<blockquote>培训结束后安排随堂测试，请提前安排好手头工作。</blockquote>'
    },
    {
        key: 'blank',
        name: '空白正文',
        desc: '清空正文从头写',
        html: ''
    }
]
