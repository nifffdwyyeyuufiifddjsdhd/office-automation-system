import request from "@/utils/request";

export const getTasks = (params: any) => request.get<any, any>('/tasks', { params })
export const getTaskDetail = (id: number) => request.get<any, any>(`/tasks/${id}`)
export const createTask = (data: any) => request.post<any, any>('/tasks', data)
export const updateTask = (id: number, data: any) => request.put<any, any>(`/tasks/${id}`, data)
export const reassignTask = (id: number, data: any) => request.post<any, any>(`/tasks/${id}/reassign`, data)
export const acceptTask = (id: number) => request.post<any, any>(`/tasks/${id}/accept`)
export const claimTask = (id: number) => request.post<any, any>(`/tasks/${id}/claim`)
export const rejectTask = (id: number, data?: any) => request.post<any, any>(`/tasks/${id}/reject`, data || {})
export const updateProgress = (id: number, data: any) => request.put<any, any>(`/tasks/${id}/progress`, data)
export const submitTask = (id: number, data?: any) => request.post<any, any>(`/tasks/${id}/submit`, data || {})
export const approveTask = (id: number) => request.post<any, any>(`/tasks/${id}/approve`)
export const rejectApproveTask = (id: number, data?: any) => request.post<any, any>(`/tasks/${id}/reject-approve`, data || {})
export const revokeTask = (id: number) => request.post<any, any>(`/tasks/${id}/revoke`)
export const addComment = (id: number, data: any) => request.post<any, any>(`/tasks/${id}/comments`, data)
export const addTaskMember = (id: number, data: any) => request.post<any, any>(`/tasks/${id}/members`, data)
export const removeTaskMember = (id: number, userId: number) => request.delete<any, any>(`/tasks/${id}/members/${userId}`)
export const getBoard = (params?: any) => request.get<any, any>('/tasks/board', { params })
export const getStatistics = () => request.get<any, any>('/tasks/statistics')
export const moveTask = (id: number, data: any) => request.post<any, any>(`/tasks/${id}/move`, data)
export const archiveTask = (id: number) => request.post<any, any>(`/tasks/${id}/archive`)
export const unarchiveTask = (id: number) => request.post<any, any>(`/tasks/${id}/unarchive`)
export const restoreTask = (id: number) => request.post<any, any>(`/tasks/${id}/restore`)
export const deleteTaskComment = (id: number, cid: number) =>
  request.delete<any, any>(`/tasks/${id}/comments/${cid}`)
export const uploadTaskAttachment = (file: File, taskId?: number) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post<any, { id: number; fileName: string; fileSize: number; fileExt: string }>(
    '/tasks/attachments/upload', fd, { params: taskId ? { taskId } : {} })
}
export const downloadTaskAttachment = (aid: number) =>
  request.get<any, Blob>(`/tasks/attachments/${aid}/download`, { responseType: 'blob' })
export const deleteTaskAttachment = (aid: number) =>
  request.delete<any, any>(`/tasks/attachments/${aid}`)

// 删除任务：管理员可删任意任务，其他人只能删未开工的任务
export function deleteTask(id: number) {
  return request.delete<any, void>(`/tasks/${id}`)
}
