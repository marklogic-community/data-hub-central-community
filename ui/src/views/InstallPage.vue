<template>
	<v-container fluid fill-height>
		<v-layout align-center justify-center>
			<v-flex xs12 sm8 md4>
				<div class="text-center">
					<v-progress-circular
						indeterminate
						color="primary"
					></v-progress-circular>
					<div>Installing...</div>
				</div>
			</v-flex>
		</v-layout>
	</v-container>
</template>

<script>
import { mapState } from 'vuex'

export default {
	name: 'InstallPage',
  computed: {
		...mapState({
			needsInstall: state => state.auth.needsInstall
		})
	},
	watch: {
		needsInstall(newVal) {
			console.log('needsInstall Changed: ', newVal)
			if (!newVal) {
				this.$router.replace({ name: 'root.landing' });
			}
		}
	}
};
</script>
