<template>
  <form class="ml-input ml-search form-inline" role="search"
    v-on:submit.prevent="hideSuggestions();search(tmpQtext)">
    <div class="input-group">
      <input ref="input" type="text" class="form-control" placeholder="Search..." autocomplete="off"
        v-model="tmpQtext" v-on:input.prevent="resolveSuggest(tmpQtext)"
        v-on:keydown.down.prevent="focusToSuggestions()"
        v-on:keydown.esc.prevent="hideSuggestions()"/>
      <span v-if="tmpQtext" class="search-input-icon search-input-clear form-control-feedback"
        v-on:click.prevent="clear()">
        <i class="fa fa-times-circle"></i>
      </span>
      <span v-if="loadingSuggestions" class="search-input-icon search-loading form-control-feedback">
        <i class="fa fa-refresh fa-spin"></i>
      </span>
      <div class="input-group-addon search-submit"
        v-on:click.prevent="search(tmpQtext)">
        <i class="fa fa-search"></i>
      </div>
    </div>
    <transition name="fade" mode="out-in">
      <select v-if="suggestions.length" ref="suggestions" class="suggestions" :size="suggestions.length"
        v-on:click.prevent="focusToSuggestions()"
        v-on:keydown.enter.prevent="selectSuggestion($event.target.value)"
        v-on:keydown.esc.prevent="hideSuggestions()">
        <option v-for="(s, $index) in suggestions" :key="$index">{{ s }}</option>
      </select>
    </transition>
  </form>
</template>

<script>
export default {
  name: 'ml-input',
  props: {
    qtext: {
      type: String
    },
    search: {
      type: Function
    },
    suggest: {
      type: Function
    }
  },
  data() {
    return {
      tmpQtext: this.qtext || '',
      suggestions: [],
      loadingSuggestions: false
    };
  },
  methods: {
    clear() {
      console.log('Empty search');
      this.tmpQtext = '';
      this.hideSuggestions();
      if (this.$store.state.search) {
        this.search(this.tmpQtext);
      }
    },
    resolveSuggest(tmpQtext) {
      this.loadingSuggestions = true;
      this.suggest(tmpQtext).then(result => {
        this.suggestions = result;
        this.loadingSuggestions = false;
      });
    },
    selectSuggestion(s) {
      console.log('Selected suggestion ' + s);
      this.tmpQtext = s;
      this.resolveSuggest(this.tmpQtext);
      this.focusToInput();
    },
    focusToSuggestions() {
      if (this.suggestions.length) {
        console.log('Focus to suggestions');
        this.$refs.suggestions.focus();
      } else {
        this.resolveSuggest(this.tmpQtext);
      }
    },
    focusToInput() {
      console.log('Focus to input');
      this.$refs.input.focus();
    },
    hideSuggestions() {
      console.log('Hide suggestions');
      this.suggestions = [];
      this.focusToInput();
    }
  }
};
</script>

<style lang="less" scoped>
.ml-input {
  .input-group {
    width: 100%;
  }

  .search-submit {
    width: 40px !important;
  }

  .search-input-icon {
    z-index: 3;
    top: 0px;
  }

  .search-input-clear {
    right: 35px;
    cursor: pointer;
    cursor: hand;
    pointer-events: auto;
  }

  .search-loading {
    right: -35px;
  }

  .suggestions {
    left: 25px;
    top: 35px;
    position: absolute;
  }
}
</style>
