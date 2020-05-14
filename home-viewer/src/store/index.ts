import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    homeTopology: {},
    homeProperties: {}
  },
  mutations: {
    setHomeTopology(state, home) {
      state.homeTopology = home;
    },
    updateHomeProperties(state, home) {
      state.homeProperties = home;
    }
  },
  actions: {},
  modules: {}
});
