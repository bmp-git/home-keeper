<template>
  <v-container fluid>
    <v-row align="start" justify="start">
      <v-col cols="9">
        <FloorSelector
          :selected-floor-index.sync="selectedFloorIndex"
          :floor-names="floors.map(f => f.name)"
        ></FloorSelector>
      </v-col>
      <v-col cols="3">
        <UsersList :users="usersNames"></UsersList>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="9">
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
      <v-col cols="3">
        <EntitiesViewer ref="entitiesViewer" :selected-floor-index="selectedFloorIndex"></EntitiesViewer>
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
import UsersList from "@/components/UsersList.vue";

@Component({ components: { FloorSelector, Tooltip, EntitiesViewer, UsersList } })
export default class Home extends Vue {
  private usersNames: string[] = this.$store.state.homeTopology.users.map((u: any) => u.name);

  private floors = this.$store.state.homeTopology.floors.map((f: { name: string, level: number }) => ({
    name: f.name,
    level: f.level,
    svg: ""
  }));

  private selectedFloorIndex = this.floors.findIndex((f: any) => f.level === 0);
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
    if (id == null) {
      return;
    }
    (this.$refs['entitiesViewer'] as any).addEntity(this.selectedFloorIndex, id, true);
  }

  private pinUserCard(userName: string) {
    (this.$refs['entitiesViewer'] as any).addUser(userName,true);
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
    $("svg").attr("height", "calc(100vh - 231px)");
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
