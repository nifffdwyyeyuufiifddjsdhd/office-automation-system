import request from "@/utils/request"

// 招聘岗位：人事维护的在招职位
export interface RecruitmentPositionItem {
    id: number
    positionName: string
    deptId: number | null
    deptName: string | null
    positionId: number | null
    positionLevelName: string | null
    headcount: number
    education: string | null
    experienceYears: number | null
    salaryRange: string | null
    workPlace: string | null
    duty: string | null
    requirement: string | null
    status: number
    statusText: string
    publishTime: string | null
    deadline: string | null
    remark: string | null
    candidateCount: number
    hiredCount: number
}

// 候选人：一人对一个岗位，状态从待筛选到已入职
export interface RecruitmentCandidateItem {
    id: number
    positionId: number
    positionName: string | null
    deptName: string | null
    candidateName: string
    gender: number
    phone: string | null
    email: string | null
    education: string | null
    experienceYears: number | null
    source: string | null
    resumePath: string | null
    status: number
    statusText: string
    interviewTime: string | null
    interviewer: string | null
    evaluation: string | null
    expectedSalary: number | null
    remark: string | null
    createdAt: string
}

export interface RecruitmentOverview {
    openPositions: number
    headcount: number
    screening: number
    interview: number
    passed: number
    hired: number
    rejected: number
}

export interface PageData<T> {
    records: T[]
    total: number
    pageNum: number
    pageSize: number
}

// 岗位列表
export function pagePositions(params: Record<string, unknown>) {
    return request.get<any, PageData<RecruitmentPositionItem>>("/recruitment/positions", { params })
}

// 岗位下拉（候选人页选应聘岗位用）
export function positionOptions() {
    return request.get<any, RecruitmentPositionItem[]>("/recruitment/positions/options")
}

export function createPosition(data: Record<string, unknown>) {
    return request.post<any, number>("/recruitment/positions", data)
}

export function updatePosition(id: number, data: Record<string, unknown>) {
    return request.put<any, void>("/recruitment/positions/" + id, data)
}

export function updatePositionStatus(id: number, status: number) {
    return request.put<any, void>("/recruitment/positions/" + id + "/status", { status })
}

export function deletePosition(id: number) {
    return request.delete<any, void>("/recruitment/positions/" + id)
}

// 候选人列表
export function pageCandidates(params: Record<string, unknown>) {
    return request.get<any, PageData<RecruitmentCandidateItem>>("/recruitment/candidates", { params })
}

export function createCandidate(data: Record<string, unknown>) {
    return request.post<any, number>("/recruitment/candidates", data)
}

export function updateCandidate(id: number, data: Record<string, unknown>) {
    return request.put<any, void>("/recruitment/candidates/" + id, data)
}

// 状态流转：body 里可以带面试时间、面试官、面试评价
export function updateCandidateStatus(id: number, data: Record<string, unknown>) {
    return request.put<any, void>("/recruitment/candidates/" + id + "/status", data)
}

export function deleteCandidate(id: number) {
    return request.delete<any, void>("/recruitment/candidates/" + id)
}

// 招聘概览：首页统计卡
export function getRecruitmentOverview() {
    return request.get<any, RecruitmentOverview>("/recruitment/overview")
}