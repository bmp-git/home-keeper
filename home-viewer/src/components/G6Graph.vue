<template>
  <div id="graph-container"></div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import { initGraph } from "@/Graph";

@Component
export default class G6Graph extends Vue {
  @Prop() private data!: any;
  @Prop() private width!: any;
  @Prop() private height!: any;

  mounted() {
    const graph = initGraph("graph-container", this.width, this.height);

    console.log(graph);

    graph.data(this.data);
    graph.render();

    graph.on("node:mouseenter", (evt: any) => {
      const { item } = evt;
      graph.setItemState(item, "hover", true);
    });

    graph.on("node:mouseleave", (evt: any) => {
      const { item } = evt;
      graph.setItemState(item, "hover", false);
    });
  }
}
</script>

<style scoped>
polygon {
  cursor: grab !important;
}
</style>