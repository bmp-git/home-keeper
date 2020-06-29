<template>
  <v-card class="entity_card">
    <v-system-bar
      color="primary text--primary"
      dark
      :class=" (this.properties.length > 0 || this.actions.length > 0) && open? '' : 'minimized_card'" >
      <UserAvatar :name="name" size="16"></UserAvatar>
      <span class="pl-2">
        {{ name }}:
        <template v-if="position.type === 'in_room'">
          {{ position.floor }} -> {{ position.room }}
        </template>
        <template v-else>
          {{ position.type }}
        </template>
      </span>

      <v-spacer></v-spacer>

      <v-icon
        v-if="properties.length > 0 || actions.length > 0"
        @click="open = !open" >{{ open ? "mdi-chevron-up" : "mdi-chevron-down" }}</v-icon>
      <v-icon v-if="isClosable" @click="closeCard()">mdi-close</v-icon>
    </v-system-bar>
    <v-card-text
      :class=" (this.properties.length > 0 || this.actions.length > 0) && open ? 'pt-0' : 'pb-0 pt-0'" >
      <v-expand-transition>
        <div v-show="open">
          <PropertiesViewer
            :properties="properties"
            :entity-url="`/home/users/${name}`"
          ></PropertiesViewer>
          <ActionsViewer
            :actions="actions"
            :entity-url="`/home/users/${name}`"
          ></ActionsViewer>
        </div>
      </v-expand-transition>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import PropertiesViewer from "@/components/PropertiesViewer.vue";
import ActionsViewer from "@/components/ActionsViewer.vue";
import UserAvatar from "@/components/UserAvatar.vue";

@Component({ components: { UserAvatar, ActionsViewer, PropertiesViewer } })
export default class UserCard extends Vue {
  @Prop() private name: string;
  @Prop() private isClosable: boolean;

  private properties: [] = [];
  private actions: [] = [];
  private position = { type: "unknown" };

  private open = true;

  mounted() {
    this.updateCardContent();
  }

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateCardContent();
  }

  private updateCardContent() {
    const user = this.$store.state.homeProperties.users.find(
      (u: any) => u.name === this.name
    );
    if (user) {
      this.properties = user.properties;
      this.actions = user.actions;
      this.position = this.properties.find(
        (p: any) => p.semantic === "user_position"
      )["value"];
    }
  }

  private closeCard() {
    (this.$parent as any).onUserCardClose(this.name);
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
