<template>
	<div class="chiclets">
		<span v-for="(facet, facetName, $index) in activeFacets" :key="$index">
			<v-chip v-for="(value, $index) in facet.values" :key="$index" :color="color(value.negated)"
				@click:close="!!toggle && toggle(facetName, facet.type, value.value)"
				@click="!!toggle && toggle(facetName, facet.type, value.value)" close>
				<span :title="value.value">{{ facetName }}: {{ value.value }} </span>
			</v-chip>
		</span>
	</div>
</template>

<script>
export default {
	name: 'ml-chiclets',
	props: {
		activeFacets: {
			type: Object,
			required: true
		},
		toggle: {
			type: Function
		}
	},
	methods: {
		color(negated) {
			return negated ? 'warning' : 'primary'
		}
	}
};
</script>
