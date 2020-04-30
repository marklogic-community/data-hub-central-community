<template>
  <friendly-json :json="json"></friendly-json>
</template>

<script>
import * as X2JS from 'x2js';
import friendlyJson from '@/components/friendly-json.vue';

const x2js = new X2JS();

export default {
  name: 'friendly-xml',
  components: {
    friendlyJson
  },
  props: {
    xml: {
      type: String,
      required: true
    }
  },
  computed: {
    json() {
      return x2js.xml2js(this.xml);
    }
  },
  methods: {
    // copied from angularjs
    isArray(arr) {
      return Array.isArray(arr) || arr instanceof Array;
    },
    isFunction(value) {
      return typeof value === 'function';
    },
    isObject(value) {
      return value !== null && typeof value === 'object';
    }
  }
};
</script>

<style lang="less" scoped>
.dl-horizontal {
  margin: 0;

  dt {
    width: 100px;
    text-align: left;
  }
  dd {
    margin-left: 20px;
  }
  .dl-horizontal {
    margin-bottom: 0;
  }
}
</style>
