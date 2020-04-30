<template>
  <dl class="dl-horizontal">
    <span v-for="(val, key, $index) in json" :key="$index" v-show="!((isFunction(val)) || ((''+key).startsWith('_') && key !== '__text'))">
      <dt>{{ (''+key).startsWith('__') ? key.replace('__', '') : key }}</dt>
      <!-- simple value -->
      <dd v-if="!isObject(val)">{{ val !== '' ? val : '&#160;' }}</dd>
      <!-- array or object -->
      <span v-if="isObject(val)">
        <!-- array with simple values -->
        <dd v-if="isArray(val) && !isObject(val[0])">{{ val.join(', ') }}</dd>
        <!-- add nbsp for better alignment of values -->
        <dd v-if="!isArray(val) || isObject(val[0])">&#160;</dd>
        <!-- object, recurse -->
        <dd v-if="!isArray(val)">
          <!--span ng-init="json = val" ng-include="'/view-file-ng/friendly-json.html'"></span-->
          <friendly-json :json="val"></friendly-json>
        </dd>
        <!-- array of object, repeat recurse -->
        <dd v-if="isArray(val) && isObject(val[0])" v-for="(v, $index) in val" :key="$index">
          <!--span ng-repeat="json in val track by $index" ng-include="'/view-file-ng/friendly-json.html'"></span-->
          <friendly-json :json="v"></friendly-json>
        </dd>
      </span>
    </span>
  </dl>
</template>

<script>
export default {
  name: 'friendly-json',
  props: {
    json: {
      type: [Object, Array],
      required: true
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
