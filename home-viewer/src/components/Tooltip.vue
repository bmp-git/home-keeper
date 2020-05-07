<template>
  <div id="tooltip" role="tooltip" hidden>
    <div id="arrow" data-popper-arrow></div>

    <h2 class="font-weight-black">
      <v-icon v-if="currentType === 'door'">
        mdi-door-closed
      </v-icon>
      <v-icon v-else-if="currentType === 'window'">
        mdi-window-open-variant
      </v-icon>
      <v-icon v-else-if="currentType === 'room'">
        mdi-floor-plan
      </v-icon>
      {{ currentId }}
    </h2>
    <ul v-for="prop in currentProperties" :key="prop.name">
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

  private currentProperties: [] = null;
  private currentType: string = null;
  private currentFloor: number = null;
  private currentId: string = null;

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateTooltipContent();
  }

  private updateTooltipContent() {
    if (!(this.currentFloor == null) && !(this.currentId == null)) {
      const entity = this.$store.state.homeProperties.floors[
        this.currentFloor
      ].rooms
        .flatMap((r: any) => {
          const rooms = [
            { name: r.name, properties: r.properties, type: "room" }
          ];
          const doors = r.doors.map((d: any) => ({
            name: d.name,
            properties: d.properties,
            type: "door"
          }));
          const windows = r.windows.map((w: any) => ({
            name: w.name,
            properties: w.properties,
            type: "window"
          }));
          return rooms.concat(doors, windows);
        })
        .filter((e: any) => e.name == this.currentId)[0];

      this.currentProperties = entity?.properties;
      this.currentType = entity?.type;

      console.log(this.currentProperties, this.currentType, this.currentFloor, this.currentId);
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
    this.currentProperties = [];
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
