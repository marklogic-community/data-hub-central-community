<template>
	<div :class="prop.isStructured ? 'structured-card': ''">
		<div class="prop-grid grid-row" :class="expanded ? 'expanded': ''">
			<div @click="expanded = !expanded" :class="prop.isStructured ? 'is-structured': ''">
				<i v-if="prop.isStructured" class="fa fa-angle-right" :class="expanded ? 'expanded': ''"></i>
				{{prop.label}}
			</div>
			<div v-if="prop.isStructured"></div> <!-- needed for grid layout -->
			<template v-else>
				<template v-for="(value, idx) in asArray(prop.value)">
					<div :key="`${idx}-value`">
						<template v-if="value && value.contentType">
							<v-tooltip bottom>
								<template v-slot:activator="{ on: tooltip }">
									<span class="clickable-binary"
										v-on="{ ...tooltip }"
										@click="showBinary(value)">
										<i :class="typeIcon(value.contentType)"></i>
										<span>{{value.value}}</span>
									</span>
								</template>
								<span>Preview {{value.value}}</span>
							</v-tooltip>
						</template>
						<template v-else-if="value && value.length > 100 && !textExpanded">
							<span>{{value | truncate(100, '')}}</span>
							<a class="more-less" @click="textExpanded = true">(more...)</a>
						</template>
						<template v-else>
							<span>{{value}}</span>
							<a class="more-less" v-if="value && value.length > 100" @click="textExpanded = false">(less...)</a>
						</template>
					</div>
					<div :key="`${idx}-blank`" v-if="idx < asArray(prop.value).length - 1"></div>
				</template>
			</template>
		</div>
		<template v-if="prop.isStructured">
			<div></div> <!-- needed for grid layout -->
			<div class="structured" :class="expanded ? 'expanded': ''">
				<entity-properties
					:properties="getProperties(prop.type)"
					:entity="prop.value[prop.type]"
					:isNested="true"
					@showBinary="showBinary"
					/>
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
			expanded: false,
			textExpanded: false
		}
	},
	methods: {
		showBinary(meta) {
			this.$emit('showBinary', meta)
		},
		getProperties(entityName) {
			return ((this.model.nodes[entityName.toLowerCase()] || {}).properties || [])
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
	&.expanded {
		transform: rotate(90deg);
	}
}

.is-structured {
	cursor: pointer;
}

&.grid-row {
	border-radius: 4px 4px 0px 0px;
	&:hover {
		background-color: #eee;
	}

	&.expanded {
		border: 1px solid rgb(68, 73, 156);
		background-color: rgb(68, 73, 156);
		color: white;
		border-bottom: 0px;
	}
}

.structured {
	border: none;
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
