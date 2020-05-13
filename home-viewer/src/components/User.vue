<template>
    <div>
        <v-avatar color="primary">
            <img    v-if="avatarExists"
                    :src="avatarRelativePath"
                    :alt="name"
                    @error="avatarExists = false"
            >
            <v-icon v-else dark>mdi-account-circle</v-icon>
        </v-avatar>
        <p style="text-align: center">{{name}}</p>
    </div>
</template>

<script lang="ts">
    import {Component, Vue, Prop, Watch} from "vue-property-decorator";
    import {server} from "@/Api";

    @Component
    export default class User extends Vue {
        @Prop() private name: string;
        private avatarExists = true;

        get avatarRelativePath() {
            return server + `/home/users/${this.name}/properties/avatar/raw`;
        }
    }
</script>

<style scoped>

</style>