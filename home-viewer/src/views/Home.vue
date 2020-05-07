<template>
  <v-container fluid>
    <v-row align="start" justify="start">
      <v-col mb="10">
        <FloorSelector
          :selected-floor-index.sync="selectedFloorIndex"
          :floor-names="floors.map(f => f.name)"
        ></FloorSelector>
      </v-col>
    </v-row>
    <v-row>
      <v-col lg="9" mb="6">
        <div
          v-for="(floor, index) in floors"
          :key="floor.name"
          :ref="'obj_' + index"
          :id="'obj_' + index"
          class="clickable"
          v-html="floor.svg"
          :hidden="selectedFloorIndex !== index"
        ></div>
      </v-col>
      <v-col lg="3" mb="6">
        <EntitiesViewer :entities="pinnedEntities" :selected-floor-index="selectedFloorIndex"></EntitiesViewer>
      </v-col>
    </v-row>
    <Tooltip ref="tooltip"></Tooltip>
  </v-container>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import { server } from "@/Api.ts";
import FloorSelector from "@/components/FloorSelector.vue";
import Tooltip from "@/components/Tooltip.vue";
import EntitiesViewer from "@/components/EntitiesViewer.vue";
import $ from "jquery";
import { getSVG } from "@/Api";

@Component({ components: { FloorSelector, Tooltip, EntitiesViewer } })
export default class Home extends Vue {
  private serverPath = server;
  private pinnedEntities: { floor: number; entityId: string }[] = [];

  private floors = this.$store.state.homeTopology.floors.map((f: { name: string }) => ({
    name: f.name,
    svg: ""
  }));

  private selectedFloorIndex = 0;
  private tooltip: any = null;

  mounted() {
    $(".clickable").on("click", "path", event =>
      this.onPathSelect(event.currentTarget)
    );

    $(".clickable").on("mouseenter", "path", event =>
      this.onPathEnter(event.currentTarget)
    );

    $(".clickable").on("mouseleave", "path", event =>
      this.onPathLeave(event.currentTarget)
    );

    this.updateFloorsSvg();
    this.tooltip = this.$refs["tooltip"] as any;
  }

  private onPathSelect(path: any) {
    console.log("Path clicked!");
    const id = $(path).attr("data-bindid");
    if (id == null || this.pinnedEntities.find(e => e.entityId === id)) {
      return;
    }

    this.pinnedEntities.push({floor : this.selectedFloorIndex, entityId: id});
  }

  private onPathEnter(path: any) {
    console.log("Path hover!");
    const id = $(path).attr("data-bindid");
    if (id == null) {
      return;
    }

    this.tooltip?.createTooltip(path, this.selectedFloorIndex, id);
  }

  private onPathLeave(path: any) {
    console.log("Path hover!");
    this.tooltip?.deleteTooltip();
  }

  private updateFloorsSvg() {
    const promises = [];
    for (let i = 0; i < this.floors.length; i++) {
      const index = i;
      const promise = getSVG(this.floors[index].name, svg => {
        this.floors[index].svg = svg;
      });
      promises.push(promise);
    }

    Promise.all(promises).then(this.onSvgLoad);
  }

  private onSvgLoad() {
    $("svg").attr("height", "100%");
    $("svg").attr("width", "100%");
    $("svg")
      .find("*")
      .css("pointer-events", "none");
    $("svg")
      .find("path")
      .css("pointer-events", "all");
    $("svg")
      .find("title")
      .remove();
  }
}
</script>

<style>

</style>
