<template>
  <v-container fluid>
    <v-row align="start" justify="start">
      <v-col mb="10">
        <FloorSelector
          :selected-floor-index.sync="selectedFloorIndex"
          :floor-names="floors.map(f => f.name)"
        ></FloorSelector>
      </v-col>
      <v-col mb="1">
        <v-btn
          :hidden="showBindButton === false"
          color="success"
          class="ma-2 white--text"
          @click.prevent="bindBtnClicked"
        >
          <v-icon left>mdi-link-variant</v-icon>
          Bind
        </v-btn>
        <v-btn
          :hidden="showUnBindButton === false"
          color="warning"
          class="ma-2 white--text"
          @click.prevent="unbindBtnClicked"
        >
          <v-icon left>mdi-link-variant-off</v-icon>
          Unbind
        </v-btn>
        <v-btn
          color="blue-grey"
          class="ma-2 white--text"
          @click.prevent="uploadBtnClicked"
        >
          Upload
          <v-icon right dark>mdi-cloud-upload</v-icon>
        </v-btn>
        <input
          type="file"
          ref="inputFile"
          accept="image/svg+xml"
          @change="loadImage"
          hidden
        />
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
        <v-card class="mx-auto text-center justify-center py-2" raised outlined>
          <v-tabs v-model="selectedClassIndex" center-active centered>
            <v-tab v-for="c in classes" :key="c.name">
              {{ c.name }}
            </v-tab>
          </v-tabs>
          <v-tabs-items v-model="selectedClassIndex">
            <v-tab-item v-for="(c, cindex) in classes" :key="c.name">
              <v-list dense>
                <v-list-item-group
                  v-model="selectedEntityIndexes[cindex]"
                  color="primary"
                >
                  <v-list-item
                    v-for="item in classes[cindex].values"
                    :key="item.name"
                  >
                    <v-list-item-icon>
                      <v-icon v-if="item && item.isBound" color="light-grey"
                        >mdi-link-variant</v-icon
                      >
                    </v-list-item-icon>

                    <v-list-item-content>
                      <v-list-item-title v-text="item.name">
                      </v-list-item-title>
                    </v-list-item-content>
                  </v-list-item>
                </v-list-item-group>
              </v-list>
            </v-tab-item>
          </v-tabs-items>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { Component, Vue, Watch } from "vue-property-decorator";
import { server, uploadSVG, getSVG } from "@/Api.ts";
import $ from "jquery";
import FloorSelector from "@/components/FloorSelector.vue";

@Component({ components: { FloorSelector } })
export default class Settings extends Vue {
  private floors = this.$store.state.homeTopology.floors.map((f: { name: string, level: number }) => ({
    name: f.name,
    level: f.level,
    svg: ""
  }));
  private selectedFloorIndex = this.floors.findIndex((f: any) => f.level === 0);
  private selectedEntityIndexes: any[] = [];
  private lastPathSelected: any = null;
  private selectedClassIndex = 0;
  private boundEntities: string[] = [];

  private initializeSelectedEntityIndexs() {
    for (let i = 0; i < this.classes.length; i++) {
      this.selectedEntityIndexes[i] = null;
    }
  }

  mounted() {
    $(".clickable").on("click", "path", event =>
      this.onPathSelect(event.currentTarget)
    );

    this.initializeSelectedEntityIndexs();
    this.updateFloorsSvg();
  }

  private updateBoundEntities() {
    const floorSelector = $(`#obj_${this.selectedFloorIndex}`);
    const readEntities = $.makeArray(
      floorSelector.find("path[data-bindid]").map((index, domElement) => {
        return $(domElement).attr("data-bindid");
      })
    );
    this.boundEntities = readEntities.filter(e =>
      this.classes
        .flatMap(c => c.values)
        .map(v => v.name)
        .includes(e)
    );

    const toRemove = readEntities.filter(x => !this.boundEntities.includes(x));
    console.log("Removed incorrect bound ids from svg: " + toRemove);
    toRemove.forEach(e =>
      floorSelector
        .find(`path[data-bindid=${$.escapeSelector(e)}]`)
        .removeAttr("data-bindid")
    );
  }

  private getSelectedEntityIndex() {
    return this.selectedEntityIndexes[this.selectedClassIndex];
  }

  private setSelectedEntityIndex(value: number) {
    this.selectedEntityIndexes[this.selectedClassIndex] = value;
  }

  private getSelectedEntities() {
    return this.classes[this.selectedClassIndex].values;
  }

  private getSelectedEntity() {
    if (!(this.getSelectedEntityIndex() == null)) {
      return this.getSelectedEntities()[this.getSelectedEntityIndex()];
    }
    return null;
  }

  @Watch("selectedEntityIndexes", { deep: true })
  private onSelectedEntityIndexes() {
    const entity = this.getSelectedEntity();

    if (!(entity == null) && entity.isBound) {
      this.deselectLastPath();
      this.setCurrentPath(
        $(`#obj_${this.selectedFloorIndex}`).find(
          `path[data-bindid=${$.escapeSelector(entity.name)}]`
        )
      );
    } else if ($(this.lastPathSelected).is("[data-bindid]")) {
      this.deselectLastPath();
    }
  }

  @Watch("selectedClassIndex")
  private onSelectedClassIndex() {
    console.log(this.selectedEntityIndexes);
    this.onSelectedEntityIndexes();
  }

  @Watch("selectedFloorIndex")
  private onSelectedFloorChange() {
    this.initializeSelectedEntityIndexs();
    this.deselectLastPath();
    this.updateBoundEntities();
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

    this.updateBoundEntities();
    console.log(this.boundEntities);
  }

  private deselectLastPath() {
    $(this.lastPathSelected).removeClass("path_selected");
    this.lastPathSelected = null;
  }

  private setCurrentPath(path: any) {
    if (!(this.lastPathSelected == null)) {
      this.deselectLastPath();
    }
    this.lastPathSelected = path;
    $(path).addClass("path_selected");
  }

  private selectBoundedEntity(path: any) {
    const name = $(path).attr("data-bindid");

    console.log("SVG-selected: " + name);

    if (name == null) {
      if (this.getSelectedEntity()?.isBound) {
        this.setSelectedEntityIndex(null);
      }
      return;
    }

    for (let i = 0; i < this.classes.length; i++) {
      const c = this.classes[i];
      for (let j = 0; j < c.values.length; j++) {
        if (c.values[j].name === name) {
          this.selectedClassIndex = i;
          this.setSelectedEntityIndex(j);
          return;
        }
      }
    }
  }

  private onPathSelect(path: any) {
    if (path === this.lastPathSelected) {
      this.deselectLastPath();
    } else {
      this.setCurrentPath(path);
      this.selectBoundedEntity(path);
    }
  }

  private addIsBound(item: any) {
    item.isBound = this.boundEntities.includes(item.name);
    return item;
  }

  get rooms() {
    return this.$store.state.homeTopology.floors[this.selectedFloorIndex].rooms.map(
      this.addIsBound
    );
  }

  private distinctName(value: any, index: any, array: any): boolean {
    return array.findIndex((i: any) => i.name === value.name) === index;
  }

  get doors() {
    return this.$store.state.homeTopology.floors[this.selectedFloorIndex].rooms
      .flatMap((r: any) => r.doors)
      .filter(this.distinctName)
      .map(this.addIsBound);
  }

  get windows() {
    return this.$store.state.homeTopology.floors[this.selectedFloorIndex].rooms
      .flatMap((r: any) => r.windows)
      .filter(this.distinctName)
      .map(this.addIsBound);
  }

  get classes() {
    return [
      { name: "Rooms", values: this.rooms },
      { name: "Doors", values: this.doors },
      { name: "Windows", values: this.windows }
    ];
  }

  get showBindButton() {
    return (
      !(this.getSelectedEntity() == null) &&
      !(this.lastPathSelected == null) &&
      !this.getSelectedEntity().isBound
    );
  }

  get showUnBindButton() {
    const propertyTrigger = this.lastPathSelected; //force vue re-computation
    return (
      !(this.getSelectedEntity() == null) && this.getSelectedEntity().isBound
    );
  }

  private uploadBtnClicked() {
    const inputFile = this.$refs.inputFile as any;
    inputFile.click();
  }

  private loadImage(event: any) {
    const selectedFloor = this.floors[this.selectedFloorIndex];
    const img = event.target.files?.[0];

    if (img) {
      const reader = new FileReader();

      reader.onloadend = () => {
        uploadSVG(
          reader.result,
          selectedFloor.name,
          () => {
            console.log("SVG successfully uploaded!");
            this.updateFloorsSvg();
          },
          () => {
            console.log("SVG upload failed!");
          }
        );
      };

      reader.onabort = () => {
        alert("Error during file reading! (Aborted).");
      };

      reader.onerror = () => {
        alert("Error during file reading!");
      };

      reader.readAsText(img);
    }
  }

  private uploadCurrentSvg() {
    const html = this.$refs["obj_" + this.selectedFloorIndex] as any;
    const svg = html[0].innerHTML;
    uploadSVG(
      svg,
      this.floors[this.selectedFloorIndex].name,
      () => {
        console.log("SVG successfully updated!");
        this.updateFloorsSvg();
      },
      () => {
        console.log("SVG update failed!");
      }
    );
  }

  private unbindBtnClicked() {
    $(this.lastPathSelected).removeAttr("data-bindid");
    this.deselectLastPath();
    this.setSelectedEntityIndex(null);
    this.uploadCurrentSvg();
  }

  private bindBtnClicked() {
    $(this.lastPathSelected).attr("data-bindid", this.getSelectedEntity().name);
    this.deselectLastPath();
    this.setSelectedEntityIndex(null);
    this.uploadCurrentSvg();
  }
}
</script>

<style>
.custom-loader {
  animation: loader 1s infinite;
  display: flex;
}
@-moz-keyframes loader {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
@-webkit-keyframes loader {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
@-o-keyframes loader {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
@keyframes loader {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}

.path_selected {
  fill: darkseagreen !important;
  fill-opacity: 0.5 !important;
}

path[data-bindid] {
  fill: cornflowerblue;
  fill-opacity: 0.3;
}
</style>
