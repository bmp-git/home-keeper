<template>
  <div class="home">
    <img alt="Vue logo" src="../assets/logo.png" />
    <HelloWorld msg="Welcome to Your Vue.js App" />
    <template v-if="this.$store.state.home != null">
      <div v-for="floor in this.$store.state.home.floors" :key="floor.name"> 
        {{ floor.name }}
        <div v-for="room in floor.rooms" :key="room.name"> 
          {{ room.name }} 
        </div>
      </div>
    </template>    
  </div>
</template>

<script>
// @ is an alias to /src
import HelloWorld from "@/components/HelloWorld.vue";

export default {
  name: "Home",
  components: {
    HelloWorld,
  }
};

</script>
