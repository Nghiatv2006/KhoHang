<script setup>
import { ref } from "vue";
import Card from "../components/card.vue";
import Chart from "../components/chart.vue";
import { useRouter } from "vue-router";
const kpi = [
    {
        title: "Tổng sản phẩm nhập",
        value: 1524,
        icon: "bi bi-box-arrow-in-down"
    },
    {
        title: "Tổng sản phẩm xuất",
        value: 1984,
        icon: "bi bi-box-arrow-in-up"
    },
    {
        title: "Tổng tồn kho",
        value: 10472,
        icon: "bi bi-box-seam"
    },
    {
        title: "Đơn hôm nay",
        value: 34,
        icon: "bi bi-file-earmark-text"
    }
]

const menuItem = ref([
    {
        name: "Chi nhánh",
        icon: "bi bi-buildings",
        route: "#"
    },
    {
        name: "Sản phẩm",
        icon: "bi bi-box-seam",
        route: "#"
    },
    {
        name: "Nhân viên",
        icon: "bi bi-person",
        route: "#"
    },
    {
        name: "Kho hàng",
        icon: "fa-solid fa-warehouse",
        route: "#"
    },
    {
        name: "Vận đơn",
        icon: "bi bi-truck",
        route: "#"
    },
    {
        name: "Vai trò",
        icon: "bi bi-person-gear",
        route: "#"
    }
])

const collapse = ref(false);

const toggleSidebar = () => {
    collapse.value = !collapse.value;
}

const router = useRouter();

const logout = () => {
    router.push("/login");
};
</script>

<template>
    <div class="d-flex vh-100">
        <div class="sidebar" :class="{ collapsed: collapse }"
            style="background: linear-gradient(180deg,#0353c5 0%,#3657d4 45%,#6b5ce7 100%);">

            <!-- Sidebar Header -->
            <div class="sidebar-header">
                <div class="logo-section">
                    <span class="sidebar-text" v-show="!collapse">
                        <button class="btn btn-info btn-rounded border-dark" disabled>
                            <i class="bi bi-box-seam-fill" style="color: white;"></i>
                        </button>
                    </span>



                    <div class="logo-text" v-show="!collapse" style="color: bisque;">
                        <div class="fw-bold">Quản lý kho</div>
                        <small class="text-white-50">HỆ THỐNG WMS</small>
                    </div>
                </div>
                <div class="d-flex justify-content-end p-2">
                    <button class="btn btn-sm btn-outline-info toggle-btn" @click="toggleSidebar">
                        <i class="bi bi-layout-sidebar"></i>
                    </button>
                </div>
            </div>

            <!-- Sidebar Body -->

            <div class="flex-grow-1">
                <div v-for="item in menuItem" :key="item.route" class="sidebar-item">
                    <i :class="item.icon"></i>

                    <span class="sidebar-text" v-show="!collapse">
                        {{ item.name }}
                    </span>
                </div>
            </div>

            <!-- Sidebar Footer -->

            <div class="mt-auto">
                <div class="sidebar-item">
                    <i class="bi bi-gear"></i>
                    <span class="sidebar-text">Cài đặt</span>
                </div>

                <div class="sidebar-item text-danger"  @click="logout">
                    <i class="bi bi-box-arrow-right"></i>
                    <span class="sidebar-text">Đăng xuất</span>
                </div>
            </div>
        </div>
        <div class="flex-grow-1">
            <nav class="navbar navbar-expand-lg bg-body-tertiary">
                <div class="container-fluid">
                    <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                        data-bs-target="#navbarSupportedContent" aria-controls="navbarSupportedContent"
                        aria-expanded="false" aria-label="Toggle navigation">
                        <span class="navbar-toggler-icon"></span>
                    </button>
                    <div class="collapse navbar-collapse" id="navbarSupportedContent">
                        <ul class="navbar-nav ms-auto mb-2 mb-lg-0">
                            <li class="nav-item d-flex align-items-center gap-3">
                                <div class="rounded-circle bg-info text-dark d-flex justify-content-center align-items-center"
                                    style="width: 36px; height: 36px;">A</div>
                                <span>
                                    Admin
                                </span>
                            </li>
                        </ul>
                    </div>
                </div>
            </nav>

            <div class="container mt-5">
                <div class="row g-3">
                    <div class="col-md-3 " v-for="item in kpi" :key="item.title">
                        <Card :title="item.title" :value="item.value" :icon="item.icon" />
                    </div>
                </div>
            </div>
            <div class="container mt-3">
                <div class="row">
                    <div class="col-md-7">
                        <div class="card shadow-sm h-100">
                            <div class="card-header fw-bold">
                                Tồn kho theo chi nhánh
                            </div>
                            <div class="card-body">
                                <Chart />
                            </div>
                        </div>
                    </div>
                    <div class="col-md-5">
                        <div class="card shadow-sm h-100">
                            <div class="card-header fw-bold">
                                Sản phẩm đang có nhu cầu
                            </div>
                            <div class="card-body">
                                <ul class="list-group list-group-flush">

                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Mì ăn liền Hảo Hảo chua cay</span>
                                        <span class="badge bg-primary">150</span>
                                    </li>

                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Bia 333 lon 330ml</span>
                                        <span class="badge bg-primary">70</span>
                                    </li>

                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Laptop MacBook Air M3 2024</span>
                                        <span class="badge bg-primary">35</span>
                                    </li>

                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Điện thoại iPhone 15 Pro Max</span>
                                        <span class="badge bg-primary">30</span>
                                    </li>

                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Tủ lạnh Samsung Inverter 380L</span>
                                        <span class="badge bg-primary">10</span>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

</template>

<style scoped>
.sidebar a {
    color: rgba(255, 255, 255, 0.85);
    transition: all 0.25s ease;
}

.sidebar a:hover {
    color: #6ee7ff;
    transform: translateX(4px);
}

.sidebar i {
    transition: color 0.25s ease;
}

.sidebar a:hover i {
    color: #6ee7ff;
}

.sidebar {
    width: 250px;
    transition: width 0.3s ease;
    overflow: hidden;
    overflow-x: hidden;
    flex-shrink: 0;
    border-right: 1px solid #dee2e6;

    display: flex;
    flex-direction: column;
    height: 100vh;
}

.sidebar.collapsed {
    width: 70px;
}

.sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    padding: 16px;
    margin-bottom: 20px;
}

.logo-section {
    display: flex;
    align-items: center;
    gap: 10px;
}

.sidebar-item {
    display: flex;
    align-items: center;

    gap: 12px;

    padding: 12px 16px;
    margin: 4px 8px;

    border-radius: 10px;

    cursor: pointer;

    transition: background-color .2s;
    color: rgba(255,255,255,0.9);
    transition: all .25s ease;
}

.sidebar-item:hover {
    background: #eef3fb;
    color: #3c3ff8;
}

.sidebar-item i {
    font-size: 20px;
    min-width: 24px;
    text-align: center;
}

.logo-text,
.sidebar-text {
    white-space: nowrap;
    opacity: 1;
    transition: opacity .2s ease;
}

.sidebar.collapsed .logo-text,
.sidebar.collapsed .sidebar-text {
    opacity: 0;
    width: 0;
    overflow: hidden;
}
</style>