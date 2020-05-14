<template>
    <v-card class="entity_card">
        <v-system-bar color="primary text--primary" dark :class="(this.properties.length > 0 || this.actions.length > 0) && open ? '' : 'minimized_card'">
            <span>
                  <v-icon>mdi-home</v-icon>
            </span>
            <span>Home</span>
            <v-spacer></v-spacer>

            <v-icon
                    v-if="properties.length > 0 || actions.length > 0"
                    @click="open = !open"
            >{{ open ? "mdi-chevron-up" : "mdi-chevron-down" }}</v-icon
            >
        </v-system-bar>
        <v-card-text :class="(this.properties.length > 0 || this.actions.length > 0) && open ? 'pt-0' : 'pb-0 pt-0'">


            <v-expand-transition>
                <div v-show="open">
                    <PropertiesViewer
                            :properties="properties"
                            :entity-url="`/home`"
                    ></PropertiesViewer>
                    <ActionsViewer
                            :actions="actions"
                            :entity-url="`/home`"
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

    @Component({ components: { ActionsViewer, PropertiesViewer } })
    export default class HomeCard extends Vue {
        private properties: [] = [];
        private actions: [] = [];

        private open = true;

        mounted() {
            this.updateCardContent();
        }

        @Watch("$store.state.homeProperties", { deep: true })
        private onHomePropertiesChange() {
            this.updateCardContent();
        }

        private updateCardContent() {
            const home = this.$store.state.homeProperties;
            if (home) {
                this.properties = home.properties;
                this.actions = home.actions;
            }
        }
    }
</script>

<style scoped>
    .entity_card {
        margin-bottom: 5px;
    }

    .minimized_card {
        border-bottom-right-radius: inherit;
        border-bottom-left-radius: inherit;
    }
</style>
