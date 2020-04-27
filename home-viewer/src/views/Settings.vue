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
          v-html="floor.svg"
          :hidden="selectedFloorIndex !== index"
        ></div>
      </v-col>
      <v-col lg="3" mb="6">
        <v-card class="mx-auto text-center justify-center py-2" raised outlined>
          <v-tabs v-model="selectedClass" center-active centered>
            <v-tab v-for="c in classes" :key="c.name">
              {{ c.name }}
            </v-tab>
          </v-tabs>
          <v-list dense>
            <v-list-item-group v-model="selectedEntityIndex" color="primary">
              <draggable v-model="items">
                <v-list-item v-for="item in items" :key="item.name">
                  <v-list-item-content>
                    <v-list-item-title v-text="item.name"></v-list-item-title>
                  </v-list-item-content>
                </v-list-item>
              </draggable>
            </v-list-item-group>
          </v-list>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import draggable from "vuedraggable";
import { server, uploadSVG, getSVG } from "@/Api.ts";
import $ from "jquery";

@Component({ components: { draggable } })
export default class Settings extends Vue {
  private selectedFloorIndex = 0;
  private selectedEntityIndex = 0;

  private btnuploadloading = false;

  mounted() {
    this.updateFloorsSvg();
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
    $(document).on("click", "path", function() {
      const clickedBtnID = $(this).attr("id"); // or var clickedBtnID = this.id
      console.log("evento")
    });

    $('svg').find('*').css("pointer-events", "none")
    $('svg').find('path').css("pointer-events", "all")

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

  private selectedClass = 0;

  get items() {
    console.log(this.selectedEntityIndex);
    return this.classes[this.selectedClass].values;
  }

  set items(values) {
    if (Array.isArray(values)) {
      this.classes[this.selectedClass].values = values;
    }
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
</style>
