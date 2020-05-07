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
      console.log("STORE: setHomeTopology");
      state.homeTopology = home;
    },
    updateHomeProperties(state, home) {
      console.log("STORE: updateHomeProperties");
      state.homeProperties = home;
    }
  },
  actions: {},
  modules: {}
});
