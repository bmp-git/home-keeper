<template>
  <v-card class="entity_card">
    <v-system-bar
            color="primary"
            dark
    >
      <v-spacer></v-spacer>

      <v-icon v-if="this.properties.length > 0 || this.actions.length > 0" @click="show = !show">{{ show ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
      <v-icon v-if="isClosable" @click="closeCard()">mdi-close</v-icon>
    </v-system-bar>
    <v-card-text>
        <p class="headline text--primary" style="margin-bottom:8px;">

          <v-icon v-if="entityType === 'door'"> mdi-door </v-icon>
          <v-icon v-else-if="entityType === 'window'"> mdi-window-closed-variant </v-icon>
          <v-icon v-else-if="entityType === 'room'"> mdi-floor-plan</v-icon>
          <template v-else-if="entityType === 'floor'">
            <v-icon v-if="floorLevel >=0 && floorLevel <= 3"> mdi-home-floor-{{floorLevel}} </v-icon>
            <v-icon v-else-if="floorLevel === -1"> mdi-home-floor-negative-1 </v-icon>
            <v-icon v-else>mdi-home-minus</v-icon>
          </template>
          {{entityId}}

        </p>

      <v-expand-transition>
        <div v-show="show">
          <div v-if="properties && properties.length > 0" class="text--primary mt-3">
            <div class="text--secondary" style="text-align: right">
              Properties
            </div>
            <v-divider class="mb-1"></v-divider>
        <table>
          <tr v-for="prop in properties" :key="prop.name">
            <template v-if="prop['semantic'] === 'video'">
               <td colspan="2">
                 <div>{{prop['name']}}:</div>
                 <img :src="`${getPropertyAbsolutePath(prop.name)}/raw`" style="display:block; width:100%;"/>
               </td>
            </template>
            <template v-else-if="prop['semantic'] === 'time'">
              <td>{{prop.name + ": "}}</td>
              <td>{{prop.value | timeFormat}}</td>
            </template>
            <template v-else-if="prop['semantic'] === 'ble_receiver'">
              <td>Users seen:</td>
              <td>
                <table>
                  <tr v-for="record in prop.value" :key="record.user">
                    <td>{{record.user}}</td>
                    <td>{{record["last_seen"] | timeFormat}}</td>
                    <td>{{record["rssi"] + "db"}}</td>
                  </tr>
                </table>
              </td>
            </template>
            <template v-else-if="prop['content-type'] === 'application/json'">
              <template v-if="prop['value']">
                <td> {{prop.name + ": "}}</td>
                <td> {{prop.value}}</td>
              </template>
              <template v-else-if="prop['error']">
                <td> {{prop.name + ": "}}</td>
                <td style="color: red;"> {{prop.error}}</td>
              </template>
            </template>
          </tr>
        </table>
      </div>
          <div v-if="actions && actions.length > 0" class="text--primary mt-3">
            <div class="text--secondary"  style="text-align: right">
              Actions
            </div>
            <v-divider class="mb-1"></v-divider>
            <v-row v-for="(action, index) in actions" :key="action.name">
              <template v-if="action['semantic'] === 'trig'">
                <v-col cols="4">
                  <p>{{action['name']}}</p>
                </v-col>
                <v-col cols="8" align="right">
                  <v-btn @click="onTrigAction(index)">Trig</v-btn>
                </v-col>
              </template>
              <template v-else-if="action['semantic'] === 'turn'">
                <v-col cols="4">
                  <p>{{action['name']}}</p>
                </v-col>
                <v-col cols="8" align="right">
                  <v-btn @click="onTurnActionOff(index)">Off</v-btn>
                  <v-btn class="ml-2" @click="onTurnActionOn(index)">On</v-btn>
                </v-col>
              </template>
              <template v-else-if="action['semantic'] === 'file_write'">

              </template>
              <template v-else>
                <v-col cols="4">
                  <p>{{action['name']}}</p>
                </v-col>
                <v-col cols="8" align="right">
                  <v-text-field
                          label="value"
                          outlined
                          dense
                          v-model="payload[index]"
                          append-outer-icon="send"
                          @click:append-outer="onGenericAction(index)"
                  ></v-text-field>
                </v-col>
              </template>
            </v-row>
          </div>
        </div>

      </v-expand-transition>

    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import {Component, Vue, Prop, Watch} from "vue-property-decorator";
import { flatHome } from '@/Utils';
import {server, postAction} from "@/Api"

@Component({
  filters: {
    timeFormat(date: number) {
      return new Date(date).toTimeString().split(" ")[0];
    }
  }
})
export default class EntityCard extends Vue {
  @Prop() private floor: number;
  @Prop() private entityId: string;
  @Prop() private isClosable: boolean;

  private entityType = "";
  private properties: [] = [];
  private actions : [] = [];
  private entityUrl: string = null;
  private floorLevel: number = null;

  private payload: any[] = [];

  private show = true;
  //TODO: use floor.level instead of floor index.

  @Watch("$store.state.homeProperties", {deep: true})
  private onHomePropertiesChange() {
    this.updateCardContent();
  }

  private getPropertyAbsolutePath(prop:string) {
    return server + this.entityUrl + "/properties/" + prop;
  }

  private updateCardContent() {
    const floor = this.$store.state.homeProperties.floors[this.floor].name;
    const home = flatHome(this.$store.state.homeProperties);
    const found = home.find((e: any) => e.entity.name === this.entityId && e.floor === floor);

    if (found) {
      this.entityUrl = found.url;
      this.properties = found.entity.properties;
      this.actions = found.entity.actions;
      this.entityType = found.type;
      this.floorLevel = found.level;
    }
  }

  private closeCard() {
    (this.$parent as any).onCardClose({ floor: this.floor, entityId: this.entityId })
  }

  private getActionRelativePath(name: string) {
    return `${this.entityUrl}/actions/${name}`;
  }

  private onGenericAction(index: number) {
    postAction(this.getActionRelativePath(this.actions[index]['name']), this.payload[index], res => {
      console.log("Action post " + this.actions[index]['name'] + "succeeded!")
    }, err => {
      console.log("Error on post action " + this.actions[index]['name'])
    })
  }

  private onTurnActionOn(index: number) {
    postAction(this.getActionRelativePath(this.actions[index]['name']), "true", res => {
      console.log("Action post " + this.actions[index]['name'] + "succeeded!")
    }, err => {
      console.log("Error on post action " + this.actions[index]['name'])
    })
  }

  private onTurnActionOff(index: number) {
    postAction(this.getActionRelativePath(this.actions[index]['name']), "false", res => {
      console.log("Action post " + this.actions[index]['name'] + "succeeded!")
    }, err => {
      console.log("Error on post action " + this.actions[index]['name'])
    })
  }

  private onTrigAction(index: number) {
    postAction(this.getActionRelativePath(this.actions[index]['name']), "", res => {
      console.log("Action post " + this.actions[index]['name'] + "succeeded!")
    }, err => {
      console.log("Error on post action " + this.actions[index]['name'])
    })
  }
}
</script>

<style scoped>
.entity_card {
  margin-bottom:5px;
}
</style>
