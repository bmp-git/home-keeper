import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import store from "./store";
import { getHome } from '@/Api'
 
Vue.config.productionTip = false;

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount("#app");

function updateHomedata(interval: number) {
  setInterval(() => {
    getHome(home => {
      store.commit("updateHome", home)
    })
  }, interval)
}

updateHomedata(1000);