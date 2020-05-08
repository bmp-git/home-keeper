<template>
  <v-card class="entity_card">
    <v-card-text>

      <!--<div>{{entityType}}</div>-->
      <p class="headline text--primary">

        <v-icon v-if="entityType === 'door'"> mdi-door </v-icon>
        <v-icon v-else-if="entityType === 'window'"> mdi-window-closed-variant </v-icon>
        <v-icon v-else-if="entityType === 'room'"> mdi-floor-plan</v-icon>
        <template v-else-if="entityType === 'floor'">
          <v-icon v-if="floor <= 3"> mdi-home-floor-{{floor}} </v-icon>
          <v-icon v-else>mdi-home-minus</v-icon>
        </template>
        {{entityId}}
      </p>

      <div v-if="properties && properties.length > 0" class="text--primary">
        <table>
          <tr v-for="prop in properties" :key="prop.name">
            <template v-if="prop['semantic'] === 'video'">
               <td colspan="2"><img :src="getPropertyPath(prop.name)" style="display:block; width:100%;"/></td>
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
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import {Component, Vue, Prop, Watch} from "vue-property-decorator";
import { flatHome } from '@/Utils';
import {server} from "@/Api"

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
  private entityType = "";
  private properties: [] = [];
  private actions : [] = [];
  private entityUrl: string = null;
  //TODO: use floor.level instead of floor index.

  @Watch("$store.state.homeProperties", {deep: true})
  private onHomePropertiesChange() {
    this.updateCardContent();
  }

  private getPropertyPath(prop:string) {
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
    }
  }
}
</script>

<style scoped>
.entity_card {
  margin-bottom:5px;
}
</style>
