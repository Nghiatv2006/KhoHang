<script setup>
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    BarElement
} from "chart.js"

import {Bar} from "vue-chartjs";

ChartJS.register(
    CategoryScale,
    LinearScale,
    BarElement
);

const chartData = {
    labels: ["Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng"],
    datasets: [{
        label: "Tồn kho",
        data: [5200, 4100, 2800]
    }]
};

const chartOptions = {
    responsive:true
};
</script>
<template>
    <Bar :data ="chartData" :options="chartOptions" />
</template>

<style scoped>
</style>