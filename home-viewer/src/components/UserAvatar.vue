<template>
  <v-avatar :size="size">
    <img
      v-if="avatarExists"
      :src="avatarRelativePath"
      :alt="name"
      @error="avatarExists = false"
    />
    <v-icon v-else dark>mdi-account-circle</v-icon>
  </v-avatar>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";

@Component
export default class User extends Vue {
  @Prop() private name: string;
  @Prop() private size: number;
  private avatarExists = true;

  get avatarRelativePath() {
    return (this.$store.state.serverAddress + `/home/users/${this.name}/properties/avatar/raw`);
  }
}
</script>