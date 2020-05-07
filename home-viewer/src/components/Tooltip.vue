<template>
  <div id="tooltip" role="tooltip" hidden>
    <div id="arrow" data-popper-arrow></div>
    <p class="font-weight-black">{{ currentId }}</p>
    <ul v-for="(prop, index) in tooltipContent" :key="index">
      <li>{{ prop }}</li>
    </ul>
    <br />
    <iframe src="/settings"> </iframe>
  </div>
</template>

<script lang="ts">
import $ from "jquery";
import { Component, Vue, Watch } from "vue-property-decorator";
import { createPopper } from "@popperjs/core";

@Component
export default class Tooltip extends Vue {
  private popper: any = null;
  private tooltipContent: [] = null;

  private currentFloor: number = null;
  private currentId: string = null;

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateTooltipContent();
  }

  private updateTooltipContent() {
    if (!(this.currentFloor == null) && !(this.currentId == null)) {
      const properties = this.$store.state.homeProperties.floors[
        this.currentFloor
      ].rooms
        .flatMap((r: any) => {
          const room = [{ name: r.name, properties: r.properties }];
          return room.concat(r.doors, r.windows);
        })
        .filter((e: any) => e.name == this.currentId)[0]?.properties;
      console.log(properties, this.currentFloor, this.currentId);

      this.tooltipContent = properties;
    }
  }

  public createTooltip(bindto: any, floor: number, id: string) {
    console.log("tooltip created!", floor, id);

    this.currentFloor = floor;
    this.currentId = id;

    this.updateTooltipContent();

    const svgEntity = $(bindto).get(0);
    const tooltip = $("#tooltip").get(0);
    this.popper = createPopper(svgEntity, tooltip, {});
    $("#tooltip").css("display", "block");
  }

  public deleteTooltip() {
    $("#tooltip").css("display", "none");
    this.popper?.destroy();
    this.currentFloor = this.currentId = null;
    this.tooltipContent = [];
  }
}
</script>

<style scoped>
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
