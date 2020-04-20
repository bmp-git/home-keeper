import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import store from "./store";
import { getHome } from "@/Api";
import vuetify from "./plugins/vuetify";

Vue.config.productionTip = false;

const vue = new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
});

getHome(home => {
  store.commit("updateHome", home);
}).then(() => vue.$mount("#app"));
