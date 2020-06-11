<template>
  <div class="facet-list">
    <ml-chiclets :active-facets="activeFacets" :toggle="toggle"></ml-chiclets>

		<template v-for="(facet, facetName, $index) in facets">
			<v-card :key="$index" v-show="!facet.hide && facet.facetValues.length > 0">
				<v-card-title>{{ facetName }}</v-card-title>
				<v-card-text>
					<template v-for="(value, $index) in facet.facetValues">
						<div v-if="!value.selected" :key="$index">
							<span v-on:click.prevent="toggle(facetName, facet.type, value.name)">
								<i class="fa fa-plus-circle facet-add-pos"></i>
								<span v-if="!!value.name" :title="value.name"> {{ value.name }}</span>
								<em v-if="!value.name">blank</em>
								<span> ({{ value.count }}) </span>
							</span>
							<i v-if="!!negate" class="fa fa-ban facet-add-neg" v-on:click.prevent="negate(facetName, facet.type, value.name)" :title="value.name"></i>
						</div>
					</template>
					<div v-if="!!showMore && !facet.displayingAll">
						<a class="seemore" href v-on:click.prevent="showMore(facet, facetName)">see more ...</a>
					</div>
				</v-card-text>
			</v-card>
		</template>
  </div>
</template>

<script>
import mlChiclets from '@/components/ml-search/ml-chiclets.vue';

export default {
  name: 'ml-facets',
  components: {
    mlChiclets
  },
  props: {
    facets: {
      type: Object,
      required: true
    },
    activeFacets: {
      type: Object,
      default() {
        return {};
      }
    },
    toggle: {
      type: Function,
      required: true
    },
    negate: {
      type: Function
    },
    showMore: {
      type: Function
    }
  },
  methods: {
    hasNonSelectedValues(facet) {
      return (facet.facetValues || []).filter(value => {
        return !value.selected;
      }).length;
    }
  }
};
</script>

<style lang="less" scoped>
.facet-list {
	padding: 0.25em;

  .facet-add-pos,
  .facet-add-neg {
    visibility: hidden;
  }
  span:hover > .facet-add-pos,
  div:hover > .facet-add-neg {
    visibility: visible !important;
  }
}

.seemore {
	padding-left: 15px;
}

.v-card {
	margin-bottom: 20px;
}
.v-card__title {
	text-transform: capitalize;
}
</style>
