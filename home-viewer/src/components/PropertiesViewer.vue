<template>
  <div v-if="properties && properties.length > 0" class="text--primary mt-3">
    <div class="text--secondary" style="text-align: right">
      Properties
    </div>
    <v-divider class="mb-1"></v-divider>
    <table>
      <tr v-for="prop in properties" :key="prop.name">
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
          <td>{{ prop.value | timeFormat }}</td>
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
          <template v-if="prop['value']">
            <td>{{ prop.name + ": " }}</td>
            <td>{{ prop.value }}</td>
          </template>
          <template v-else-if="prop['error']">
            <td>{{ prop.name + ": " }}</td>
            <td style="color: red;">{{ prop.error }}</td>
          </template>
        </template>
      </tr>
    </table>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import { server } from "@/Api";

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
    return `${server}${this.entityUrl}/properties/${prop}`;
  }
}
</script>

<style scoped></style>
