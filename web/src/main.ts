import {createApp} from 'vue';
// import './style.css';
import App from './App.vue';

import router from "./router";
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/lib/locale/lang/zh-cn';
import 'element-plus/dist/index.css';

const app = createApp(App);

app.use(router);
app.use(ElementPlus, {
    locale: zhCn,
});

app.mount('#app');


