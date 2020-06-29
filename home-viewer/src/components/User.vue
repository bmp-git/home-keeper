<template>
  <div @click="pinUserCard" class="pointer" style="text-align: center">
    <p style=" margin-bottom: 0">{{ name }}</p>
    <UserAvatar :name="this.name" style="display: inline-block;"></UserAvatar>
    <h6>
      <template v-if="userProperties.get('user_position').value.type === 'in_room'">
        {{ userProperties.get("user_position").value.floor }} -> {{userProperties.get("user_position").value.room}}
      </template>
      <template v-else>
        {{ userProperties.get("user_position").value.type }}
      </template>
    </h6>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import UserAvatar from "@/components/UserAvatar.vue";

@Component({ components: { UserAvatar } })
export default class User extends Vue {
  @Prop() private name: string;
  private userProperties = new Map([["user_position", { value: "" }]]);

  @Watch("$store.state.homeProperties", { deep: true })
  private onHomePropertiesChange() {
    this.updateUserContent();
  }

  private updateUserContent() {
    const user = this.$store.state.homeProperties.users.find((u: any) => u.name === this.name);
    if (user) {
      this.userProperties = new Map(user.properties.map((p: any) => [p.semantic, p]));
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
