<template>
  <v-container fluid>
    <v-row align="start" justify="start">
      <v-col cols="8">
        <FloorSelector
          :selected-floor-index.sync="selectedFloorIndex"
          :floor-names="floors.map(f => f.name)"
        ></FloorSelector>
      </v-col>
      <v-col cols="1" align="end" justify="end">
        <v-btn @click="toggleSvg()" :disabled="disableToggleSvg">
          {{ showSvg ? "Hide map" : "Show map" }}
        </v-btn>
      </v-col>
      <v-col cols="3">
        <UsersList :users="usersNames"></UsersList>
      </v-col>
    </v-row>
    <v-row>
      <transition name="custom-transition" enter-active-class="animate__animated animate__bounceInLeft" leave-active-class="animate__animated animate__bounceOutLeft">
        <v-col cols="9" v-show="showSvg">
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
      </transition>
      <transition name="custom-transition" enter-active-class="animate__animated animate__bounceIn" leave-active-class="animate__animated animate__bounceOut">
        <v-col v-show="showCards">
          <div style="height:calc(100vh - 231px);overflow-x: hidden; overflow-y: auto;">
            <EntitiesViewer
              ref="entitiesViewer"
              :selected-floor-index="selectedFloorIndex">
            </EntitiesViewer>
          </div>
        </v-col>
      </transition>
    </v-row>
    <Tooltip ref="tooltip"></Tooltip>
  </v-container>
</template>

<script lang="ts">
import { Component, Vue, Watch } from "vue-property-decorator";
import FloorSelector from "@/components/FloorSelector.vue";
import Tooltip from "@/components/Tooltip.vue";
import EntitiesViewer from "@/components/EntitiesViewer.vue";
import $ from "jquery";
import { getSVG } from "@/Api";
import UsersList from "@/components/UsersList.vue";
import { flatHome } from "@/Utils";
import { initialSelectedFloorIndex } from "@/Utils";

@Component({
  components: { FloorSelector, Tooltip, EntitiesViewer, UsersList }
})
export default class Home extends Vue {
  private usersNames: string[] = this.$store.state.homeTopology.users.map(
    (u: any) => u.name
  );

  private floors = this.$store.state.homeTopology.floors.map(
    (f: { name: string; level: number }) => ({
      name: f.name,
      level: f.level,
      svg: ""
    })
  );

  private selectedFloorIndex = initialSelectedFloorIndex(this.floors);
  private tooltip: any = null;
  private showSvg = true;
  private showCards = true;
  private disableToggleSvg = false;

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

  private toggleSvg() {
    this.disableToggleSvg = true;
    const opening = !this.showSvg;

    this.showCards = !this.showCards;
    if (!opening) {
      this.showSvg = !this.showSvg;
    }

    setTimeout(() => {
      if (opening) {
        this.showSvg = !this.showSvg;
      }
      this.showCards = !this.showCards;
      this.disableToggleSvg = false;
    }, 1000);
  }

  private onPathSelect(path: any) {
    console.log("Path clicked!");
    const id = $(path).attr("data-bindid");
    if (id == null) {
      return;
    }
    (this.$refs["entitiesViewer"] as any).addEntity(this.selectedFloorIndex, id);
  }

  private pinUserCard(userName: string) {
    (this.$refs["entitiesViewer"] as any).addUser(userName);
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

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateSvgContent();
  }

  private updateSvgContent() {
    const floor = this.$store.state.homeProperties.floors[
      this.selectedFloorIndex
    ].name;
    const home = flatHome(this.$store.state.homeProperties);
    const entities = home.filter((h: any) => h.floor === floor);

    entities.forEach((e: any) => {
      const svgEntity = $(`#obj_${this.selectedFloorIndex}`).find(`path[data-bindid=${$.escapeSelector(e.entity.name)}]`);
      if (svgEntity[0]) {
        e.entity.properties.forEach((p: any) => {
          Home.cleanSvgStyle(svgEntity);
          if (!(p.value === null)) {
            switch (p.semantic) {
              case "is_open":
                svgEntity.addClass(p.value.open ? "is_open" : "is_closed");
                break;
              case "motion_detection":
                svgEntity.addClass(Date.now() - p.value["last_seen"] <= 5000 ? "motion_detected" : "");
                break;
            }
          }
        });
      }
    });
  }

  private static cleanSvgStyle(svgEntity: any) {
    svgEntity.removeClass([
      "is_open",
      "is_closed",
      "is_light_on",
      "is_light_off",
      "motion_detected"
    ]);
  }
}
</script>

<style>
.is_open {
  fill: red !important;
  fill-opacity: 0.7 !important;
}

.is_closed {
  fill: green !important;
  fill-opacity: 0.7 !important;
}

.is_light_on {
  fill: rgba(255, 255, 0, 0.99) !important;
  fill-opacity: 0.3 !important;
}

.motion_detected {
  fill: rgba(198, 198, 90, 0.99) !important;
  fill-opacity: 0.6 !important;
}

.is_light_off {
  fill: rgba(9, 9, 8, 0.39) !important;
  fill-opacity: 0.5 !important;
}

.entity_card {
  width: 350px;
}

.hide-native-scrollbar {
  scrollbar-width: none; /* Firefox 64 /
  -ms-overflow-style: none; / Internet Explorer 11 */
}
/** WebKit */
::-webkit-scrollbar {
  display: none;
}
</style>
