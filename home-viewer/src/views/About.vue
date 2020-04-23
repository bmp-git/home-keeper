<template>
  <div id="container" width="100%" height="100%">
    <G6Graph :data="this.data" :width="this.width" :height="this.height" />
  </div>
</template>

<script lang="ts">
import { generateData } from "@/GraphDataGenerator";
import G6Graph from "@/components/G6Graph.vue";
import { Component, Vue } from "vue-property-decorator";

@Component({ components: { G6Graph } })
export default class About extends Vue {
  private data: any = generateData(this.$store.state.home);
  private width: any =
    document.getElementById("container")?.scrollWidth || 1920;
  private height: any =
    document.getElementById("container")?.scrollHeight || 1080;
}
</script>
