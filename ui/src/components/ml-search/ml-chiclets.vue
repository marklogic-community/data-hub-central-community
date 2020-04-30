<template>
  <div class="chiclets">
    <div v-for="(facet, facetName, $index) in activeFacets" :key="$index">
      <div class="btn chiclet" v-for="(value, $index) in facet.values" :key="$index" :class="{ 'btn-primary': !value.negated, 'btn-warning': value.negated, negated: value.negated }" v-on:click.prevent="!!toggle && toggle(facetName, facet.type, value.value)">
        <span :title="value.value">{{ facetName }}: {{ value.value }} </span>
        <span class="glyphicon glyphicon-remove-circle icon-white"></span>
      </div>
    </div>
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
  }
};
</script>
