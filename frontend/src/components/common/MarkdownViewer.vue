<template>
  <div class="md-view" v-html="sanitizedHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = withDefaults(
  defineProps<{
    content?: string
  }>(),
  {
    content: ''
  }
)

const sanitizedHtml = computed(() => {
  const raw = String(props.content || '')
  if (!raw.trim()) return ''

  const html = marked.parse(raw, {
    gfm: true,
    breaks: true
  }) as string

  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true }
  })
})
</script>

<style scoped>
.md-view {
  color: var(--text-color-muted);
  line-height: 1.7;
  word-break: break-word;
}

.md-view :deep(p) {
  margin: 0 0 8px;
}

.md-view :deep(ul),
.md-view :deep(ol) {
  padding-left: 18px;
  margin: 6px 0 8px;
}

.md-view :deep(li) {
  margin: 2px 0;
}

.md-view :deep(strong) {
  color: var(--text-color);
}

.md-view :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.06);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 0.95em;
}

[data-theme='dark'] .md-view :deep(code) {
  background: rgba(255, 255, 255, 0.08);
}

.md-view :deep(pre) {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.06);
  overflow: auto;
}

[data-theme='dark'] .md-view :deep(pre) {
  background: rgba(255, 255, 255, 0.08);
}

.md-view :deep(pre code) {
  padding: 0;
  background: transparent;
}
</style>
