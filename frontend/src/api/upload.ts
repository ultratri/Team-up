import { request } from '@utils/request'

// 上传头像
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传封面
export function uploadCover(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload/cover', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传附件
export function uploadAttachment(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload/attachment', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传消息图片
export function uploadMessageImage(file: File, onProgress?: (percentage: number) => void) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<{ url: string; fileName: string; fileSize: number }>('/files/upload/message-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (event) => {
      if (!onProgress || !event.total) return
      const percentage = Math.round((event.loaded / event.total) * 100)
      onProgress(percentage)
    }
  })
}

// 上传消息文件
export function uploadMessageFile(file: File, onProgress?: (percentage: number) => void) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<{ url: string; fileName: string; fileSize: number }>('/files/upload/message-file', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (event) => {
      if (!onProgress || !event.total) return
      const percentage = Math.round((event.loaded / event.total) * 100)
      onProgress(percentage)
    }
  })
}

// 删除文件
export function deleteFile(url: string) {
  return request.delete('/files', { params: { url } })
}
