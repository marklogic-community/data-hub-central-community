<template>
  <div class="row detail">
    <div class="col-sm-8 content-col">
      <h3>{{ metadata.fileName }} ({{ metadata.contentType }})</h3>
      <div class="card">
        <b-tabs card no-fade v-model="tabIndex">
          <b-tab title="Details" active>
            <dl class="row">
              <dt v-show="metadata.collections" class="col-sm-2">Collections</dt>
              <dd v-show="metadata.collections" class="col-sm-10">
                <span v-for="(c, $index) in metadata.collections" :key="$index">
                  {{ c }}<span v-show="$index !== metadata.collections.length - 1">, </span>
                </span>
              </dd>

              <dt class="col-sm-2">Content-Type</dt>
              <dd class="col-sm-10">{{ metadata.contentType }}</dd>

              <dt class="col-sm-2">File Name</dt>
              <dd class="col-sm-10">{{ metadata.fileName }}</dd>

              <dt class="col-sm-2">Format</dt>
              <dd class="col-sm-10">{{ metadata.format }}</dd>

              <dt v-show="metadata.metadataValues" class="col-sm-2">Metadata</dt>
              <dd v-show="metadata.metadataValues" class="col-sm-10">
                <dl class="row">
                  <template v-for="(v, key, $index) in metadata.metadataValues">
                    <dt class="col-sm-2" :key="$index">{{ key }}</dt>
                    <dd class="col-sm-10" :key="$index">{{ v }}</dd>
                  </template>
                </dl>
              </dd>

              <dt v-show="metadata.permissions" class="col-sm-2">Permissions</dt>
              <dd v-show="metadata.permissions" class="col-sm-10">
                <span v-for="(p, $index) in metadata.permissions" :key="$index">
                  {{ p }}<span v-show="$index !== metadata.permissions.length - 1">, </span>
                </span>
              </dd>

              <dt class="col-sm-2">Quality</dt>
              <dd class="col-sm-10">{{ metadata.quality }}</dd>

              <dt class="col-sm-2">Size</dt>
              <dd class="col-sm-10">{{ metadata.size }} bytes</dd>

              <dt class="col-sm-2">Uri</dt>
              <dd class="col-sm-10">{{ metadata.uri }}</dd>
            </dl>
          </b-tab>
          <b-tab title="Preview">
            <div v-if="metadata.format === 'json' && json">
              <friendly-json :json="json"></friendly-json>
            </div>
            <div v-else-if="metadata.format === 'xml' && raw">
              <friendly-xml :xml="raw"></friendly-xml>
            </div>
            <div v-else>
              <view-binary :src="viewUri" :type="metadata.contentType" :title="metadata.fileName">
                <a slot="fallback" class="btn btn-default" :href="downloadUri" target="_blank" download>Download</a>
              </view-binary>
            </div>
          </b-tab>
          <b-tab title="Raw" v-show="raw">
            <pre>{{ raw }}</pre>
          </b-tab>
        </b-tabs>
      </div>
    </div>
    <div class="col-sm-4 right-col">
      <div id="buttons-detail">
        <router-link tag="button" class="btn btn-default"
                 :to="{ name: 'root.search' }">Search</router-link>
        <router-link tag="button" class="btn btn-primary"
                v-show="!profile.disallowUpdates"
                :to="{ name: 'root.edit', params: { id: id } }">Edit</router-link>
        <button class="btn btn-primary"
                v-show="!profile.disallowUpdates"
                v-on:click.prevent="deleteDoc()">Delete</button>
      </div>
      <br>
      <!-- show (links to) similar documents -->
      <div class="card similar">
        <div class="card-header">Similar</div>
        <div class="card-body">
          <ml-similar :uri="uri" limit="5"></ml-similar>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import mlSimilar from '@/components/ml-similar.vue';
import friendlyJson from '@/components/friendly-json.vue';
import friendlyXml from '@/components/friendly-xml.vue';

export default {
  name: 'DetailPage',
  components: {
    mlSimilar,
    friendlyJson,
    friendlyXml
  },
  props: ['type', 'id'],
  data() {
    const self = this;
    return {
      metadata: {},
      json: undefined,
      raw: undefined,
      tabIndex: 0
    };
  },
  computed: {
    uri() {
      return this.metadata.uri || '';
    },
    profile() {
      return this.$store.state.auth.profile || {};
    },
    viewUri() {
      return '/api/crud/' + this.type + '/' + this.id + '?';
    },
    downloadUri() {
      return this.viewUri + 'download=true';
    }
  },
  mounted() {
    this.update();
  },
  methods: {
    update() {
      const self = this;

      self.metadata = {};
      self.json = undefined;
      self.raw = undefined;

      this.$store
        .dispatch('crud/' + self.type + '/view', {
          id: self.id,
          view: 'metadata'
        })
        .then(function(response) {
          if (!response.isError) {
            var metadata = JSON.parse(response.response);
            var permissions = [];
            // flatten permissions for simplified display
            metadata.permissions.forEach(function(p) {
              p.capabilities.forEach(function(c) {
                permissions.push(p['role-name'] + ':' + c);
              });
            });
            metadata.permissions = permissions;
            if (metadata.collections.length === 0) {
              delete metadata.collections;
            }
            if (metadata.permissions.length === 0) {
              delete metadata.permissions;
            }
            if (
              metadata.metadataValues &&
              Object.keys(metadata.metadataValues).length === 0
            ) {
              delete metadata.metadataValues;
            }
            self.metadata = metadata;

            if (metadata.format === 'json') {
              self.$store
                .dispatch('crud/' + self.type + '/read', { id: self.id })
                .then(function(response) {
                  if (!response.isError) {
                    self.json = JSON.parse(response.response);
                    self.raw = JSON.stringify(self.json, null, 2);
                  }
                });
            } else if (metadata.format === 'xml') {
              self.$store
                .dispatch('crud/' + self.type + '/view', {
                  id: self.id,
                  view: 'indent'
                })
                .then(function(response) {
                  if (!response.isError) {
                    self.raw = response.response;
                  }
                });
            }
          }
        });
    },
    deleteDoc() {
      const self = this;
      if (
        window.confirm(
          'This will permanently delete ' +
            self.metadata.fileName +
            ', are you sure?'
        )
      ) {
        const toast = self.$parent.$refs.toast;
        self.$store
          .dispatch('crud/' + self.type + '/delete', { id: self.id })
          .then(function(response) {
            if (response.isError) {
              toast.showToast('Failed to delete the document', {
                theme: 'error'
              });
            } else {
              toast.showToast('Successfully deleted the document', {
                theme: 'success'
              });
              self.$router.push({
                name: self.previousRoute
                  ? self.previousRoute.name
                  : 'root.search',
                params: {
                  refresh: true,
                  ...(self.previousRoute ? self.previousRoute.params : {})
                }
              });
            }
          });
      }
    }
  },
  watch: {
    '$route.params': {
      handler(params) {
        if (params) {
          this.update();
        }
      },
      deep: true
    }
  }
}
</script>

<style lang="less" scoped>
view-binary {
  display: block;
  height: 600px;
}
</style>
