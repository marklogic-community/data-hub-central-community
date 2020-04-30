<template>
  <div class="simple-view-file">
    <div class="loading" v-show="loading">
      Loading... <i class="fa fa-spinner fa-spin"></i>
    </div>
    <div class="viewer" v-show="!loading">
      <audio v-if="(fileType === 'audio') && uri" controls preload="metadata" :src="uri" :title="fileName">
        <a v-show="downloadUri" class="btn btn-default" :href="downloadUri" target="_blank" download>Download</a>
        <div v-show="!downloadUri" class="alert alert-warning">Alert: cannnot display this file!</div>
      </audio>
      <video v-else-if="(fileType === 'video') && uri" controls preload="metadata" :src="uri" :title="fileName" playsinline>
        <a v-show="downloadUri" class="btn btn-default" :href="downloadUri" target="_blank" download>Download</a>
        <div v-show="!downloadUri" class="alert alert-warning">Alert: cannnot display this file!</div>
      </video>
      <picture v-else-if="(fileType === 'image') && uri" :title="fileName">
        <source :srcset="uri">
        <a v-show="downloadUri" class="btn btn-default" :href="downloadUri" target="_blank" download>Download</a>
        <div v-show="!downloadUri" class="alert alert-warning">Alert: cannnot display this file!</div>
      </picture>
      <div v-else-if="((fileType === 'html') || (fileType === 'text')) && content" v-highlight>
        <pre>
          <code class="html">{{ content }}</code>
        </pre>
      </div>
      <tree-view v-else-if="fileType === 'json' && content" :data="content" :options="{ maxDepth: 1 }"></tree-view>
      <div v-else-if="(fileType === 'xml') && content" v-highlight>
        <pre>
          <code class="xml">{{ content }}</code>
        </pre>
      </div>
      <object v-if="((fileType === 'other') || (fileType === 'pdf')) && uri" :data="uri" :type="contentType" :title="fileName">
        <a v-show="downloadUri" class="btn btn-default" :href="downloadUri" target="_blank" download>Download</a>
        <div v-show="!downloadUri" class="alert alert-warning">Alert: cannnot display this file!</div>
      </object>
    </div>
  </div>
</template>

<script>
import Vue from 'vue';
import Highlight from 'vue-hljs';

import 'vue-hljs/dist/vue-hljs.min.css';
import 'highlight.js/styles/github.css';

// attribute directives like Highlight require different registration
Vue.use(Highlight);

export default {
  name: 'simple-view-file',
  components: {},
  props: {
    uri: {
      type: String,
      required: true
    },
    contentType: {
      type: String,
      required: true
    },
    downloadUri: {
      type: String
    },
    fileName: {
      type: String,
      default() {
        return this.uri.split('/').pop();
      }
    }
  },
  data() {
    return {
      loading: false,
      content: undefined
    };
  },
  computed: {
    fileType() {
      var type = 'other';
      if (/[+/](html|json|pdf|xhtml|xml)$/.test(this.contentType)) {
        type = this.contentType
          .split('/')
          .pop()
          .replace('xhmtml', 'html');
      } else if (/^(audio|image|text|video|xml)\//.test(this.contentType)) {
        type = this.contentType.split('/')[0];
      } else if (/^application\//.test(this.contentType)) {
        // TODO
      }
      return type;
    }
  },
  methods: {
    updateContent() {
      this.loading = true;
      this.$http({
        method: 'GET',
        url: this.uri,
        auth: {
          username: this.$store.state.auth.username,
          password: this.$store.state.auth.password,
          sendImmediately: true
        }
      }).then(
        response => {
          this.content = response.data;
          this.loading = false;
        },
        error => {
          console.log(error);
          this.loading = false;
        }
      );
    },
    error: function(err) {
      console.log(err);
    }
  },
  watch: {
    uri(newUri) {
      if (newUri) {
        this.updateContent();
      }
    }
  },
  mounted() {
    this.updateContent();
  }
};
</script>

<style lang="less" scoped>
// .view-file {
//   padding: 0;

//   .viewer-wrapper {
//     height: 600px;

//     .loading {
//       position: absolute;
//     }

//     .viewer {
//       position: absolute;
//       width: 100%;
//       background-color: inherit;
//       padding: 0;
//       overflow: hidden;
//       max-height: 600px;

//       hljs pre, .hljs pre {
//         overflow: scroll;
//         max-height: 600px;
//       }
//       view-object {
//         object {
//           height: 575px;
//           width: 100%;
//         }
//       }
//       videogular.audio {
//         height: 50px;
//       }
//     }
//   }

//   .source {
//     display: block;
//     padding: 9.5px;
//     margin: 0;
//     font-size: 13px;
//     line-height: 1.42857143;
//     word-break: break-all;
//     word-wrap: break-word;
//     color: #333333;
//     background-color: #f5f5f5;
//     border: 1px solid #ccc;
//     border-radius: 4px;
//     overflow: scroll;
//     max-height: 600px;
//   }
// }

// .view-file.audio .viewer-wrapper {
//   height: 71px;
// }

// .modal-body {
//   .viewer-wrapper {
//     padding: 0;
//   }
// }
</style>
