<template>
  <div v-if="actions && actions.length > 0" class="text--primary mt-3">
    <div class="text--secondary" style="text-align: right">
      Actions
    </div>
    <v-divider class="mb-1"></v-divider>
    <v-row v-for="(action, index) in actions" :key="action.name">
      <template v-if="action['semantic'] === 'trig'">
        <v-col cols="4">
          <p>{{ action["name"] }}</p>
        </v-col>
        <v-col cols="8" align="right">
          <v-btn @click="onTrigAction(index)">Trig</v-btn>
        </v-col>
      </template>
      <template v-else-if="action['semantic'] === 'turn'">
        <v-col cols="4">
          <p>{{ action["name"] }}</p>
        </v-col>
        <v-col cols="8" align="right">
          <v-btn @click="onTurnActionOff(index)">Off</v-btn>
          <v-btn class="ml-2" @click="onTurnActionOn(index)">On</v-btn>
        </v-col>
      </template>
      <template v-else-if="action['semantic'] === 'file_write'"> </template>
      <template v-else>
        <v-col cols="4">
          <p>{{ action["name"] }}</p>
        </v-col>
        <v-col cols="8" align="right">
          <v-text-field
            label="value"
            outlined
            dense
            v-model="payload[index]"
            :error-messages="errors[index]"
            append-outer-icon="send"
            @click:append-outer="onGenericAction(index)"
            @keyup.enter="onGenericAction(index)"
          ></v-text-field>
        </v-col>
      </template>
    </v-row>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import { postAction } from "@/Api";

@Component
export default class ActionsViewer extends Vue {
  @Prop() private actions: [];
  @Prop() private entityUrl: string;

  private payload: string[] = [];
  private errors: string[] = [];

  private getActionRelativePath(name: string) {
    return `${this.entityUrl}/actions/${name}`;
  }

  private onGenericAction(index: number) {
    postAction(
      this.getActionRelativePath(this.actions[index]["name"]),
      this.payload[index],
      res => {
        console.log(
          "Action post " + this.actions[index]["name"] + "succeeded!"
        );
        this.payload[index] = "";
        this.errors[index] = "";
      },
      err => {
        console.log("Error on post action " + this.actions[index]["name"]);
        this.errors[index] = "Invalid payload.";
      }
    );
  }

  private onTurnActionOn(index: number) {
    postAction(
      this.getActionRelativePath(this.actions[index]["name"]),
      "true",
      res => {
        console.log(
          "Action post " + this.actions[index]["name"] + "succeeded!"
        );
      },
      err => {
        console.log("Error on post action " + this.actions[index]["name"]);
      }
    );
  }

  private onTurnActionOff(index: number) {
    postAction(
      this.getActionRelativePath(this.actions[index]["name"]),
      "false",
      res => {
        console.log(
          "Action post " + this.actions[index]["name"] + "succeeded!"
        );
      },
      err => {
        console.log("Error on post action " + this.actions[index]["name"]);
      }
    );
  }

  private onTrigAction(index: number) {
    postAction(
      this.getActionRelativePath(this.actions[index]["name"]),
      "",
      res => {
        console.log(
          "Action post " + this.actions[index]["name"] + "succeeded!"
        );
      },
      err => {
        console.log("Error on post action " + this.actions[index]["name"]);
      }
    );
  }
}
</script>