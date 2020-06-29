<template>
  <v-app id="inspire">
    <v-app-bar
      :clipped-left="$vuetify.breakpoint.lgAndUp"
      app
      color="blue darken-3"
      dark
    >
      <v-toolbar-title style="width: 300px" class="ml-0 pl-4">
        <router-link style="color: white; text-decoration: none" to="/"
          ><span class="hidden-sm-and-down">Home viewer</span></router-link
        >
      </v-toolbar-title>
      <v-spacer />
      <v-btn icon to="/">
        <v-icon>mdi-home</v-icon>
      </v-btn>
      <v-btn icon to="/settings">
        <v-icon>mdi-cog</v-icon>
      </v-btn>

      <v-menu
        v-model="menu"
        :close-on-content-click="false"
        :nudge-width="200"
        offset-y
      >
        <template v-slot:activator="{ on }">
          <v-btn icon v-on="on">
            <v-icon>mdi-api</v-icon>
          </v-btn>
        </template>

        <v-card>
          <v-list>
            <v-list-item>
              <v-list-item-avatar>
                <user-avatar name="Admin" size="48"></user-avatar>
              </v-list-item-avatar>

              <v-list-item-content>
                <v-list-item-title>Admin</v-list-item-title>
              </v-list-item-content>

            </v-list-item>
          </v-list>

          <v-divider></v-divider>

          <v-list class="pt-6 pb-0">
            <v-list-item>
              <v-text-field
                label="Server Address"
                v-model="myServer"
                outlined
              ></v-text-field>
            </v-list-item>
            <v-list-item>
              <v-text-field
                label="API Key"
                v-model="myKey"
                outlined
              ></v-text-field>
            </v-list-item>
          </v-list>

          <v-card-actions class="pt-0">
            <v-spacer></v-spacer>

            <v-btn text @click="menu = false">Cancel</v-btn>
            <v-btn color="primary" text @click="onApiDropDownSave">Save</v-btn>
          </v-card-actions>
        </v-card>
      </v-menu>
    </v-app-bar>

    <v-content>
      <router-view />
    </v-content>
  </v-app>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import "material-design-icons-iconfont/dist/material-design-icons.css";
import UserAvatar from "@/components/UserAvatar.vue";

@Component({ components: { UserAvatar } })
export default class App extends Vue {
  private fav = true;
  private menu = false;

  private myServer = this.$store.state.serverAddress;
  private myKey = this.$store.state.apiKey;

  private onApiDropDownSave() {
    this.$store.commit("updateServerAddress", this.myServer);
    this.$store.commit("updateApiKey", this.myKey);
    window.location.reload();
  }
}
</script>

<style></style>
