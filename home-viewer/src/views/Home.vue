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
    </v-row>
    <div id="tooltip" role="tooltip" hidden>
      <div id="arrow" data-popper-arrow></div>
      {{ tooltipText }}
      <br>
      <iframe src="/settings">

      </iframe>
    </div>
  </v-container>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import { server } from "@/Api.ts";
import FloorSelector from "@/components/FloorSelector.vue";
import $ from "jquery";
import { getSVG } from "@/Api";
import { createPopper } from "@popperjs/core";
import axios from "axios";

@Component({ components: { FloorSelector } })
export default class Home extends Vue {
  private serverPath = server;

  private floors = this.$store.state.home.floors.map((f: { name: string }) => ({
    name: f.name,
    svg: ""
  }));

  private selectedFloorIndex = 0;
  private popper: any = null;
  private tooltipText = "test";

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

    setInterval(() => {
      axios.get(server + "/home/properties/time").then(response => {
        this.tooltipText = response.data;
      });
    }, 1000);
  }

  private onPathSelect(path: any) {
    console.log("Path clicked!");
    return;
  }

  private onPathEnter(path: any) {
    console.log("Path hover!");

    const name = $(path).attr("data-bindid");

    if (name == null) {
      return;
    }

    this.tooltipText = name;
    const svgEntity = $(path).get(0);
    const tooltip = $("#tooltip").get(0);
    this.popper = createPopper(svgEntity, tooltip, {});

    $("#tooltip").css("display", "block");
  }

  private onPathLeave(path: any) {
    console.log("Path hover!");
    $("#tooltip").css("display", "none");
    this.popper?.destroy();
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
#tooltip {
  background: #333;
  color: white;
  font-weight: bold;
  padding: 4px 8px;
  font-size: 13px;
  border-radius: 4px;
}

#arrow,
#arrow::before {
  position: absolute;
  width: 8px;
  height: 8px;
  z-index: -1;
}

#arrow::before {
  content: "";
  transform: rotate(45deg);
  background: #333;
}
#tooltip[data-popper-placement^="top"] > #arrow {
  bottom: -4px;
}

#tooltip[data-popper-placement^="bottom"] > #arrow {
  top: -4px;
}

#tooltip[data-popper-placement^="left"] > #arrow {
  right: -4px;
}

#tooltip[data-popper-placement^="right"] > #arrow {
  left: -4px;
}
</style>
