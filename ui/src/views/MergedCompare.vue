<template>
	<v-container fluid>
		<v-breadcrumbs :items="breadcrumbs"></v-breadcrumbs>

		<v-snackbar
      v-model="showSnack"
      color="success"
      :timeout="3000"
      :top="true"
    >{{snackMessage}}
		</v-snackbar>

		<compare-docs
			v-if="uris"
			:uris="uris"
			:showPreview="true"
			:blocked="blocked"
			:merged="true"
			:mergedDoc="mergedDoc"
			:mergedUri="mergedUri"
			:showActions="false"
			@unmerge="unmerge"
			@block="block"
			@unblock="unblock"
		/>
	</v-container>
</template>

<script>
import CompareDocs from '@/components/mastering/CompareDocs'
import { mapState } from 'vuex'
export default {
	components: {
		CompareDocs
	},
	data() {
		return {
			showSnack: false,
			snackMessage: null,
			breadcrumbs: [
				{
					text: 'Explore',
					to: { name: 'root.explorer', params: { q: '' } },
					disabled: true
				},
				{
					text: 'Merge History',
					disbled: true
				}
			]
		}
	},
	computed: {
		...mapState({
			docs: state => state.mastering.docs
		}),
		mergedDoc() {
			return this.docs[this.mergedUri]
		},
		mergedUri() {
			return this.$route.query.uri
		},
		uris() {
			return this.mergedDoc ? this.mergedDoc.envelope.headers.merges.map(m => m['document-uri']) : null
		},
		blocked() {
			return this.notification ? this.notification.meta.blocked : false
		},
		merged() {
			return this.notification ? this.notification.meta.merged : false
		}
	},
	methods: {
		unmerge() {
			this.showAlert('Unmerge Complete')
		},
		block() {
			this.showAlert('Blocked')
		},
		unblock() {
			this.showAlert('Unblocked')
		},
		showAlert(msg) {
			this.snackMessage = msg
			this.showSnack = true
			setTimeout(() => {
				this.showSnack = false
				this.snackMessage = null
			}, 2000)
		}
	},
	mounted: function() {
		this.$store.dispatch('mastering/getDocs', [this.$route.query.uri])
	},
	watch: {
		'$route.query.uri'() {
			this.$store.dispatch('mastering/getDocs', [this.$route.query.uri])
		}
	}
}
</script>
