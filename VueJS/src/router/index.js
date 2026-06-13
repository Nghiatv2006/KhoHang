import { createRouter, createWebHistory } from "vue-router";

import Login from "../pages/login.vue";
import Homepage from "../pages/homepage.vue";

const routes = [
    {
        path: "/",
        redirect: "/login"
    },
    {
        path: "/login",
        name: "Login",
        component: Login
    },
    {
        path: "/homepage",
        name: "Homepage",
        component: Homepage
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;