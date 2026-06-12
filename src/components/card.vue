# Card Component - for displaying storage items in a card format, or sorting into card format.
<script setup>
defineProps({
    title: String,
    value: [String, Number],
    icon: String
})
</script>

<template>
<div class="card shadow-sm h-100" style="background-color: lightyellow;">
    <div class="card-body">
        <h6 class="text-muted"> {{ title }} </h6>
        <div class="d-flex justify-content-between align-item-center">
                <i v-if="icon" :class="icon" class="fs-3"></i>
                <h2 class="fw-bold"> {{ value }}</h2>
        </div>
    </div>
</div>
</template>

<style scoped>
    .card {
        border-radius: 12px;
        height: 100%;
    }
</style>
