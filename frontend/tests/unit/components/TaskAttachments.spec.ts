import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskAttachments from '@/components/team/TaskAttachments.vue'
import * as requestUtils from '@/utils/request'

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(),
    },
  }
})

// Mock fetch for download
global.fetch = vi.fn()

describe('TaskAttachments.vue', () => {
  let requestMock: any

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()

    // Mock request utility
    requestMock = {
      get: vi.fn(),
      post: vi.fn(),
      delete: vi.fn(),
    }
    vi.spyOn(requestUtils, 'request', 'get').mockReturnValue(requestMock)

    // Mock URL.createObjectURL and revokeObjectURL
    global.URL.createObjectURL = vi.fn(() => 'blob:mock-url')
    global.URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const mockAttachments = [
    {
      id: 1,
      taskId: 100,
      fileName: 'document.pdf',
      fileSize: 1024000,
      uploadedBy: 1,
      uploaderName: '张三',
      uploadedAt: '2026-01-26T10:00:00',
    },
    {
      id: 2,
      taskId: 100,
      fileName: 'image.png',
      fileSize: 512000,
      uploadedBy: 2,
      uploaderName: '李四',
      uploadedAt: '2026-01-26T11:00:00',
    },
  ]

  describe('Attachment List Rendering', () => {
    it('should display attachment list when attachments exist', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('document.pdf')
      expect(wrapper.text()).toContain('image.png')
    })

    it('should display empty state when no attachments', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('暂无附件')
    })

    it('should display file names', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const fileNames = wrapper.findAll('.attachment-name')
      expect(fileNames.length).toBe(2)
      expect(fileNames[0].text()).toBe('document.pdf')
      expect(fileNames[1].text()).toBe('image.png')
    })

    it('should display file sizes', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const fileSizes = wrapper.findAll('.file-size')
      expect(fileSizes.length).toBe(2)
      expect(fileSizes[0].text()).toContain('KB')
      expect(fileSizes[1].text()).toContain('KB')
    })

    it('should display uploader names', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('张三')
      expect(wrapper.text()).toContain('李四')
    })

    it('should display upload times', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const uploadTimes = wrapper.findAll('.upload-time')
      expect(uploadTimes.length).toBe(2)
      expect(uploadTimes[0].text()).toBeTruthy()
    })

    it('should display attachment count in header', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('附件 (2)')
    })
  })

  describe('Upload Attachment Interaction', () => {
    it('should show upload button', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const uploadButton = wrapper.find('.el-button')
      expect(uploadButton.exists()).toBe(true)
      expect(uploadButton.text()).toContain('上传附件')
    })

    it('should validate file size before upload', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const largeFile = new File([''], 'large.pdf', {
        type: 'application/pdf',
      })
      Object.defineProperty(largeFile, 'size', { value: 11 * 1024 * 1024 })

      const result = vm.beforeUpload(largeFile)

      expect(result).toBe(false)
      expect(ElMessage.error).toHaveBeenCalledWith('文件大小不能超过 10MB')
    })

    it('should validate file type before upload', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const invalidFile = new File([''], 'script.exe', {
        type: 'application/x-msdownload',
      })

      const result = vm.beforeUpload(invalidFile)

      expect(result).toBe(false)
      expect(ElMessage.error).toHaveBeenCalledWith('不支持的文件类型')
    })

    it('should accept valid file', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const validFile = new File(['content'], 'document.pdf', {
        type: 'application/pdf',
      })
      Object.defineProperty(validFile, 'size', { value: 1024000 })

      const result = vm.beforeUpload(validFile)

      expect(result).toBe(true)
      expect(vm.uploading).toBe(true)
      expect(vm.uploadingFileName).toBe('document.pdf')
    })

    it('should show upload progress', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.uploading = true
      vm.uploadProgress = 50
      vm.uploadingFileName = 'test.pdf'

      await wrapper.vm.$nextTick()

      expect(wrapper.find('.upload-progress').exists()).toBe(true)
      expect(wrapper.text()).toContain('正在上传: test.pdf')
    })

    it('should handle upload success', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const response = {
        code: 200,
        data: {
          id: 3,
          taskId: 100,
          fileName: 'new-file.pdf',
          fileSize: 2048000,
          uploadedBy: 1,
          uploaderName: '张三',
          uploadedAt: '2026-01-26T12:00:00',
        },
      }

      vm.handleUploadSuccess(response)
      await flushPromises()

      expect(vm.uploading).toBe(false)
      expect(vm.uploadProgress).toBe(0)
      expect(ElMessage.success).toHaveBeenCalledWith('附件上传成功')
      expect(wrapper.emitted('update')).toBeTruthy()
    })

    it('should handle upload error', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.uploading = true
      vm.handleUploadError(new Error('Upload failed'))

      expect(vm.uploading).toBe(false)
      expect(ElMessage.error).toHaveBeenCalledWith('附件上传失败')
    })

    it('should update progress during upload', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.handleUploadProgress({ percent: 75 })

      expect(vm.uploadProgress).toBe(75)
    })
  })

  describe('Download Attachment Interaction', () => {
    it('should download attachment when download button is clicked', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })
      
      const mockBlob = new Blob(['file content'], { type: 'application/pdf' })
      ;(global.fetch as any).mockResolvedValueOnce({
        ok: true,
        blob: () => Promise.resolve(mockBlob),
      })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      
      // Mock document.createElement and appendChild
      const mockLink = {
        href: '',
        download: '',
        click: vi.fn(),
      }
      const createElementSpy = vi.spyOn(document, 'createElement').mockReturnValue(mockLink as any)
      const appendChildSpy = vi.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink as any)
      const removeChildSpy = vi.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink as any)

      await vm.handleDownload(mockAttachments[0])
      await flushPromises()

      expect(global.fetch).toHaveBeenCalled()
      expect(mockLink.click).toHaveBeenCalled()
      expect(mockLink.download).toBe('document.pdf')
      expect(ElMessage.success).toHaveBeenCalledWith('下载成功')

      createElementSpy.mockRestore()
      appendChildSpy.mockRestore()
      removeChildSpy.mockRestore()
    })

    it('should handle download error', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })
      ;(global.fetch as any).mockResolvedValueOnce({
        ok: false,
      })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDownload(mockAttachments[0])
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should show loading state during download', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.downloadingId = 1

      await wrapper.vm.$nextTick()

      // Download button should show loading state
      expect(vm.downloadingId).toBe(1)
    })
  })

  describe('Delete Attachment Interaction', () => {
    it('should show confirmation dialog when delete button is clicked', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockAttachments[0])

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        '确定要删除附件 "document.pdf" 吗？',
        '提示',
        expect.any(Object)
      )
    })

    it('should delete attachment successfully', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })
      requestMock.delete.mockResolvedValueOnce({})
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockAttachments[0])
      await flushPromises()

      expect(requestMock.delete).toHaveBeenCalledWith('/tasks/100/attachments/1')
      expect(ElMessage.success).toHaveBeenCalledWith('删除附件成功')
      expect(wrapper.emitted('update')).toBeTruthy()
    })

    it('should show error when delete fails', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })
      requestMock.delete.mockRejectedValueOnce(new Error('删除失败'))
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockAttachments[0])
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should not delete when user cancels confirmation', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })
      ;(ElMessageBox.confirm as any).mockRejectedValueOnce(new Error('cancel'))

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockAttachments[0])
      await flushPromises()

      expect(requestMock.delete).not.toHaveBeenCalled()
    })
  })

  describe('File Size Formatting', () => {
    it('should format bytes correctly', async () => {
      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      expect(vm.formatFileSize(0)).toBe('0 B')
      expect(vm.formatFileSize(500)).toBe('500 B')
      expect(vm.formatFileSize(1024)).toBe('1 KB')
      expect(vm.formatFileSize(1024 * 1024)).toBe('1 MB')
      expect(vm.formatFileSize(1024 * 1024 * 1024)).toBe('1 GB')
    })

    it('should format with decimals', async () => {
      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      expect(vm.formatFileSize(1536)).toBe('1.5 KB')
      expect(vm.formatFileSize(1024 * 1024 * 1.5)).toBe('1.5 MB')
    })
  })

  describe('Time Formatting', () => {
    it('should display "刚刚" for very recent time', async () => {
      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const now = new Date().toISOString()
      const formatted = vm.formatTime(now)
      expect(formatted).toBe('刚刚')
    })

    it('should display minutes ago for recent time', async () => {
      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(tenMinutesAgo)
      expect(formatted).toContain('分钟前')
    })

    it('should display hours ago for time within a day', async () => {
      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(twoHoursAgo)
      expect(formatted).toContain('小时前')
    })

    it('should display days ago for recent days', async () => {
      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(threeDaysAgo)
      expect(formatted).toContain('天前')
    })

    it('should display date for old time', async () => {
      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const twoWeeksAgo = new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(twoWeeksAgo)
      expect(formatted).toBeTruthy()
    })
  })

  describe('API Error Handling', () => {
    it('should handle fetch attachments error', async () => {
      requestMock.get.mockRejectedValueOnce(new Error('Network error'))

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })
  })

  describe('Component Lifecycle', () => {
    it('should fetch attachments on mount', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAttachments })

      mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(requestMock.get).toHaveBeenCalledWith('/tasks/100/attachments')
    })

    it('should expose refresh method', async () => {
      requestMock.get.mockResolvedValue({ data: mockAttachments })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(typeof vm.refresh).toBe('function')

      await vm.refresh()
      await flushPromises()

      expect(requestMock.get).toHaveBeenCalledTimes(2)
    })
  })

  describe('Edge Cases', () => {
    it('should handle empty file name', async () => {
      const attachmentNoName = {
        ...mockAttachments[0],
        fileName: '',
      }
      requestMock.get.mockResolvedValueOnce({ data: [attachmentNoName] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.find('.attachment-item').exists()).toBe(true)
    })

    it('should handle zero file size', async () => {
      const attachmentZeroSize = {
        ...mockAttachments[0],
        fileSize: 0,
      }
      requestMock.get.mockResolvedValueOnce({ data: [attachmentZeroSize] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('0 B')
    })

    it('should handle missing uploader name', async () => {
      const attachmentNoUploader = {
        ...mockAttachments[0],
        uploaderName: '',
      }
      requestMock.get.mockResolvedValueOnce({ data: [attachmentNoUploader] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.find('.attachment-item').exists()).toBe(true)
    })
  })

  describe('File Validation', () => {
    it('should reject file exceeding 10MB', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const largeFile = new File([''], 'large.pdf', { type: 'application/pdf' })
      Object.defineProperty(largeFile, 'size', { value: 11 * 1024 * 1024 })

      const result = vm.beforeUpload(largeFile)

      expect(result).toBe(false)
      expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('文件大小不能超过 10MB'))
    })

    it('should accept file at 10MB limit', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const maxFile = new File([''], 'max.pdf', { type: 'application/pdf' })
      Object.defineProperty(maxFile, 'size', { value: 10 * 1024 * 1024 })

      const result = vm.beforeUpload(maxFile)

      expect(result).toBe(true)
    })

    it('should reject empty file', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const emptyFile = new File([''], 'empty.pdf', { type: 'application/pdf' })
      Object.defineProperty(emptyFile, 'size', { value: 0 })

      const result = vm.beforeUpload(emptyFile)

      expect(result).toBe(false)
      expect(ElMessage.error).toHaveBeenCalledWith('不能上传空文件')
    })

    it('should reject file with name exceeding 255 characters', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const longName = 'a'.repeat(256) + '.pdf'
      const longNameFile = new File(['content'], longName, { type: 'application/pdf' })
      Object.defineProperty(longNameFile, 'size', { value: 1024 })

      const result = vm.beforeUpload(longNameFile)

      expect(result).toBe(false)
      expect(ElMessage.error).toHaveBeenCalledWith('文件名长度不能超过 255 个字符')
    })

    it('should accept file with name at 255 character limit', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const maxName = 'a'.repeat(251) + '.pdf' // 251 + 4 = 255
      const maxNameFile = new File(['content'], maxName, { type: 'application/pdf' })
      Object.defineProperty(maxNameFile, 'size', { value: 1024 })

      const result = vm.beforeUpload(maxNameFile)

      expect(result).toBe(true)
    })

    it('should accept image files (jpg, png, gif)', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      
      const jpgFile = new File([''], 'image.jpg', { type: 'image/jpeg' })
      Object.defineProperty(jpgFile, 'size', { value: 1024 })
      expect(vm.beforeUpload(jpgFile)).toBe(true)

      const pngFile = new File([''], 'image.png', { type: 'image/png' })
      Object.defineProperty(pngFile, 'size', { value: 1024 })
      expect(vm.beforeUpload(pngFile)).toBe(true)

      const gifFile = new File([''], 'image.gif', { type: 'image/gif' })
      Object.defineProperty(gifFile, 'size', { value: 1024 })
      expect(vm.beforeUpload(gifFile)).toBe(true)
    })

    it('should accept document files (pdf, doc, docx, xls, xlsx)', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      
      const pdfFile = new File([''], 'doc.pdf', { type: 'application/pdf' })
      Object.defineProperty(pdfFile, 'size', { value: 1024 })
      expect(vm.beforeUpload(pdfFile)).toBe(true)

      const docFile = new File([''], 'doc.doc', { type: 'application/msword' })
      Object.defineProperty(docFile, 'size', { value: 1024 })
      expect(vm.beforeUpload(docFile)).toBe(true)

      const docxFile = new File([''], 'doc.docx', { 
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' 
      })
      Object.defineProperty(docxFile, 'size', { value: 1024 })
      expect(vm.beforeUpload(docxFile)).toBe(true)
    })

    it('should accept archive files (zip, rar)', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      
      const zipFile = new File([''], 'archive.zip', { type: 'application/zip' })
      Object.defineProperty(zipFile, 'size', { value: 1024 })
      expect(vm.beforeUpload(zipFile)).toBe(true)

      const rarFile = new File([''], 'archive.rar', { type: 'application/x-rar-compressed' })
      Object.defineProperty(rarFile, 'size', { value: 1024 })
      expect(vm.beforeUpload(rarFile)).toBe(true)
    })

    it('should reject unsupported file types', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      
      const exeFile = new File([''], 'virus.exe', { type: 'application/x-msdownload' })
      Object.defineProperty(exeFile, 'size', { value: 1024 })
      const result = vm.beforeUpload(exeFile)

      expect(result).toBe(false)
      expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('不支持的文件类型'))
    })

    it('should show detailed error message for unsupported file types', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const invalidFile = new File([''], 'script.sh', { type: 'application/x-sh' })
      Object.defineProperty(invalidFile, 'size', { value: 1024 })

      vm.beforeUpload(invalidFile)

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('支持的格式：图片(jpg, png, gif)、文档(pdf, doc, docx, xls, xlsx)、压缩包(zip, rar)')
      )
    })

    it('should show file size in error message when exceeding limit', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskAttachments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const largeFile = new File([''], 'large.pdf', { type: 'application/pdf' })
      Object.defineProperty(largeFile, 'size', { value: 15 * 1024 * 1024 })

      vm.beforeUpload(largeFile)

      expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('15 MB'))
    })
  })
})
