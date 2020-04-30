<template>
  <div class="similar-items">
    <i class="fa fa-refresh fa-spin pull-right" v-if="loading"></i>
    <h3 v-if="title">{{ title }}</h3>
    <ul>
      <li v-for="(result, $index) in similar" :key="$index">
        <router-link :to="{ name: 'root.view', params: {id: result.id || encodeURIComponent(result) } }">{{ resultLabel(result) }}</router-link>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'ml-similar',
  props: {
    title: {
      type: String,
      default() {
        return '';
      }
    },
    uri: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      loading: false,
      similar: []
    };
  },
  methods: {
    updateSimilar() {
      if (this.uri) {
        this.loading = true;
        // TODO: fetching similar docs needs a better middleware/backend solution
        this.$http({
          method: 'GET',
          url: '/v1/resources/extsimilar',
          params: {
            'rs:uri': this.uri
          },
          auth: {
            username: this.$store.state.auth.username,
            password: this.$store.state.auth.password,
            sendImmediately: true
          }
        }).then(
          response => {
            this.similar = response.data.similar;
            this.loading = false;
          },
          error => {
            console.log(error);
            this.loading = false;
          }
        );
      }
    },
    resultLabel(result) {
      if (result.label || result.uri) {
        return result.label || result.uri.split('/').pop();
      } else {
        return result.split('/').pop();
      }
    }
  },
  watch: {
    uri(newUri) {
      if (newUri) {
        this.updateSimilar();
      }
    }
  },
  mounted() {
    this.updateSimilar();
  }
};
</script>
