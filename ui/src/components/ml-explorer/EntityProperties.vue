<template>
	<div :class="isNested ? 'nested': ''">
		<p class="no-rels" v-if="filteredProperties.length == 0">No properties</p>
		<div v-else>
			<div v-if="!isNested" class="prop-grid">
				<div class="grid-head">Name</div>
				<div class="grid-head">Type</div>
			</div>
			<div class="overflow" v-for="(prop, index) in filteredProperties" :key="index">
				<entity-details-row
					:prop="prop"
					@showBinary="showBinary"/>
			</div>
		</div>
	</div>
</template>

<script>
import EntityDetailsRow from '@/components/ml-explorer/EntityDetailsRow.vue'

export default {
	name: 'EntityProperties',
	props: {
		properties: { type: Array },
		entity: {type: Object},
		isNested: { type: Boolean, default: false }
	},
	components: {
		EntityDetailsRow
	},
	computed: {
		filteredProperties() {
			return this.properties ? this.properties.map(p => {
				return {
					label: p.name,
					type: p.type,
					isStructured: p.isStructured,
					value: this.entity[p.name]
				}
			}) : []
		},
	},
	methods: {
		showBinary(meta) {
			this.$emit('showBinary', meta)
		}
	}
}
</script>

<style lang="less" scoped>
* {
	box-sizing: border-box;
}

.nested {
	margin-left: 5px;
}

.overflow {
	overflow: auto;
}
</style>
