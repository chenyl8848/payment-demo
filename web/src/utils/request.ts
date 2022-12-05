import axios, {AxiosRequestConfig, AxiosResponse} from "axios";
import {ElMessage} from "element-plus";

const service = axios.create({
    baseURL: 'http://localhost:5173',
    // 请求超时时间
    timeout: 10000
});

// request 拦截器
service.interceptors.request.use(
    (config: AxiosRequestConfig) => {
        return config;
    },
    (error: any) => {
        Promise.reject<any>(error)
    }
);

// response 拦截器
service.interceptors.response.use(
    async (response: AxiosResponse) => {
        const res = response.data;
        if (res.code < 0) {
            ElMessage({
                message: res.message,
                type: 'error',
                showClose: true,
                duration: 5 * 1000
            });

            return Promise.reject('error');
        } else {
            return response.data
        }
    },

    (error: any) => {
        return Promise.reject(error)
    }
);

export default service;