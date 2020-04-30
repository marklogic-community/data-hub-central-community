<template>
  <div v-if="total > 0" class="ml-metrics search-metrics">
    Showing {{ pageStart }}-{{ pageEnd }} of {{ total }}<span v-if="!showDuration">.</span>
    <span v-if="showDuration && (seconds >= 0)">
      <span> in {{ seconds }} seconds.</span>
    </span>
  </div>
</template>

<script>
export default {
  name: 'ml-metrics',
  props: {
    metrics: {
      type: Object,
      default: () => {
        return {};
      }
    },
    showDuration: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    total() {
      return this.metrics.total;
    },
    pageLength() {
      return this.metrics.pageLength || 10;
    },
    pageStart() {
      return (this.metrics.page - 1) * this.pageLength + 1;
    },
    pageEnd() {
      return Math.min(this.pageStart + this.pageLength - 1, this.total);
    },
    seconds() {
      return this.metrics.seconds;
    }
  }
};
</script>

<style lang="less" scoped>
.ml-metrics {
  padding-bottom: 10px;
  font-style: italic;
}
</style>
