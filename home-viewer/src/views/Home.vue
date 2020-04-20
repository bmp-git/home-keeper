<template>
  <v-row align="start" justify="start">
    <v-col cols="12" sm="4">
      <p>Floor</p>
      <v-overflow-btn :items="floors" v-model="selected"></v-overflow-btn>
      <p>{{ selected }}</p>
    </v-col>
  </v-row>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";

@Component
export default class Home extends Vue {
  private floors = this.$store.state.home.floors.map(
    (f: { name: string }) => f.name
  );

  private selected = this.floors[0];
}
</script>
