<template>
  <div class="facet-list">
    <ml-chiclets :active-facets="activeFacets" :toggle="toggle"></ml-chiclets>

    <div class="facet" v-for="(facet, facetName, $index) in facets" :key="$index"
        v-if="hasNonSelectedValues(facet)" v-show="!facet.hide">
      <h3>{{ facetName }}</h3>
      <div v-for="(value, $index) in facet.facetValues" :key="$index" v-if="!value.selected">
        <span v-on:click.prevent="toggle(facetName, facet.type, value.name)">
          <i class="fa fa-plus-circle facet-add-pos"></i>
          <span v-if="!!value.name" :title="value.name"> {{ value.name }}</span>
          <em v-if="!value.name">blank</em>
          <span> ({{ value.count }}) </span>
        </span>
        <i v-if="!!negate" class="fa fa-ban facet-add-neg" v-on:click.prevent="negate(facetName, facet.type, value.name)" :title="value.name"></i>
      </div>
      <div v-if="!!showMore && !facet.displayingAll">
        <a href v-on:click.prevent="showMore(facetName)">see more ...</a>
      </div>
    </div>
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
  .facet-add-pos,
  .facet-add-neg {
    visibility: hidden;
  }
  span:hover > .facet-add-pos,
  div:hover > .facet-add-neg {
    visibility: visible !important;
  }
}
</style>
