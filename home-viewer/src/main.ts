import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import store from "./store";
import { getHome } from '@/Api'

Vue.config.productionTip = false;

const vue = new Vue({
  router,
  store,
  render: h => h(App)
})

getHome(home => {
    store.commit("updateHome", home)
})
.then(() => vue.$mount("#app"))



