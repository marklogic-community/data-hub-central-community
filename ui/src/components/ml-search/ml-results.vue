<template>
  <transition-group name="fade" mode="out-in">
    <div v-for="(result, $index) in results" :key="$index" class="result">
      <h4>
        <router-link :to="{ name: 'root.view', params: { id: result.id } }">{{ resultLabel(result) }}</router-link>
      </h4>
      <div class="matches">
        <div class="match" v-for="(match, $index) in result.matches" :key="$index">
          <em v-for="(text, $index) in match['match-text']" :key="$index">
            <span :class="text.highlight !== undefined ? 'highlight' : ''">{{
              text.highlight !== undefined ? text.highlight : text
            }}</span>
          </em>
        </div>
      </div>
      <hr>
    </div>
  </transition-group>
</template>

<script>
export default {
  name: 'ml-results',
  props: {
    results: {
      type: Array,
      default: () => {
        return [];
      }
    }
  },
  methods: {
    resultLabel(result) {
      return result.label || result.uri.split('/').pop();
    }
  }
};
</script>
