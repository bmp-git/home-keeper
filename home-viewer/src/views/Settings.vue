<template>
  <v-container fluid>
    <v-row align="start" justify="start">
      <v-col mb="10">
        <v-btn-toggle v-model="selectedFloorIndex" mandatory>
          <v-btn v-for="floor in floors" :key="floor.name">
            {{ floor.name }}
          </v-btn>
        </v-btn-toggle>
      </v-col>
      <v-col mb="1">
        <v-btn
          :hidden="showBindButton === false"
          color="success"
          class="ma-2 white--text"
          @click.prevent="bindbtnClicked"
        >
          Bind
        </v-btn>
        <v-btn
          :hidden="showUnBindButton === false"
          color="warning"
          class="ma-2 white--text"
          @click.prevent="bindbtnClicked"
        >
          Unbind
        </v-btn>
        <v-btn
          :loading="btnuploadloading"
          :disabled="btnuploadloading"
          color="blue-grey"
          class="ma-2 white--text"
          @click.prevent="uploadbtnClicked"
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
          <v-list dense>
            <v-list-item-group v-model="selectedEntityIndex" color="primary">
              <v-list-item v-for="item in items" :key="item.name">
                <v-list-item-icon>
                  <v-icon v-if="item.isBound" color="light-grey"
                    >mdi-link-variant</v-icon
                  >
                </v-list-item-icon>

                <v-list-item-content>
                  <v-list-item-title v-text="item.name"> </v-list-item-title>
                </v-list-item-content>
              </v-list-item>
            </v-list-item-group>
          </v-list>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { Component, Vue, Watch } from "vue-property-decorator";
import draggable from "vuedraggable";
import { server, uploadSVG, getSVG } from "@/Api.ts";
import $ from "jquery";

@Component({ components: { draggable } })
export default class Settings extends Vue {
  private selectedFloorIndex = 0;
  private selectedEntityIndex: any = null;
  private lastPathSelected: any = null;
  private selectedClassIndex = 0;
  private bindedEntities: string[] = [];

  private btnuploadloading = false;

  mounted() {
    this.updateFloorsSvg();
  }

  private updateBindedEntities() {
    this.bindedEntities = $.makeArray(
      $("path[data-bindid]").map((index, domElement) => {
        return $(domElement).attr("data-bindid");
      })
    );
  }

  @Watch("selectedClassIndex")
  private onSelectedClassIndex() {
    this.selectedEntityIndex = null;
  }

  @Watch("selectedFloorIndex")
  private onSelectedFloorChange() {
    this.selectedEntityIndex = null;
    this.deselectLastPath();
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

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    Promise.all(promises).then(this.onSvgLoad);
  }

  private onSvgLoad() {
    $("svg").attr("height", "100%");
    $("svg").attr("width", "100%");
    $(document).on("click", "path", event =>
      this.onPathSelect(event.currentTarget)
    );
    $("svg")
      .find("*")
      .css("pointer-events", "none");
    $("svg")
      .find("path")
      .css("pointer-events", "all");

    this.updateBindedEntities();
    console.log(this.bindedEntities);
  }

  private deselectLastPath() {
    $(this.lastPathSelected).removeClass("path_selected");
    this.lastPathSelected = null;
  }

  private setCurrentPath(path: any) {
    if (this.lastPathSelected) {
      this.deselectLastPath();
    }
    this.lastPathSelected = path;
    $(path).addClass("path_selected");
  }

  private selectBoundedEntity(path: any) {
    const name = $(path).attr("data-bindid");
    if (name == null) {
      return;
    }

    for (let i = 0; i < this.classes.length; i++) {
      const c = this.classes[i];
      for (let j = 0; j < c.values.length; j++) {
        if (c.values[j].name === name) {
          this.selectedClassIndex = i;
          this.selectedEntityIndex = j;
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
    console.log("evento");
  }

  private floors = this.$store.state.home.floors.map((f: { name: string }) => ({
    name: f.name,
    svg: ""
  }));

  get rooms() {
    return this.$store.state.home.floors[this.selectedFloorIndex].rooms;
  }

  private distinctName(value: any, index: any, array: any): boolean {
    return array.findIndex((i: any) => i.name === value.name) === index;
  }

  get doors() {
    return this.$store.state.home.floors[this.selectedFloorIndex].rooms
      .flatMap((r: any) => r.doors)
      .filter(this.distinctName);
  }

  get windows() {
    return this.$store.state.home.floors[this.selectedFloorIndex].rooms
      .flatMap((r: any) => r.windows)
      .filter(this.distinctName);
  }

  get classes() {
    return [
      { name: "Rooms", values: this.rooms },
      { name: "Doors", values: this.doors },
      { name: "Windows", values: this.windows }
    ];
  }

  get items() {
    console.log(this.selectedEntityIndex);
    return this.classes[this.selectedClassIndex].values.map((v: any) => {
      v.isBound = this.bindedEntities.includes(v.name);
      return v;
    });
  }

  get showBindButton() {
    return (
      !(this.lastPathSelected == null) && !(this.selectedEntityIndex == null)
    );
  }

  get showUnBindButton() {
    let res = false;
    if (!(this.selectedEntityIndex == null)) {
      res = this.items[this.selectedEntityIndex].isBound;
    }
    return res;
  }

  private uploadbtnClicked() {
    this.btnuploadloading = true;
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
          res => {
            console.log("SVG successfully uploaded!");
            this.btnuploadloading = false;
            this.updateFloorsSvg();
          },
          err => {
            console.log("SVG upload failed!");
            this.btnuploadloading = false;
          }
        );
      };

      reader.onabort = () => {
        alert("Error during file reading! (Aborted).");
        this.btnuploadloading = false;
      };

      reader.onerror = () => {
        alert("Error during file reading!");
        this.btnuploadloading = false;
      };

      reader.readAsText(img);
    }
  }

  private bindbtnClicked() {
    $(this.lastPathSelected).attr(
      "data-bindid",
      this.classes[this.selectedClassIndex].values[this.selectedEntityIndex]
        .name
    );
    this.deselectLastPath();
    this.selectedEntityIndex = null;
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
