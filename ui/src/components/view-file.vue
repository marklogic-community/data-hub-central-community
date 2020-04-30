<template>
  <div class="view-file container-fluid" :class="fileType">
    <div class="row">
      <div class="controls col-sm-1 text-right" v-if="controls && (((fileType === 'xml') || (fileType === 'json') || (fileType === 'html') || (fileType === 'text')) || allowModal || downloadUri)">
        <div class="code-control">
          <a v-if="(fileType === 'xml') || (fileType === 'json') || (fileType === 'html') || (fileType === 'text')" class="btn btn-default" v-on:click.prevent="toggleCode()">
            <span v-show="!showCode_ && ((fileType === 'json') || (fileType === 'text'))">{ }</span>
            <i v-show="!showCode_ && ((fileType === 'html') || (fileType === 'xml'))" class="fa fa-code"></i>
            <i v-show="showCode_" class="fa fa-align-left"></i>
          </a>
        </div>

        <div class="modal-control">
          <a v-if="allowModal && fileType !== 'audio'" class="btn btn-default" v-on:click.prevent="showModal()"><i class="glyphicon glyphicon-resize-full"></i></a>
        </div>

        <div class="download-control">
          <a v-if="downloadUri" class="btn btn-default" :href="downloadUri" target="_blank" download><i class="glyphicon glyphicon-download-alt"></i></a>
        </div>
      </div>

      <div class="viewer-wrapper" :class="{'col-sm-11': controls, 'col-sm-12': !controls}">
        <div class="loading" v-show="loading">
          Loading... <i class="fa fa-spinner fa-spin"></i>
        </div>

        <div class="viewer" v-show="!loading">
          <!-- audio / video -->
          <div class="source" v-if="(fileType === 'audio' || fileType === 'video') && uri">
            <!--videogular :class="fileType" v-if="uri">
              <vg-media vg-src="uri"></vg-media>
              <vg-controls>
                <vg-play-pause-button></vg-play-pause-button>
                <vg-time-display>{{ currentTime | date:'mm:ss' }}</vg-time-display>
                <vg-scrub-bar>
                  <vg-scrub-bar-current-time></vg-scrub-bar-current-time>
                </vg-scrub-bar>
                <vg-time-display>{{ timeLeft | date:'mm:ss' }}</vg-time-display>
                <vg-volume>
                  <vg-mute-button></vg-mute-button>
                  <vg-volume-bar></vg-volume-bar>
                </vg-volume>
                <vg-fullscreen-button ng-show="fileType === 'video'"></vg-fullscreen-button>
              </vg-controls>
            </videogular-->
          </div>
          <div v-if="(fileType === 'audio' || fileType === 'video') && !uri" class="alert alert-warning">
            Data view not supported for audio and video
          </div>

          <!-- html / text -->
          <div v-if="(fileType === 'html') || (fileType === 'text')">
            <div v-if="!showCode_ && content">
              <div class="source" v-html="content"></div>
            </div>
            <div v-if="showCode_ && content">
              <div v-highlight>
                <pre>
                  <code class="html">{{ content }}</code>
                </pre>
              </div>
            </div>
          </div>

          <!-- image -->
          <div class="source text-center" v-if="fileType === 'image' && uri">
            <img :src="uri">
          </div>
          <div class="alert alert-warning" v-if="fileType === 'image' && !uri">
            Data view not supported for images
          </div>

          <!-- json -->
          <div v-if="fileType === 'json'">
            <div v-if="!showCode_ && content">
              <friendly-json class="source" :json="content"></friendly-json>
            </div>
            <div v-if="showCode_ && content">
              <!--tree-view :data="content" :options="{ maxDepth: 1 }"></tree-view-->
            </div>
          </div>

          <!-- xml -->
          <div v-if="fileType === 'xml'">
            <div v-if="!showCode_ && content">
              <friendly-xml class="source" :xml="content"></friendly-xml>
            </div>
            <div v-if="showCode_ && content">
              <div v-highlight>
                <pre>
                  <code class="xml">{{ content }}</code>
                </pre>
              </div>
            </div>
          </div>

          <!-- pdf -->
          <div class="source" v-if="fileType === 'pdf'">
            <input v-model.number="page" type="number" min="1" :max="numPages" style="width: 5em"> / {{numPages}}
            <button class="btn btn-default btn-sm" v-on:click.prevent="rotate -= 90">&#x27F2;</button>
            <button class="btn btn-default btn-sm" v-on:click.prevent="rotate += 90">&#x27F3;</button>
            <pdf v-if="uri" :src="uri" :page="page" :rotate="rotate" @progress="loading = 1 > $event" @error="error" @num-pages="numPages = $event"></pdf>
            <pdf v-if="!uri" :data="data" :page="page" :rotate="rotate" @progress="loading = 1 > $event" @error="error" @num-pages="numPages = $event"></pdf>
          </div>

          <!-- other -->
          <object class="source" v-if="fileType === 'other' && uri" :data="uri" :type="contentType">
            <a v-show="downloadUri" class="btn btn-default" :href="downloadUri" target="_blank" download>Download</a>
            <div v-show="!downloadUri" class="alert alert-warning">Alert: cannnot display this file!</div>
          </object>
          <div class="alert alert-warning" v-if="fileType === 'other' && !uri">
            Data view not supported for binaries
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import Vue from 'vue';
import Highlight from 'vue-hljs';
import Pdf from 'vue-pdf';
import TreeView from 'vue-json-tree-view';

import friendlyJson from '@/components/friendly-json.vue';
import friendlyXml from '@/components/friendly-xml.vue';

import 'vue-hljs/dist/vue-hljs.min.css';
import 'highlight.js/styles/github.css';

// attribute directives like Highlight require different registration
Vue.use(Highlight);

export default {
  name: 'view-file',
  components: {
    friendlyJson,
    friendlyXml,
    Pdf,
    TreeView
  },
  props: {
    uri: {
      type: String,
      required: true
    },
    contentType: {
      type: String,
      required: true
    },
    allowModal: {
      type: Boolean,
      default() {
        return true;
      }
    },
    controls: {
      type: Boolean,
      default() {
        return this.allowModal || !!this.downloadUri;
      }
    },
    downloadUri: {
      type: String
    },
    fileName: {
      type: String,
      default() {
        return this.uri.split('/').pop();
      }
    },
    showCode: {
      type: Boolean,
      default() {
        return false;
      }
    },
    trustUri: {
      type: Boolean,
      default() {
        return false;
      }
    }
  },
  data() {
    return {
      loading: false,
      content: undefined,
      showCode_: this.showCode,
      page: 1,
      numPages: 0,
      rotate: 0
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
    toggleCode() {
      this.showCode_ = !this.showCode_;
    },
    showModal() {
      // TODO
      console.log('view-file: showModal not implemented yet!');
    },
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
.view-file {
  padding: 0;

  .viewer-wrapper {
    height: 600px;

    .loading {
      position: absolute;
    }

    .viewer {
      position: absolute;
      width: 100%;
      background-color: inherit;
      padding: 0;
      overflow: hidden;
      max-height: 600px;

      hljs pre,
      .hljs pre {
        overflow: scroll;
        max-height: 600px;
      }
      view-object {
        object {
          height: 575px;
          width: 100%;
        }
      }
      videogular.audio {
        height: 50px;
      }
    }
  }

  .source {
    display: block;
    padding: 9.5px;
    margin: 0;
    font-size: 13px;
    line-height: 1.42857143;
    word-break: break-all;
    word-wrap: break-word;
    color: #333333;
    background-color: #f5f5f5;
    border: 1px solid #ccc;
    border-radius: 4px;
    overflow: scroll;
    max-height: 600px;
  }
}

.view-file.audio .viewer-wrapper {
  height: 71px;
}

.modal-body {
  .viewer-wrapper {
    padding: 0;
  }
}
</style>
