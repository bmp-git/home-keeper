<template>
  <div class="entity_viewer">
    <HomeCard />
    <EntityCard
      :floor="selectedFloorIndex"
      :entity-id="
        this.$store.state.homeTopology.floors[selectedFloorIndex].name
      "
      :is-closable="false"
    ></EntityCard>
    <UserCard
      v-for="user in users"
      :key="user.name"
      :name="user.name"
      :is-closable="true"
    ></UserCard>
    <EntityCard
      v-for="entity in entities"
      :key="`${entity.entityId}_${entity.floor}`"
      :floor="entity.floor"
      :entity-id="entity.entityId"
      :is-closable="true"
    ></EntityCard>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import EntityCard from "@/components/EntityCard.vue";
import UserCard from "@/components/UserCard.vue";
import { addToArrayIfNot, removeFromArray } from "@/Utils";
import HomeCard from "@/components/HomeCard.vue";

@Component({ components: { UserCard, EntityCard, HomeCard } })
export default class EntitiesViewer extends Vue {
  @Prop() private selectedFloorIndex: number;
  private entities: { floor: number; entityId: string }[] = [];
  private users: { name: string }[] = [];

  private addEntity(floor: number, entityId: string) {
    addToArrayIfNot(
      this.entities,
      e => e.entityId === entityId && e.floor === floor,
      { floor: floor, entityId: entityId }
    );
  }

  private removeEntity(floor: number, entityId: string) {
    removeFromArray(
      this.entities,
      (obj: any) => obj.floor === floor && obj.entityId === entityId
    );
    console.log(this.entities);
  }

  private addUser(name: string) {
    addToArrayIfNot(this.users, e => e.name === name, { name: name });
  }

  private removeUser(name: string) {
    removeFromArray(this.users, (obj: any) => obj.name === name);
  }

  private onCardClose(value: { floor: number; entityId: string }) {
    this.removeEntity(value.floor, value.entityId);
  }

  private onUserCardClose(userName: string) {
    this.removeUser(userName);
  }
}
</script>

<style scoped>
.entity_viewer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-around;
  align-items: flex-start;
  align-content: flex-end;
}
</style>
