<template>
  <div v-if="editor" class="rich-text-editor">
    <div v-if="showToolbar" class="editor-toolbar">
      <div class="toolbar-group">
        <el-button-group>
          <el-button
            :type="editor.isActive('heading', { level: 1 }) ? 'primary' : ''"
            @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
            title="标题1"
          >
            H1
          </el-button>
          <el-button
            :type="editor.isActive('heading', { level: 2 }) ? 'primary' : ''"
            @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
            title="标题2"
          >
            H2
          </el-button>
          <el-button
            :type="editor.isActive('heading', { level: 3 }) ? 'primary' : ''"
            @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
            title="标题3"
          >
            H3
          </el-button>
        </el-button-group>
      </div>

      <div class="toolbar-group">
        <el-button-group>
          <el-button
            :type="editor.isActive('bold') ? 'primary' : ''"
            @click="editor.chain().focus().toggleBold().run()"
            title="粗体"
          >
            <strong>B</strong>
          </el-button>
          <el-button
            :type="editor.isActive('italic') ? 'primary' : ''"
            @click="editor.chain().focus().toggleItalic().run()"
            title="斜体"
          >
            <em>I</em>
          </el-button>
          <el-button
            :type="editor.isActive('underline') ? 'primary' : ''"
            @click="editor.chain().focus().toggleUnderline().run()"
            title="下划线"
          >
            <u>U</u>
          </el-button>
          <el-button
            :type="editor.isActive('strike') ? 'primary' : ''"
            @click="editor.chain().focus().toggleStrike().run()"
            title="删除线"
          >
            <s>S</s>
          </el-button>
        </el-button-group>
      </div>

      <div class="toolbar-group">
        <el-button-group>
          <el-button
            :type="editor.isActive('bulletList') ? 'primary' : ''"
            @click="editor.chain().focus().toggleBulletList().run()"
            title="无序列表"
          >
            •
          </el-button>
          <el-button
            :type="editor.isActive('orderedList') ? 'primary' : ''"
            @click="editor.chain().focus().toggleOrderedList().run()"
            title="有序列表"
          >
            1.
          </el-button>
          <el-button
            :type="editor.isActive('codeBlock') ? 'primary' : ''"
            @click="editor.chain().focus().toggleCodeBlock().run()"
            title="代码块"
          >
            &lt;/&gt;
          </el-button>
        </el-button-group>
      </div>

      <div class="toolbar-group">
        <el-button-group>
          <el-button
            @click="setLink"
            :type="editor.isActive('link') ? 'primary' : ''"
            title="插入链接"
          >
            链接
          </el-button>
          <el-button
            @click="addImage"
            title="插入图片"
          >
            图片
          </el-button>
          <el-button
            @click="editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()"
            title="插入表格"
          >
            表格
          </el-button>
        </el-button-group>
      </div>

      <!-- 表格编辑工具 -->
      <div class="toolbar-group" v-if="editor.isActive('table')">
        <el-button-group>
          <el-button
            @click="editor.chain().focus().addColumnBefore().run()"
            size="small"
            title="在左侧插入列"
          >
            ←列
          </el-button>
          <el-button
            @click="editor.chain().focus().addColumnAfter().run()"
            size="small"
            title="在右侧插入列"
          >
            列→
          </el-button>
          <el-button
            @click="editor.chain().focus().deleteColumn().run()"
            size="small"
            title="删除列"
          >
            删列
          </el-button>
        </el-button-group>
        <el-button-group>
          <el-button
            @click="editor.chain().focus().addRowBefore().run()"
            size="small"
            title="在上方插入行"
          >
            ↑行
          </el-button>
          <el-button
            @click="editor.chain().focus().addRowAfter().run()"
            size="small"
            title="在下方插入行"
          >
            行↓
          </el-button>
          <el-button
            @click="editor.chain().focus().deleteRow().run()"
            size="small"
            title="删除行"
          >
            删行
          </el-button>
        </el-button-group>
        <el-button
          @click="editor.chain().focus().deleteTable().run()"
          size="small"
          type="danger"
          title="删除表格"
        >
          删除表格
        </el-button>
      </div>

      <div class="toolbar-group">
        <el-button
          @click="editor.chain().focus().undo().run()"
          :disabled="!editor.can().undo()"
          title="撤销"
        >
          撤销
        </el-button>
        <el-button
          @click="editor.chain().focus().redo().run()"
          :disabled="!editor.can().redo()"
          title="重做"
        >
          重做
        </el-button>
      </div>
    </div>

    <div class="editor-content">
      <editor-content :editor="editor" />
    </div>

    <div v-if="showCharCount" class="editor-footer">
      <span class="char-count" :class="{ 'char-limit': editor.storage.characterCount.characters() >= props.maxLength }">
        {{ editor.storage.characterCount.characters() }} / {{ props.maxLength }} 字
      </span>
    </div>
  </div>
  <div v-else class="rich-text-editor is-loading">
    <el-skeleton :rows="3" animated />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import CharacterCount from '@tiptap/extension-character-count'
import { createLowlight } from 'lowlight'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadMessageImage } from '@/api/upload'

interface Props {
  modelValue: string
  placeholder?: string
  showToolbar?: boolean
  showCharCount?: boolean
  maxLength?: number
  minHeight?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入内容...',
  showToolbar: true,
  showCharCount: true,
  maxLength: 5000,
  minHeight: '200px'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [1, 2, 3]
      },
      // 禁用 StarterKit 中的 codeBlock，使用自定义的 CodeBlockLowlight
      codeBlock: false
    }),
    Image.configure({
      inline: true,
      allowBase64: true
    }),
    Table.configure({
      resizable: true
    }),
    TableRow,
    TableHeader,
    TableCell,
    CodeBlockLowlight.configure({
      lowlight: createLowlight()
    }),
    CharacterCount.configure({
      limit: props.maxLength
    })
  ],
  editorProps: {
    attributes: {
      class: 'tiptap-editor',
      style: `min-height: ${props.minHeight}`
    }
  },
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    emit('update:modelValue', html)
  }
})

watch(() => props.modelValue, (newValue) => {
  if (editor.value && editor.value.getHTML() !== newValue) {
    editor.value.commands.setContent(newValue)
  }
})

const setLink = () => {
  ElMessage.warning('链接功能暂时禁用')
  return
  
  // const previousUrl = editor.value?.getAttributes('link').href
  // const url = window.prompt('请输入链接地址', previousUrl)

  // if (url === null) {
  //   return
  // }

  // if (url === '') {
  //   editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
  //   return
  // }

  // editor.value?.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

const addImage = async () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return

    // 检查文件大小（5MB）
    const maxSize = 5 * 1024 * 1024
    if (file.size > maxSize) {
      ElMessage.error('图片大小不能超过 5MB')
      return
    }

    const loadingMessage = ElMessage.info('图片上传中...')

    try {
      const res: any = await uploadMessageImage(file)
      const imageUrl = res?.data?.url || res?.url || res?.data?.data?.url
      
      if (imageUrl) {
        editor.value?.chain().focus().setImage({ src: imageUrl }).run()
        loadingMessage.close()
        ElMessage.success('图片上传成功')
      } else {
        loadingMessage.close()
        ElMessage.error('图片上传失败：未获取到图片地址')
      }
    } catch (error: any) {
      loadingMessage.close()
      ElMessage.error(error?.response?.data?.message || '图片上传失败')
    }
  }
  input.click()
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped lang="scss">
.rich-text-editor {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
  background: var(--el-bg-color);

  .editor-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    background: var(--el-bg-color-overlay);

    .toolbar-group {
      display: flex;
      gap: 4px;
    }
  }

  .editor-content {
    padding: 12px;
    background: var(--el-bg-color);

    :deep(.tiptap-editor) {
      outline: none;
      min-height: v-bind(minHeight);
      color: var(--el-text-color-primary);
      background: transparent;

      p {
        margin: 0.5em 0;
      }

      h1, h2, h3 {
        margin: 1em 0 0.5em;
        font-weight: 600;
      }

      h1 {
        font-size: 2em;
      }

      h2 {
        font-size: 1.5em;
      }

      h3 {
        font-size: 1.25em;
      }

      ul, ol {
        padding-left: 1.5em;
        margin: 0.5em 0;
      }

      code {
        background: var(--el-fill-color-lighter);
        padding: 2px 4px;
        border-radius: 3px;
        font-family: 'Courier New', monospace;
      }

      pre {
        background: var(--el-fill-color-lighter);
        padding: 12px;
        border-radius: 4px;
        overflow-x: auto;
        margin: 0.5em 0;
      }

      img {
        max-width: 100%;
        height: auto;
        border-radius: 4px;
      }

      table {
        border-collapse: collapse;
        margin: 0.5em 0;
        width: 100%;

        td, th {
          border: 1px solid var(--el-border-color);
          padding: 8px;
        }

        th {
          background: var(--el-fill-color-lighter);
          font-weight: 600;
        }
      }

      a {
        color: var(--el-color-primary);
        text-decoration: underline;
      }
    }
  }

  .editor-footer {
    padding: 8px 12px;
    border-top: 1px solid var(--el-border-color-lighter);
    background: var(--el-bg-color-overlay);
    text-align: right;

    .char-count {
      font-size: 12px;
      color: var(--el-text-color-secondary);

      &.char-limit {
        color: var(--el-color-danger);
        font-weight: 500;
      }
    }
  }
}
</style>
