<template>
  <div class="entity_viewer">
    <EntityCard
      :floor="selectedFloorIndex"
      :entity-id="this.$store.state.homeTopology.floors[selectedFloorIndex].name"
      :is-closable="false"
    ></EntityCard>
    <EntityCard
      v-for="(entity, index) in entities"
      :key="index"
      :floor="entity.floor"
      :entity-id="entity.entityId"
      :is-closable="true"
    ></EntityCard>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import EntityCard from "@/components/EntityCard.vue";

@Component({ components: { EntityCard } })
export default class EntitiesViewer extends Vue {
  @Prop() private selectedFloorIndex: number;
  @Prop() private entities: { floor: number; entityId: string }[];

  private onCardClose(value: { floor: number; entityId: string }) {
    (this.$parent as any).onCardClose(value)
  }
}
</script>

<style scoped></style>
