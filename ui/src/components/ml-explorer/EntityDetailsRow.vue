<template>
	<div>
		<div class="array" v-if="prop.isArray && isArray(prop.value)">
			<div class="prop-grid grid-row array-header">
				<div><i class="fa fa-angle-right hidden"></i> {{prop.label}}</div>
				<div>[ ]</div>
			</div>
			<div v-if="!prop.isStructured">
				<template v-for="(value, idx) in prop.value">
					<div :key="`${idx}-child`" class="prop-grid grid-row">
						<div></div>
						<div>{{value}}</div>
					</div>
				</template>
			</div>
			<div v-else>
				<template v-for="(value, idx) in prop.value">
					<div :key="`${idx}-array-expander`" @click="toggleExpanded(idx)" class="prop-grid grid-row clickable" :class="expanded[idx] ? 'expanded' : ''">
						<div><i class="fa fa-angle-right" :class="expanded[idx] ? 'expanded': ''"></i> [ {{idx}} ]</div>
						<div></div>
					</div>
					<div :key="`${idx}-array-value`" class="structured" :class="expanded[idx] ? 'expanded': ''">
						<entity-properties
							:properties="getProperties(prop.type)"
							:entity="value[prop.type] || {}"
							:isNested="true"
							@showBinary="showBinary"
							/>
					</div>
				</template>
			</div>
		</div>
		<template v-else-if="prop.isStructured" class="structured-card">
			<div class="prop-grid grid-row clickable" :class="expanded[0] ? 'expanded': ''" @click="toggleExpanded(0)">
				<div><i v-if="prop.isStructured" class="fa fa-angle-right" :class="expanded[0] ? 'expanded': ''"></i> {{prop.label}}</div>
				<div></div>
			</div>
			<div class="structured expanded" :class="expanded[0] ? 'expanded': ''" v-if="expanded[0]">
				<entity-properties
					:properties="getProperties(prop.type)"
					:entity="(prop.value ? prop.value[prop.type] : {}) || {}"
					:isNested="true"
					@showBinary="showBinary"
					/>
			</div>
		</template>
		<template v-else>
			<div class="prop-grid grid-row">
				<div><i class="fa fa-angle-right hidden"></i> {{prop.label}}</div>
				<div>
					<template v-if="prop.value && prop.value.contentType">
						<v-tooltip bottom>
							<template v-slot:activator="{ on: tooltip }">
								<span class="clickable-binary"
									v-on="{ ...tooltip }"
									@click="showBinary(prop.value)">
									<i :class="typeIcon(prop.value.contentType)"></i>
									<span>{{prop.value.value}}</span>
								</span>
							</template>
							<span>Preview {{prop.value.value}}</span>
						</v-tooltip>
					</template>
					<template v-else-if="prop.value && prop.value.length > 100 && !textExpanded">
						<span>{{prop.value | truncate(100, '')}}</span>
						<a class="more-less" @click="textExpanded = true">(more...)</a>
					</template>
					<template v-else>
						<span>{{prop.value}}</span>
						<a class="more-less" v-if="prop.value && prop.value.length > 100" @click="textExpanded = false">(less...)</a>
					</template>
				</div>
			</div>
		</template>
	</div>
</template>

<script>
import { mapState } from 'vuex'
import _ from 'lodash'

export default {
	name: 'EntityDetailsRow',
	props: {
		prop: { type: Object }
	},
	components: {
		EntityProperties: () => import('@/components/ml-explorer/EntityProperties.vue')
	},
	computed: {
		...mapState({
			model: state => state.model.model
		})
	},
	data() {
		return {
			expanded: {},
			textExpanded: false
		}
	},
	methods: {
		toggleExpanded(idx) {
			this.$set(this.expanded, idx, !this.expanded[idx])
		},
		showBinary(meta) {
			this.$emit('showBinary', meta)
		},
		getProperties(entityName) {
			return ((this.model.nodes[entityName.toLowerCase()] || {}).properties || [])
		},
		isArray(value) {
			return _.isArray(value)
		},
		asArray(prop) {
			if (prop instanceof Array) {
				return prop
			}
			return [prop]
		},
		typeIcon(type) {
			let icon = null
			if (type.match('application/pdf')) {
				icon = "fa fa-file-pdf-o"
			}
			else if (type.match(/^audio\//)) {
				icon = "fa fa-file-audio-o"
			}
			else if (type.match(/^video\//)) {
				icon = "fa fa-file-video-o"
			}
			else if (type.match(/^image\//)) {
				icon = "fa fa-file-image-o"
			}
			return icon
		}
	}
}
</script>

<style lang="less" scoped>
* {
	box-sizing: border-box;
}

span.clickable-binary {
	.fa {
		font-size: 24px;
	}

	i {
		margin-right: 5px;
	}
	span {
		font-size: 10px;
	}
	cursor: pointer;
}

i.fa-angle-right {
	margin-right: 10px;
	transition: 0.25s ease;
	margin-left: 0.5rem;

	&.hidden {
		visibility: hidden;
	}

	&.expanded {
		transform: rotate(90deg);
	}
}

.is-structured {
	cursor: pointer;
}

&.grid-row {
	border-radius: 4px 4px 0px 0px;

	&.clickable {
		cursor: pointer;
	}

	&.expanded {
		border: 1px solid rgb(68, 73, 156);
		background-color: rgb(68, 73, 156);
		color: white;
		border-bottom: 0px;
		margin: 5px;
    margin-bottom: 0;
	}
}

.array {
	border: 1px solid #b0cb2c;
	margin: 5px;
	border-radius: 4px;
	.array-header {
		background-color: #b0cb2c;
		border-bottom: 1px solid #b0cb2c;

	}
}

.structured {
	max-height: 0;
	overflow: auto;
	transition: 0.25s ease;
	border: 1px solid transparent;
	border-radius: 0px 0px 4px 4px;

	&.expanded {
		padding: 2px;
		border: 1px solid rgb(68, 73, 156);
		max-height: inherit;
		transition: 0.25s ease;
		margin: 5px;
    margin-top: 0;
	}
}

.structured-card {
	margin: 4px;
	margin-bottom: 10px;
	border-radius: 4px;
	box-shadow: 0px 3px 1px -2px rgba(0, 0, 0, 0.2), 0px 2px 2px 0px rgba(0, 0, 0, 0.14), 0px 1px 5px 0px rgba(0, 0, 0, 0.12);
	overflow: auto;
}

.more-less {
	float: right;
}
</style>
