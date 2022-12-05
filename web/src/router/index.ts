import {createRouter, createWebHistory, RouteRecordRaw} from "vue-router";


const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        component: () => import('@/pages/Dashboard.vue')
    },
    {
        path: '/orderList',
        component: () => import("@/pages/OrderList.vue")
    },
    {
        path: '/download',
        component: () => import("@/pages/Download.vue")
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router;