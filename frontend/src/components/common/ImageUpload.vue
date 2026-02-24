<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAvatar, uploadCover } from '@/api/upload'
import { Plus } from '@element-plus/icons-vue'

interface Props {
  modelValue?: string
  type?: 'avatar' | 'cover'
  width?: string
  height?: string
  borderRadius?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'avatar',
  width: '120px',
  height: '120px',
  borderRadius: '8px'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const uploading = ref(false)
const imageUrl = ref(props.modelValue)

// 监听 modelValue 变化
watch(() => props.modelValue, (newVal) => {
  imageUrl.value = newVal
})

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }

  const maxSize = props.type === 'avatar' ? 2 : 5
  const isLtSize = file.size / 1024 / 1024 < maxSize
  if (!isLtSize) {
    ElMessage.error(`图片大小不能超过 ${maxSize}MB!`)
    return false
  }

  return true
}

const handleUpload = async (file: File) => {
  if (!beforeUpload(file)) return

  uploading.value = true
  try {
    const uploadFn = props.type === 'avatar' ? uploadAvatar : uploadCover
    const res = await uploadFn(file)
    
    // 处理多种可能的响应格式
    let url = null
    if (res?.url) {
      url = res.url
    } else if (res?.data?.url) {
      url = res.data.url
    }
    
    if (url) {
      imageUrl.value = url
      emit('update:modelValue', url)
      ElMessage.success('上传成功')
    } else {
      ElMessage.error('上传失败：未返回图片地址')
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const handleChange = (file: any) => {
  if (file.raw) {
    handleUpload(file.raw)
  }
}
</script>

<template>
  <div class="image-upload">
    <el-upload
      class="upload-container"
      :show-file-list="false"
      :auto-upload="false"
      :on-change="handleChange"
      accept="image/*"
    >
      <div 
        v-loading="uploading"
        class="upload-area"
        :style="{
          width: props.width,
          height: props.height,
          borderRadius: props.borderRadius
        }"
      >
        <img v-if="imageUrl" :src="imageUrl" class="uploaded-image" />
        <div v-else class="upload-placeholder">
          <el-icon class="upload-icon"><Plus /></el-icon>
          <div class="upload-text">
            {{ props.type === 'avatar' ? '上传头像' : '上传封面' }}
          </div>
        </div>
      </div>
    </el-upload>
    
    <div class="upload-tips">
      {{ props.type === 'avatar' ? '推荐尺寸：200x200，最大2MB' : '推荐尺寸：1200x400，最大5MB' }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.image-upload {
  display: inline-block;
}

.upload-container {
  :deep(.el-upload) {
    display: block;
  }
}

.upload-area {
  border: 2px dashed var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
  
  &:hover {
    border-color: var(--el-color-primary);
  }
  
  .uploaded-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  
  .upload-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: var(--el-fill-color-light);
    
    .upload-icon {
      font-size: 28px;
      color: var(--text-color-muted);
      margin-bottom: 8px;
    }
    
    .upload-text {
      font-size: 14px;
      color: var(--text-color-muted);
    }
  }
}

.upload-tips {
  font-size: 12px;
  color: var(--text-color-muted);
  margin-top: 8px;
  text-align: center;
}
</style>
