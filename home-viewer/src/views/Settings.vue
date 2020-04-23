<template>
  <v-container fluid>
    <v-row>
      <v-col lg="9" mb="6"> </v-col>
      <v-col lg="3" mb="6">
        <v-card class="mx-auto text-center justify-center py-2" raised outlined>
          <v-tabs v-model="selectedClass" center-active centered>
            <v-tab v-for="c in classes" :key="c.name">
              {{ c.name }}
            </v-tab>
          </v-tabs>
          <v-list dense>
            <v-list-item-group v-model="items" color="primary">
              <draggable v-model="items">
                <v-list-item v-for="item in items" :key="item.name">
                  <v-list-item-content>
                    <v-list-item-title v-text="item.name"></v-list-item-title>
                  </v-list-item-content>
                </v-list-item>
              </draggable>
            </v-list-item-group>
          </v-list>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import draggable from "vuedraggable";

@Component({ components: { draggable } })
export default class Settings extends Vue {
  private rooms = this.$store.state.home.floors.flatMap((f: any) => f.rooms);

  private distinctName(value: any, index: any, array: any): boolean {
    return array.findIndex((i: any) => i.name === value.name) === index;
  }

  private doors = this.$store.state.home.floors
    .flatMap((f: any) => f.rooms)
    .flatMap((r: any) => r.doors)
    .filter(this.distinctName);

  private windows = this.$store.state.home.floors
    .flatMap((f: any) => f.rooms)
    .flatMap((r: any) => r.windows)
    .filter(this.distinctName);

  private classes = [
    { name: "Rooms", values: this.rooms },
    { name: "Doors", values: this.doors },
    { name: "Windows", values: this.windows }
  ];

  private selectedClass = 0;
  private properties = this.rooms;

  get items() {
    return this.classes[this.selectedClass].values;
  }

  set items(values) {
    if (Array.isArray(values)) {
      this.classes[this.selectedClass].values = values;
    }
  }
}
</script>
