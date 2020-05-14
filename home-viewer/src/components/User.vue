<template>
  <div @click="pinUserCard" class="pointer">
    <p style="text-align: center; margin-bottom: 0px">{{ name }}</p>
    <UserAvatar :name="this.name"></UserAvatar>
    <h6 style="text-align: center">
      {{ userProperties.get("position").value }}
    </h6>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import UserAvatar from "@/components/UserAvatar.vue";

@Component({ components: { UserAvatar } })
export default class User extends Vue {
  @Prop() private name: string;
  private userProperties = new Map([["position", { value: "" }]]);

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateUserContent();
  }

  private updateUserContent() {
    const user = this.$store.state.homeProperties.users.find((u: any) => u.name === this.name);
    if (user) {
      this.userProperties = new Map(user.properties.map((p: any) => [p.name, p]));
    }
  }

  private pinUserCard() {
    (this.$parent as any).pinUserCard(this.name);
  }
}
</script>

<style scoped>
.pointer {
  cursor: pointer;
}
</style>
