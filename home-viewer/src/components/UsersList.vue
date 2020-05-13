<template>
    <v-row justify="space-around">
        <User
                v-for="(user, index) in users"
                :key="index"
                :name="user"
        ></User>
    </v-row>
</template>

<script lang="ts">
    import {Component, Vue, Prop} from "vue-property-decorator";
    import User from "@/components/User.vue";

    @Component({ components: { User } })
    export default class UsersList extends Vue {
        @Prop() private users: string[];
    }
</script>

<style scoped>

</style>