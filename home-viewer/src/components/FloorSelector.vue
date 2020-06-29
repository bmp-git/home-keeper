<template>
  <v-btn-toggle v-model="mySelectedFloorIndex" mandatory>
    <v-btn v-for="floor in floorNames" :key="floor">
      {{ floor }}
    </v-btn>
  </v-btn-toggle>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
@Component
export default class FloorSelector extends Vue {
  @Prop() private selectedFloorIndex: number;
  @Prop() private floorNames: string[];

  private mySelectedFloorIndex = this.selectedFloorIndex;

  @Watch("mySelectedFloorIndex")
  private onSelectedFloorIndexChange() {
    this.$emit("update:selectedFloorIndex", this.mySelectedFloorIndex);
  }
}
</script>