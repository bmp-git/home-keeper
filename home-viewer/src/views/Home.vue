<template>
  <v-container fluid>
    <v-row align="start" justify="start">
      <v-col cols="12" sm="4">
        <v-btn-toggle v-model="selectedFloorIndex" mandatory>
          <v-btn v-for="floor in floors" :key="floor.name">
            {{ floor }}
          </v-btn>
        </v-btn-toggle>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="8">
        <object
          v-for="(floor, index) in floors"
          :key="floor.name"
          type="image/svg+xml"
          :data="serverPath + '/home/floors/' + floor + '/properties/plan.svg'"
          :hidden="selectedFloorIndex !== index"
          width="100%"
        >
          <!-- fallback here (<img> referencing a PNG version of the graphic, for example) -->
        </object>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import { server } from "@/Api.ts";

@Component
export default class Home extends Vue {
  private serverPath = server;
  private floors = this.$store.state.home.floors.map(
    (f: { name: string }) => f.name
  );
  private selectedFloorIndex = 0;
}
</script>
