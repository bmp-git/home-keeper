<template>
  <div v-if="properties && properties.length > 0" class="text--primary mt-3">
    <div class="text--secondary" style="text-align: right">
      Properties
    </div>
    <v-divider class="mb-1"></v-divider>
    <table width="100%" id="properties_table">
      <tr v-for="prop in properties" :key="prop.name" class="mb-4">
      <template v-if="prop['semantic'] === 'video'">
          <td colspan="2">
            <div>{{ prop["name"] }}:</div>
            <img
              :src="`${getPropertyAbsolutePath(prop.name)}/raw`"
              style="display:block; width:100%;"
            />
          </td>
        </template>
        <template v-else-if="prop['semantic'] === 'time'">
          <td>{{ prop.name + ": " }}</td>
          <td align="center">{{ prop.value | timeFormat }}</td>
        </template>
        <template v-else-if="prop['semantic'] === 'ble_receiver'">
          <td>Users seen:</td>
          <td>
            <table>
              <tr v-for="record in prop.value" :key="record.user">
                <td>{{ record.user }}</td>
                <td>{{ record["last_seen"] | timeFormat }}</td>
                <td>{{ record["rssi"] + "db" }}</td>
              </tr>
            </table>
          </td>
        </template>
        <template v-else-if="prop['content-type'] === 'application/json'">
          <template v-if="!(prop['value'] == null)">
            <td>{{ prop.name + ": " }}</td>
            <td align="center">
              <table width="100%" style="table-layout:fixed">
                <tr v-for="subprop in Object.keys(prop.value)" :key="subprop">
                  <td align="center">{{subprop}}</td>
                  <td style="word-wrap:break-word">{{prop.value[subprop]}}</td>
                </tr>
              </table>
            </td>
          </template>
          <template v-else-if="prop['error']">
            <td>{{ prop.name + ": " }}</td>
            <td style="color: red;">{{ prop.error }}</td>
          </template>
          <template v-else>
            <td>{{ prop.name + ": " }}</td>
            <td style="color: orange;">Unknown</td>
          </template>
        </template>
      </tr>
    </table>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";

@Component({
  filters: {
    timeFormat(date: number) {
      return new Date(date).toTimeString().split(" ")[0];
    }
  }
})
export default class PropertiesViewer extends Vue {
  @Prop() private properties: [];
  @Prop() private entityUrl: string;

  private getPropertyAbsolutePath(prop: string) {
    return `${this.$store.state.serverAddress}${this.entityUrl}/properties/${prop}`;
  }
}
</script>

<style scoped>
  #properties_table {
    border-spacing: 0 1em;
    padding-top: 0;
  }
  table {
    border-spacing: 0 0.3em;
  }
</style>
