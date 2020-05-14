<template>
  <v-card class="entity_card">
    <v-system-bar color="primary text--primary" dark :class="(this.properties.length > 0 || this.actions.length > 0) && open ? '' : 'minimized_card'">
      <span>
        <v-icon v-if="entityType === 'door'"> mdi-door </v-icon>
        <v-icon v-else-if="entityType === 'window'">
          mdi-window-closed-variant
        </v-icon>
        <v-icon v-else-if="entityType === 'room'"> mdi-floor-plan</v-icon>
        <template v-else-if="entityType === 'floor'">
          <v-icon v-if="floorLevel >= 0 && floorLevel <= 3">
            mdi-home-floor-{{ floorLevel }}
          </v-icon>
          <v-icon v-else-if="floorLevel === -1">
            mdi-home-floor-negative-1
          </v-icon>
          <v-icon v-else>mdi-home-minus</v-icon>
        </template>
      </span>
      <span>{{ entityId }}</span>

      <v-spacer></v-spacer>

      <v-icon
        v-if="this.properties.length > 0 || this.actions.length > 0"
        @click="open = !open"
        >{{ open ? "mdi-chevron-up" : "mdi-chevron-down" }}</v-icon
      >
      <v-icon v-if="isClosable" @click="closeCard()">mdi-close</v-icon>
    </v-system-bar>
    <v-card-text :class="(this.properties.length > 0 || this.actions.length > 0) && open ? 'pt-0' : 'pb-0 pt-0'">
      <v-expand-transition>
        <div v-show="open">
          <PropertiesViewer
            :properties="this.properties"
            :entity-url="this.entityUrl"
          ></PropertiesViewer>
          <ActionsViewer
            :actions="this.actions"
            :entity-url="this.entityUrl"
          ></ActionsViewer>
        </div>
      </v-expand-transition>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import { flatHome } from "@/Utils";
import PropertiesViewer from "@/components/PropertiesViewer.vue";
import ActionsViewer from "@/components/ActionsViewer.vue";

@Component({ components: { ActionsViewer, PropertiesViewer } })
export default class EntityCard extends Vue {
  @Prop() private floor: number;
  @Prop() private entityId: string;
  @Prop() private isClosable: boolean;
  private open = true;

  private entityType = "";
  private properties: [] = [];
  private actions: [] = [];
  private entityUrl: string = null;
  private floorLevel: number = null;

  mounted() {
    this.updateCardContent();
  }

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateCardContent();
  }

  public isOpenAndHasElements() {
    return !!((this.properties.length > 0 || this.actions.length > 0) && open);
  }

  private updateCardContent() {
    const floor = this.$store.state.homeProperties.floors[this.floor].name;
    const home = flatHome(this.$store.state.homeProperties);
    const found = home.find(
      (e: any) => e.entity.name === this.entityId && e.floor === floor
    );

    if (found) {
      this.entityUrl = found.url;
      this.properties = found.entity.properties;
      this.actions = found.entity.actions;
      this.entityType = found.type;
      this.floorLevel = found.level;
    }
  }

  private closeCard() {
    (this.$parent as any).onCardClose({
      floor: this.floor,
      entityId: this.entityId
    });
  }
}
</script>

<style scoped>
.entity_card {
  margin-bottom: 5px;
}

.minimized_card {
  border-bottom-right-radius: inherit;
  border-bottom-left-radius: inherit;
}
</style>
