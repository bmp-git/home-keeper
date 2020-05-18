import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import store from "./store";
import { getHome } from "@/Api";
import vuetify from "./plugins/vuetify";

Vue.config.productionTip = false;

const updateIntervalms = 250;

const vue = new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
});



getHome(home => {
  store.commit("setHomeTopology", home);
  store.commit("updateHomeProperties", home);
  setInterval(() => {
    getHome(home => {
      store.commit("updateHomeProperties", home);
    })
  }, updateIntervalms);
  vue.$mount("#app")
}, () => {
  console.log("Error loading home topology.")
  vue.$mount("#app")
})
