<template>
	<v-container fluid class="detailspage">
		<v-row justify="center" class="header-row">
			<v-col md="8">
				<h3 class="text-center">{{ uri }}</h3>
				<div class="text-center"><v-chip>{{ metadata.contentType }}</v-chip></div>
			</v-col>
		</v-row>
		<v-row justify="center">
			<v-col md="3" class="meta">
				<dl class="row">
					<v-card width="100%">
						<v-card-title>Collections</v-card-title>
						<v-card-text>
							<dl class="row" v-show="metadata.collections">
								<dd class="col-sm-9">
									<span v-for="(c, $index) in metadata.collections" :key="$index">
										{{ c }}<span v-show="$index !== metadata.collections.length - 1">, </span>
									</span>
								</dd>
							</dl>
						</v-card-text>
					</v-card>

					<v-card>
						<v-card-text>
							<dl class="row">
								<dt class="col-sm-3">Content-Type</dt>
								<dd class="col-sm-9">{{ metadata.contentType }}</dd>

								<dt class="col-sm-3">File Name</dt>
								<dd class="col-sm-9">{{ metadata.fileName }}</dd>

								<dt class="col-sm-3">Format</dt>
								<dd class="col-sm-9">{{ metadata.format }}</dd>
							</dl>
						</v-card-text>
					</v-card>
				</dl>
				<dl class="row" v-if="metadata.metadataValues">
					<v-card>
						<v-card-title>Metadata</v-card-title>
						<v-card-text>
							<dl>
								<dl v-for="(v, key, $index) in metadata.metadataValues" class="row" :key="$index">
									<dt class="col-sm-5">{{ key }}</dt>
									<dd class="col-sm-7">{{ v }}</dd>
								</dl>
							</dl>
						</v-card-text>
					</v-card>
				</dl>
				<dl class="row" v-if="metadata.permissions">
					<v-card width="100%">
						<v-card-title>Permissions</v-card-title>
						<v-card-text>
							<dl>
								<dd class="col-sm-9">
									<span v-for="(p, $index) in metadata.permissions" :key="$index">
										{{ p }}<span v-show="$index !== metadata.permissions.length - 1">, </span>
									</span>
								</dd>
							</dl>
						</v-card-text>
					</v-card>

					<v-card>
						<v-card-text>
							<dl class="row">
								<dt class="col-sm-3">Quality</dt>
								<dd class="col-sm-9">{{ metadata.quality }}</dd>

								<dt class="col-sm-3">Size</dt>
								<dd class="col-sm-9">{{ metadata.size }} bytes</dd>

								<dt class="col-sm-3">Uri</dt>
								<dd class="col-sm-9">{{ metadata.uri }}</dd>
							</dl>
						</v-card-text>
					</v-card>
				</dl>
			</v-col>
			<v-col md="9">
				<v-card class="content">
					<v-card-text>
						<v-tabs v-model="tabIndex">
							<v-tab v-if="raw">Raw</v-tab>
							<v-tab>Preview</v-tab>

							<v-tab-item v-if="raw" eager>
								<v-container>
									<pre>
										<code :class="codeClass">{{ raw }}</code>
									</pre>
								</v-container>
							</v-tab-item>

							<v-tab-item>
								<v-container class="preview-container">
									<friendly-json v-if="metadata.format === 'JSON' && json" :json="json"></friendly-json>
									<friendly-xml v-else-if="metadata.format === 'XML' && raw" :xml="raw"></friendly-xml>
									<binary-view v-else :src="uri" :type="metadata.contentType" :title="metadata.fileName"/>
								</v-container>
							</v-tab-item>


						</v-tabs>
					</v-card-text>
				</v-card>
			</v-col>
		</v-row>
	</v-container>
</template>

<script>
import BinaryView from '@/components/BinaryView.vue'
import friendlyJson from '@/components/friendly-json.vue'
import friendlyXml from '@/components/friendly-xml.vue'
import hljs from 'highlight.js/lib/core'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import 'highlight.js/styles/github.css'
hljs.registerLanguage('json', json)
hljs.registerLanguage('xml', xml)

export default {
	name: 'DetailPage',
	components: {
		friendlyJson,
		friendlyXml,
		BinaryView
	},
	props: {
		type: {
			type: String,
			default: 'all'
		},
		id: {
			type: String
		}
	},
	data() {
		return {
			metadata: {},
			json: undefined,
			raw: undefined,
			tabIndex: 0
		}
	},
	computed: {
		codeClass() {
			return (this.metadata && this.metadata.format) ? this.metadata.format.toLowerCase() : 'json'
		},
		uri() {
			return this.$route.query.uri
		},
		database() {
			return this.$route.query.db || 'final'
		},
		profile() {
			return this.$store.state.auth.profile || {}
		}
	},
	mounted() {
		this.update()
	},
	methods: {
		update() {
			this.metadata = {}
			this.json = undefined
			this.raw = undefined

			this.$store
				.dispatch('crud/metadata', {
					uri: this.uri,
					db: this.database
				})
				.then((metadata) => {
					var permissions = []
					// flatten permissions for simplified display
					metadata.permissions.forEach(function(p) {
						p.capabilities.forEach(function(c) {
							permissions.push(p['role-name'] + ':' + c)
						})
					})
					metadata.permissions = permissions
					if (metadata.collections.length === 0) {
						delete metadata.collections
					}
					if (metadata.permissions.length === 0) {
						delete metadata.permissions
					}
					if (
						metadata.metadataValues &&
						Object.keys(metadata.metadataValues).length === 0
					) {
						delete metadata.metadataValues
					}
					this.metadata = metadata

					if (metadata.format === 'JSON' || metadata.format === 'XML') {
						this.$store
							.dispatch('crud/doc', { uri: this.uri, db: this.database })
							.then((response) => {
								if (metadata.format === 'JSON') {
									this.json = response
									this.raw = JSON.stringify(this.json, null, 2)
								}
								else if (metadata.format === 'XML') {
									this.raw = response
								}

								this.$nextTick(() => {
									const targets = document.querySelectorAll('code')
									targets.forEach((target) => {
										hljs.highlightBlock(target)
									})
								})
							})
					}
				})
		},
		deleteDoc() {
			if (
				window.confirm(
					'This will permanently delete ' +
						this.metadata.fileName +
						', are you sure?'
				)
			) {
				const toast = this.$parent.$refs.toast
				this.$store
					.dispatch('crud/' + this.type + '/delete', { id: this.uri })
					.then(function(response) {
						if (response.isError) {
							toast.showToast('Failed to delete the document', {
								theme: 'error'
							})
						} else {
							toast.showToast('Successfully deleted the document', {
								theme: 'success'
							})
							this.$router.push({
								name: this.previousRoute
									? this.previousRoute.name
									: 'root.search',
								params: {
									refresh: true,
									...(this.previousRoute ? this.previousRoute.params : {})
								}
							})
						}
					})
			}
		}
	},
	watch: {
		'$route.query': {
			handler(params) {
				if (params) {
					this.update()
				}
			},
			deep: true
		}
	}
}
</script>

<style lang="less" scoped>
.detailspage {
	height: 100%;
	display: flex;
	flex-direction: column;

	.header-row {
		flex: 0;
	}

	.v-card.content {
		height: 100%;

		.v-card__text {
			height: 100%;

			.v-tabs {
				height: 100%;
				display: flex;
				flex-direction: column;

				/deep/ .v-tabs-items {
					flex: 1;

					.v-window__container {
						height: 100%;

						.v-window-item {
							height: 100%;
						}
					}

					.preview-container {
						height: 100%;
					}
				}
			}
		}
	}
}

view-binary {
	display: block;
	height: 600px;
}
code {
	width: 100%;
	display: block;

	/deep/ &.json {
		.hljs-attr {
			color: #000080;
		}
	}
}
.v-card {
	margin-bottom: 20px;
}

.meta {
	dd {
		padding: 12px 0px;
	}
	dt {
		font-weight: bold;
		font-size: 12px;
	}
}
</style>
