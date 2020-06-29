<template>
  <div id="tooltip" role="tooltip" hidden>
    <div id="arrow" data-popper-arrow></div>

    <div class="tooltip_title">
      <h2 class="font-weight-black">
        <v-icon v-if="currentType === 'door'"> mdi-door </v-icon>
        <v-icon v-else-if="currentType === 'window'"> mdi-window-closed-variant </v-icon>
        <v-icon v-else-if="currentType === 'room'"> mdi-floor-plan</v-icon>
        {{ currentId }}
      </h2>
    </div>
    <div v-if="currentProperties && currentProperties.length > 0" class="tooltip_content">
      <table id="properties_table">
        <tr v-for="prop in currentProperties" :key="prop.name" class="mb-4">
          <template v-if="prop['semantic'] === 'ble_receiver'"></template>
          <template v-else-if="prop['semantic'] === 'wifi_receiver'"></template>
          <template v-else-if="prop['semantic'] === 'smartphone_data'"></template>

          <template v-else-if="prop['semantic'] === 'video'">
            <td colspan="2">
              <div>{{ prop["name"] }}:</div>
              <img
                :src="`${getPropertyAbsolutePath(prop.name)}/raw`"
                style="display:block; width:100%;"
              />
            </td>
          </template>
          <template v-else-if="prop['semantic'] === 'time'">
            <td>{{ prop.name + ": " }}</td>
            <td align="center">{{ prop.value | timeFormat }}</td>
          </template>
          <template v-else-if="prop['content-type'] === 'application/json'">
            <template v-if="!(prop['value'] == null)">
              <td>{{ prop.name + ": " }}</td>
              <td align="center">
                <table style="table-layout:fixed">
                  <tr v-for="subprop in Object.keys(prop.value)" :key="subprop">
                    <td align="center" v-if="!Array.isArray(prop.value)">{{subprop}}</td>
                    <template v-if="prop.value[subprop] instanceof Object">
                      <td>
                        <table style="table-layout:auto;">
                          <tr v-for="subsubprop in Object.keys(prop.value[subprop])" :key="subsubprop">
                            <td align="center" v-if="!Array.isArray(prop.value[subprop])">{{subsubprop}}</td>
                            <td style="word-wrap:break-word; word-break: break-all;">{{propertyFormatter(subsubprop, prop.value[subprop][subsubprop])}}</td>
                          </tr>
                        </table>
                      </td>
                    </template>
                    <template v-else>
                      <td style="word-wrap:break-word"> {{ propertyFormatter(subprop, prop.value[subprop]) }} </td>
                    </template>
                  </tr>
                </table>
              </td>
            </template>
            <template v-else-if="prop['error']">
              <td>{{ prop.name + ": " }}</td>
              <td style="color: red;">{{ prop.error }}</td>
            </template>
            <template v-else>
              <td>{{ prop.name + ": " }}</td>
              <td style="color: orange;">Unknown</td>
            </template>
          </template>
        </tr>
      </table>
    </div>
  </div>
</template>

<script lang="ts">
import $ from "jquery";
import { Component, Vue, Watch } from "vue-property-decorator";
import { createPopper } from "@popperjs/core";
import { flatHome } from "@/Utils";

@Component({
  filters: {
    timeFormat(date: number) {
      return new Date(date).toTimeString().split(" ")[0];
    }
  }
})
export default class Tooltip extends Vue {
  private popper: any = null;

  private currentProperties: [] = null;
  private currentType: string = null;
  private currentFloor: number = null;
  private currentId: string = null;
  private currentEntityUrl: string = null;

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateTooltipContent();
  }

  private getPropertyAbsolutePath(prop: string) {
    return `${this.$store.state.serverAddress}${this.currentEntityUrl}/properties/${prop}`;
  }

  private floorName() {
    return this.$store.state.homeProperties.floors[this.currentFloor].name;
  }
  private getPropertyPath(prop:string) {
    return this.$store.state.serverAddress + this.currentEntityUrl + "/properties/" + prop;
  }

  private propertyFormatter(name: string, value: any) {
    if (name == "last_seen" || name == "timestamp" || name == "last_change") {
      return this.$options.filters.timeFormat(value);
    } else {
      return value;
    }
  }

  private updateTooltipContent() {
    if (!(this.currentFloor == null) && !(this.currentId == null)) {
      const floor = this.floorName();
      const home = flatHome(this.$store.state.homeProperties);
      const found = home.find((e: any) => e.entity.name === this.currentId && e.floor === floor);
      if (found) {
        this.currentEntityUrl = found.url;
        this.currentProperties = found.entity.properties;
        this.currentType = found.type;

        console.log(this.currentProperties, this.currentType, this.currentFloor, this.currentId);
      }
    }
  }

  public createTooltip(bindto: any, floor: number, id: string) {
    console.log("tooltip created!", floor, id);

    this.currentFloor = floor;
    this.currentId = id;

    this.updateTooltipContent();

    const svgEntity = $(bindto).get(0);
    const tooltip = $("#tooltip").get(0);
    this.popper = createPopper(svgEntity, tooltip, { placement: "auto" });
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

ul {
  list-style: none;
}

td {
  padding:0 5px 0 0; /* Only right padding*/
}

.tooltip_content {
  margin-top: 10px;
  max-width: 300px;
}
</style>
