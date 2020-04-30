<template>
	<v-footer
    dark color="secondary">
    <v-container mx-auto>
      <v-layout row>
				<div>Copyright <sup>&copy;</sup> {{ currentYear }} MarkLogic Corporation.</div>
      </v-layout>
    </v-container>
  </v-footer>
</template>

<script>
export default {
  name: 'ml-footer',
  data() {
    return {
      currentYear: new Date().getUTCFullYear()
    };
  }
};
</script>
