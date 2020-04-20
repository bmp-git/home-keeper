import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    home: {}
  },
  mutations: {
    updateHome(state, home) {
      console.log("STORE: updateHome");
      state.home = home;
    }
  },
  actions: {},
  modules: {}
});
