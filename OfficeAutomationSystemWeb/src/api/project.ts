import request from "@/utils/request";

export const getProjects = (params: any) => request.get<any, any>('/projects', { params })
export const getProjectOptions = () => request.get<any, any>('/projects/options')
export const getProjectMemberOptions = () => request.get<any, any>('/projects/member-options')
export interface ProjectMemberTaskAccess {
  user_id: number
  real_name: string
  role: number
  task_access: boolean
}
export const getProjectMemberTaskAccess = (id: number) =>
  request.get<any, ProjectMemberTaskAccess[]>(`/projects/${id}/member-task-access`)
export const getProjectDetail = (id: number) => request.get<any, any>(`/projects/${id}`)
export const createProject = (data: any) => request.post<any, any>('/projects', data)
export const updateProject = (id: number, data: any) => request.put<any, any>(`/projects/${id}`, data)
export const deleteProject = (id: number) => request.delete<any, any>(`/projects/${id}`)
export const changeProjectStatus = (id: number, action: string) =>
  request.post<any, any>(`/projects/${id}/status`, { action })
export const addProjectMembers = (id: number, userIds: number[]) =>
  request.post<any, any>(`/projects/${id}/members`, { user_ids: userIds })
export const removeProjectMember = (id: number, memberUserId: number) =>
  request.delete<any, any>(`/projects/${id}/members/${memberUserId}`)
export const getProjectStatistics = (id: number) => request.get<any, any>(`/projects/${id}/statistics`)