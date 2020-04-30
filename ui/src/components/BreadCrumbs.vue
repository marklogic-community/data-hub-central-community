<template>
  <nav aria-label="breadcrumbs">
    <ol class="breadcrumb">
      <li class="breadcrumb-item" v-if="history.length > crumbs.length">..</li>
      <li class="breadcrumb-item" v-for="(crumb, $index) in crumbs" :key="$index" :class="isLast($index) ? 'active' : ''" v-bind="isLast($index) ? { 'aria-current': 'page' } : {}">
        <router-link v-if="!isLast($index)" :to="crumb">{{ crumb.meta.label }}</router-link>
        <span v-if="isLast($index)">{{ crumb.meta.label }}</span>
      </li>
    </ol>
  </nav>
</template>

<script>
import _ from 'lodash';

const data = {
  history: []
};

export default {
  name: 'BreadCrumbs',
  data() {
    return data;
  },
  computed: {
    crumbs() {
      if (data.history.length === 0) {
        data.history.push(_.clone(this.$route, true));
      }
      return data.history.slice(-10);
    }
  },
  watch: {
    $route: function(newRoute) {
      data.history.push(_.clone(newRoute, true));
      if (data.history.length > 11) {
        data.history.unshift();
      }
    }
  },
  methods: {
    isLast($index) {
      return this.crumbs.length === $index + 1;
    }
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
