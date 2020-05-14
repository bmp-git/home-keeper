<template>
  <v-card class="entity_card">
    <v-system-bar color="primary" dark>
      <v-spacer></v-spacer>

      <v-icon
        v-if="properties.length > 0 || actions.length > 0"
        @click="show = !show"
        >{{ show ? "mdi-chevron-up" : "mdi-chevron-down" }}</v-icon
      >
      <v-icon v-if="isClosable" @click="closeCard()">mdi-close</v-icon>
    </v-system-bar>
    <v-card-text>
      <p class="headline text--primary" style="margin-bottom:8px;">
        <UserAvatar :name="name"></UserAvatar>
        {{name}} <span style="font-size: small">in {{position}}</span>
      </p>

      <v-expand-transition>
        <div v-show="show">
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
  private position = "";

  private show = true;

  mounted() {
    this.updateCardContent();
  }

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateCardContent();
  }

  private updateCardContent() {
    const user = this.$store.state.homeProperties.users.find((u: any) => u.name === this.name);
    if (user) {
      this.properties = user.properties;
      this.actions = user.actions;
      this.position = this.properties.find((p : any) => p.name === "position")['value'];
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
</style>
