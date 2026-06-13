<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const login = () => {
    router.push("/homepage")
}

</script>

<template>
    <div class="login-background d-flex justify-content-center align-items-center ">
        <div class="card shadow-lg p-4 login-card"
            style="width: 340px; border-radius: 12px; height:400px; color: white;">
            <div class="card-header">
                <div class="card-title d-flex justify-content-center">HỆ THỐNG WMS - Quản lý kho</div>
            </div>
            <div class="card-body">
                <form>
                    <div class="mb-3">
                        <label for="Username" class="form-label">Tên đăng nhập</label>
                        <input type="text" class="form-control" id="Username">
                    </div>
                    <div class="mb-3">
                        <label for="Password" class="form-label">Mật khẩu</label>
                        <input type="password" class="form-control" id="Password">
                    </div>
                </form>
                <RouterLink to="/homepage" class="btn aero-btn w-100 mt-3" @click="login">
                    Đăng nhập
                </RouterLink>
            </div>
        </div>
    </div>
</template>

<style scoped>
.login-background {
    min-height: 100vh;

    background:
        radial-gradient(circle at top left,
            rgba(255, 255, 255, 0.12),
            transparent 35%),
        radial-gradient(circle at bottom right,
            rgba(255, 255, 255, 0.08),
            transparent 40%),
        linear-gradient(135deg,
            #0f172a 0%,
            #1e3a8a 45%,
            #4c1d95 100%);
}

.login-card {
    width: 340px;
    height: 400px;

    border-radius: 16px;

    background: rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(18px);
    -webkit-backdrop-filter: blur(18px);

    border: 1px solid rgba(255, 255, 255, 0.35);

    box-shadow:
        0 8px 32px rgba(0, 0, 0, 0.25),
        inset 0 1px 1px rgba(255, 255, 255, 0.3);

    color: white;
}

.login-card {
    position: relative;
    overflow: hidden;
}

.login-card::before {
    content: "";

    position: absolute;
    top: 0;
    left: 0;

    width: 100%;
    height: 45%;

    background: linear-gradient(to bottom,
            rgba(255, 255, 255, 0.35),
            rgba(255, 255, 255, 0.08),
            transparent);

    pointer-events: none;
}

.form-control {
    background: rgba(255, 255, 255, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.35);
    color: white;
}

.form-control:focus {
    background: rgba(255, 255, 255, 0.25);
    color: white;

    border-color: #9ddcff;

    box-shadow:
        0 0 8px rgba(100, 180, 255, .5);
}

.form-control::placeholder {
    color: rgba(255, 255, 255, .7);
}

.aero-btn {
    background: linear-gradient(to bottom,
            #7ec8ff,
            #4fa3ff);

    border: 1px solid #8fd7ff;

    color: white;

    border-radius: 8px;

    transition: 0.2s;
}

.aero-btn:hover {
    background: linear-gradient(to bottom,
            #9ed9ff,
            #66b7ff);
}
</style>