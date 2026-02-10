<template>
  <span ref="numberRef" class="number-roll">{{ displayValue }}</span>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import gsap from 'gsap'

const props = defineProps<{
  value: number
  duration?: number
  decimals?: number
}>()

const numberRef = ref<HTMLSpanElement>()
const displayValue = ref(0)

const animateNumber = (target: number) => {
  gsap.to(displayValue, {
    value: target,
    duration: props.duration || 2,
    ease: 'power1.out',
    onUpdate: () => {
      if (props.decimals !== undefined) {
        displayValue.value = Number(displayValue.value.toFixed(props.decimals))
      } else {
        displayValue.value = Math.floor(displayValue.value)
      }
    },
  })
}

watch(
  () => props.value,
  (newValue) => {
    animateNumber(newValue)
  }
)

onMounted(() => {
  animateNumber(props.value)
})
</script>

<style scoped>
.number-roll {
  font-variant-numeric: tabular-nums;
}
</style>

