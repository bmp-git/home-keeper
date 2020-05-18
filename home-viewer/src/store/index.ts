import Vue from "vue";
import Vuex from "vuex";
import VuexPersist from "vuex-persist";

const vuexPersist = new VuexPersist({
  key: 'home-viewer',
  storage: window.localStorage
})

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    homeTopology: {},
    homeProperties: {},
    serverAddress: "http://127.0.0.1:8090/api",
    apiKey: ""
  },
  mutations: {
    setHomeTopology(state, home) {
      state.homeTopology = home;
    },
    updateHomeProperties(state, home) {
      state.homeProperties = home;
    },
    updateServerAddress(state, address) {
      state.serverAddress = address;
    },
    updateApiKey(state, key) {
      state.apiKey = key;
    }
  },
  actions: {},
  modules: {},
  plugins: [vuexPersist.plugin]
});
