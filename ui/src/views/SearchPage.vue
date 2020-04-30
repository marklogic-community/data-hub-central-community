<template>
  <section>
    <div class="search row">
      <div class="col-xs-12 search-row">
        <ml-input :qtext="qtext" :search="search" :suggest="suggest" class="search"></ml-input>
      </div>
      <div class="col-xs-12 col-sm-4 col-md-3 facets-col">
        <ml-facets v-if="facets" :facets="facets" :toggle="toggleFacet" :active-facets="activeFacets" :negate="toggleNegatedFacet"></ml-facets>
      </div>
      <div class="col-xs-12 col-sm-8 col-md-9 results-col">
        <i class="fa fa-refresh pull-right" :class="searchPending ? 'fa-spin' : ''"
          v-on:click.prevent="$forceUpdate()"></i>
        <transition name="fade" mode="out-in">
          <h4 v-if="!results">Do a search to get results</h4>
          <h4 v-else-if="total === 0">No results to show</h4>
          <div v-else class="results">
            <div class="pagination-ctrls">
              <b-pagination size="sm" v-model="page" v-on:change="pageChanged" :limit="10" boundary-links="true" :total-rows="total" :per-page="pageLength">
              </b-pagination>
              <!--div class="col-sm-12 col-lg-5" id="search-operator-dropdowns">
                <ml-select label="'Snippet Size'" current-selection="mlSearch.getSnippet() || 'compact'" selection-list="snippetList" on-select="setSnippet(selectionName)"></ml-select>
                <ml-select label="'Sort'" current-selection="mlSearch.getSort() || 'score'" selection-list="sortList" on-select="setSort(selectionName)"></ml-select>
              </div-->
            </div>
            <ml-metrics :metrics="metrics"></ml-metrics>
            <ml-results :results="results"></ml-results>
          </div>
        </transition>
      </div>
    </div>
  </section>
</template>

<script>
import mlFacets from '@/components/ml-search/ml-facets.vue';
import mlInput from '@/components/ml-search/ml-input.vue';
import mlMetrics from '@/components/ml-search/ml-metrics.vue';
import mlResults from '@/components/ml-search/ml-results.vue';
import mlSelect from '@/components/ml-select.vue';
import SearchApi from '@/api/SearchApi.js';

export default {
  name: 'SearchPage',
  props: ['type'],
  data() {
    return {
      searchPending: false
    };
  },
  components: {
    mlFacets,
    mlInput,
    mlMetrics,
    mlResults,
    mlSelect
  },
  computed: {
    isLoggedIn() {
      return this.$store.state.auth.authenticated;
    },
    searchState() {
      return this.$store.state.search[this.type];
    },
    facets() {
      return this.searchState.facets || {};
    },
    pageLength() {
      return this.searchState.pageLength || 10;
    },
    total() {
      return this.searchState.total || 0;
    },
    results() {
      return this.searchState.results;
    },
    qtext() {
      return this.searchState.qtext || '';
    },
    activeFacets() {
      return this.searchState.activeFacets || {};
    },
    seconds() {
      return this.searchState.seconds || 0;
    },
    metrics() {
      return {
        total: this.total,
        page: this.page,
        pageLength: this.pageLength,
        seconds: this.seconds
      };
    }
  },
  created() {
    if (this.isLoggedIn) {
      this.page = this.$store.getters['search/' + this.type + '/page'] || 1;
    }
  },
  mounted() {
    if (this.$route.params && this.$route.params.refresh) {
      this.search();
    }
  },
  watch: {
    isLoggedIn: function(isLoggedIn) {
      if (isLoggedIn) {
        this.search();
      }
    }
  },
  methods: {
    toggleFacet(facet, type, value) {
      console.log('Toggle ' + facet + ' ' + type + ' ' + value);
      this.searchPending = true;
      this.$store
        .dispatch('search/' + this.type + '/toggleFacet', {
          facet,
          type,
          value
        })
        .then(() => {
          this.searchPending = false;
        });
    },
    toggleNegatedFacet(facet, type, value) {
      console.log('Negate ' + facet + ' ' + type + ' ' + value);
      this.searchPending = true;
      this.$store
        .dispatch('search/' + this.type + '/toggleFacet', {
          facet,
          type,
          value,
          negated: true
        })
        .then(() => {
          this.searchPending = false;
        });
    },
    pageChanged(page) {
      console.log('Paging to ' + page);
      this.searchPending = true;
      this.$store
        .dispatch('search/' + this.type + '/paginate', { page })
        .then(() => {
          this.searchPending = false;
        });
    },
    search(qtext) {
      console.log('Searching for ' + qtext);
      this.searchPending = true;
      this.$store
        .dispatch('search/' + this.type + '/search', {
          qtext
        })
        .then(() => {
          this.searchPending = false;
        });
    },
    suggest(val) {
      console.log('Suggest ' + val);
      return SearchApi.suggest(this.type, val).then(response => {
        return response.suggestions;
      });
    }
  }
};
</script>

<style lang="less" scoped>
.search-row {
  margin-top: 20px;
  form {
    padding-bottom: 0;
  }
}
</style>
