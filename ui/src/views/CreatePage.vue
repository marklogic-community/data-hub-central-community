<template>
  <div class="create">
    <div class="row" v-if="!profile.disallowUpdates">
      <div class="col-sm-12">
        <b-alert variant="warning">Not allowed</b-alert>
      </div>
    </div>
    <div class="row" v-if="profile.disallowUpdates">
      <div class="col-sm-12">
        <b-alert variant="warning">Updates are disallowed</b-alert>
      </div>
    </div>
    <div v-if="!profile.disallowUpdates">
      <div class="row">
        <div class="col-sm-2">
        </div>
        <h2 class="col-sm-10" v-if="mode === 'create'">Create a Document</h2>
        <h2 class="col-sm-10" v-if="mode === 'edit'">Edit "{{ id }}"</h2>
      </div>
      <form class="form-horizontal">
        <div class="form-group">
          <div class="col-sm-8 col-sm-offset-2">
            <strong class="required alert-danger"><em>required</em></strong>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label required">Name</label>
          <div class="col-sm-10">
            <input type="text" class="form-control" placeholder="person's name" v-model.trim="person.name"
               name="name" minlength="3" maxlength="100" v-on:input="$v.person.name.$touch">
          </div>
        </div>
        <div class="row form-error" v-if="$v.person.name.$invalid">
          <div class="col-md-10 col-md-offset-2 has-error has-feedback" >
            <div>
              <p class="alert alert-danger" v-if="!$v.person.name.required">Person's name is required.</p>
              <p class="alert alert-danger" v-if="!$v.person.name.minLength">Minimum 3 characters.</p>
              <p class="alert alert-danger" v-if="!$v.person.name.maxLength">Maximum of 100 characters.</p>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">About</label>
          <div class="col-sm-10">
            <textarea rows="8" class="form-control" v-model="person.about"/>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Greeting</label>
          <div class="col-sm-10">
            <input type="text" class="form-control" v-model="person.greeting" placeholder="How this person should be greeted on login">
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Balance</label>
          <div class="col-sm-10">
            <input type="text" class="form-control" v-model="person.balance" placeholder="">
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Picture</label>
          <div class="col-sm-10">
            <input type="text" class="form-control" v-model="person.picture" placeholder="">
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Age</label>
          <div class="col-sm-10">
            <input type="number" class="form-control" v-model="person.age" placeholder="" name="age" v-on:input="$v.person.age.$touch">
          </div>
        </div>
        <div class="row form-error" v-if="$v.person.age.$invalid">
          <div class="col-md-10 col-md-offset-2 has-error has-feedback" >
            <div>
              <p class="alert alert-danger" v-if="!$v.person.age.numeric">Invalid age format.</p>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label required">Eye Color</label>
          <div class="col-sm-10">
            <input type="text" class="form-control" v-model="person.eyeColor" placeholder="" name="eyeColor"  v-on:input="$v.person.eyeColor.$touch"/>
          </div>
        </div>
        <div class="row form-error" v-if="$v.person.eyeColor.$invalid">
          <div class="col-md-10 col-md-offset-2 has-error has-feedback" >
            <div>
              <p class="alert alert-danger" v-if="!$v.person.eyeColor.required">Eye Color is required.</p>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label required">Gender</label>
          <div class="col-sm-1">
            <label class="radio-inline"><input type="radio" value="female" v-model="person.gender"
              placeholder="" name="gender" v-on:input="$v.person.gender.$touch">Female</label>
          </div>
          <div class="col-sm-1">
            <label class="radio-inline"><input type="radio" value="male" v-model="person.gender"
              placeholder="" name="gender" v-on:input="$v.person.gender.$touch">Male</label>
          </div>
        </div>
        <div class="row form-error" v-if="$v.person.gender.$invalid">
          <div class="col-md-10 col-md-offset-2 has-error has-feedback" >
            <div>
              <p class="alert alert-danger" v-if="!$v.person.gender.required">Gender is required.</p>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Company</label>
          <div class="col-sm-10">
            <input type="text" class="form-control" v-model="person.company" placeholder="">
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Email</label>
          <div class="col-sm-10">
            <input type="email" class="form-control" v-model="person.email" placeholder="" name="email" v-on:input="$v.person.email.$touch">
          </div>
        </div>
        <div class="row form-error" ng-if="$v.person.email.$invalid">
          <div class="col-md-10 col-md-offset-2 has-error has-feedback" >
            <div>
              <p class="alert alert-danger" v-if="!$v.person.email.email">Invalid email format.</p>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Phone</label>
          <div class="col-sm-10">
            <input type="text" class="form-control" v-model="person.phone" placeholder="">
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Address</label>
          <div class="col-sm-10">
            <input type="text" class="form-control" v-model="person.address" placeholder="">
          </div>
        </div>
        <div class="form-group" v-if="person.location">
          <label class="col-sm-2 control-label">Location</label>
          <div class="col-sm-5">
            <input type="number" class="form-control" v-model="person.location.latitude" placeholder=""
              min="-90" max="90" name="latitude" v-on:input="$v.person.location.latitude.$touch">
          </div>
          <div class="col-sm-5">
            <input type="number" class="form-control" v-model="person.location.longitude" placeholder=""
               min="-180" max="180" name="longitude" v-on:input="$v.person.location.longitude.$touch">
          </div>
        </div>
        <div class="row form-error" v-if="$v.person.location.latitude.$invalid">
          <div class="col-md-10 col-md-offset-2 has-error has-feedback" >
            <div>
              <p class="alert alert-danger" v-if="!$v.person.location.latitude.required">Latitude is required if longitude is filled.</p>
              <p class="alert alert-danger" v-if="!$v.person.location.latitude.minValue">Minimum value for latitude is -90.</p>
              <p class="alert alert-danger" v-if="!$v.person.location.latitude.maxValue">Maximum value for latitude is 90.</p>
            </div>
          </div>
        </div>
        <div class="row form-error" ng-if="$v.person.location.longitude.$invalid">
          <div class="col-md-10 col-md-offset-2 has-error has-feedback" >
              <p class="alert alert-danger" v-if="!$v.person.location.longitude.required">Longitude is required if latitude is filled.</p>
              <p class="alert alert-danger" v-if="!$v.person.location.longitude.minValue">Minimum value for longitude is -180.</p>
              <p class="alert alert-danger" v-if="!$v.person.location.longitude.maxValue">Maximum value for longitude is 180.</p>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Tags</label>
          <div class="col-sm-9">
            <input type="text" class="form-control" v-model="newTag">
            <div class="tag btn btn-info" v-for="(tag, $index) in person.tags" :key="$index">
              <span>{{tag}}</span>
              <span class="glyphicon glyphicon-remove-circle" v-on:click.prevent="removeTag(index)"></span>
            </div>
          </div>
          <div class="col-sm-1 text-right">
            <button class="add-feature btn btn-info" v-on:click.prevent="addTag()">Add</button>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">Active</label>
          <div class="col-sm-10">
            <input type="checkbox" v-model="person.isActive"/>
          </div>
        </div>
        <!--div class="form-group">
          <label class="col-sm-2 control-label required">Document Format</label>
          <div class="col-sm-1">
            <label class="radio-inline"><input type="radio" name="docFormat" value="json" v-model="person.docFormat" placeholder=""
              :disabled="mode === 'edit'" v-on:input="$v.person.docFormat.$touch">JSON</label>
          </div>
          <div class="col-sm-9">
            <label class="radio-inline"><input type="radio" name="docFormat" value="xml" v-model="person.docFormat" placeholder=""
              :disabled="mode === 'edit'" v-on:input="$v.person.docFormat.$touch">XML</label>
          </div>
        </div-->
        <div class="row form-error" ng-if="$v.person.docFormat.$invalid">
          <div class="col-md-10 col-md-offset-2 has-error has-feedback" >
            <div>
              <p class="alert alert-danger" v-if="!$v.person.docFormat.required">Document Format is required.</p>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-md-12 text-right">
            <router-link to="/" class="btn btn-default">Cancel</router-link>
            <button class="btn btn-primary" v-on:click.prevent="submit()">Submit</button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import * as X2JS from 'x2js';
import {
  required,
  minLength,
  maxLength,
  minValue,
  maxValue,
  numeric,
  email
} from 'vuelidate/lib/validators';
import crudApi from '@/api/CRUDApi.js';

const x2js = new X2JS();

export default {
  name: 'CreatePage',
  props: ['type', 'id'],
  data() {
    if (this.id) {
      crudApi.read(this.type, this.id).then(response => {
        this.person = JSON.parse(response.response);
      });
    }
    return {
      person: this.initPerson(),
      newTag: null
    };
  },
  computed: {
    profile() {
      return this.$store.state.auth.profile || {};
    },
    mode() {
      if (this.id) {
        return 'edit';
      } else {
        return 'create';
      }
    }
  },
  validations: {
    person: {
      name: {
        required,
        minLength: minLength(3),
        maxLength: maxLength(100)
      },
      age: {
        numeric
      },
      eyeColor: {
        required
      },
      gender: {
        required
      },
      email: {
        email
      },
      location: {
        latitude: {
          required,
          minValue: minValue(-90),
          maxValue: maxValue(90)
        },
        longitude: {
          required,
          minValue: minValue(-180),
          maxValue: maxValue(180)
        }
      },
      docFormat: {
        required
      }
    }
  },
  methods: {
    initPerson() {
      if (this.id) {
        return this.$store
          .dispatch('crud/' + this.type + '/view', {
            id: this.id,
            view: 'metadata'
          })
          .then(result => {
            if (!result.isError) {
              var metadata = JSON.parse(result.response);
              return this.$store
                .dispatch('crud/' + this.type + '/read', { id: this.id })
                .then(result => {
                  if (!result.isError) {
                    var doc = result.response;
                    var person = null;
                    if (metadata.format === 'json') {
                      person = JSON.parse(doc);
                    } else {
                      person = x2js.xml2js(doc);
                      if (person.xml) {
                        person = person.xml;
                      }
                    }
                    if (!person.tags) {
                      person.tags = [];
                    }
                    if (!person.location) {
                      person.location = {
                        latitude: 0,
                        longitude: 0
                      };
                    }
                  } else {
                    // error
                    return null;
                  }
                });
            } else {
              // error
              return null;
            }
          });
      } else {
        return {
          name: null,
          about: null,
          greeting: null,
          balance: 0,
          picture: 'http://placehold.it/32x32',
          age: 0,
          eyeColor: null,
          gender: null,
          company: null,
          email: null,
          phone: null,
          address: null,
          location: {
            latitude: 0,
            longitude: 0
          },
          tags: [],
          isActive: true,
          docFormat: 'json'
        };
      }
    },
    addTag() {
      if (
        this.newTag &&
        this.newTag !== '' &&
        this.person.tags.indexOf(this.newTag) < 0
      ) {
        this.person.tags.push(this.newTag);
      }
      this.newTag = null;
    },
    removeTag(index) {
      this.person.tags.splice(index, 1);
    },
    submit() {
      if (this.$v.$invalid) {
        return;
      }
      const toast = this.$parent.$refs.toast;
      //var extension = '.json';
      var data = this.person;
      // if (this.person.docFormat === 'xml') {
      //   extension = '.xml';
      //   var wrap = {
      //     xml: this.person
      //   };
      //   data = x2js.js2xml(wrap);
      // }

     
      if (this.mode === 'create') {

        return this.$store
          .dispatch('crud/' + this.type + '/create', {
            data,
            format: this.person.docFormat
          }) 
          .then(response => {
            if (response.isError) {
              toast.showToast(response.error, { theme: 'error' });
            } else {
              toast.showToast('Created', { theme: 'success' });
              this.$router.push({
                name: 'root.view',
                params: { id: response.id }
              });
            }
          });
          
      } else {
        // use update when in update mode
        return this.$store
          .dispatch('crud/' + this.type + '/update', {
            id: this.id,
            data,
            format: this.person.docFormat
          })
          .then(response => {
            if (response.isError) {
              toast.showToast(response.error, { theme: 'error' });
            } else {
              toast.showToast('Saved', { theme: 'success' });
              this.$router.push({ name: 'root.view', params: { id: this.id } });
            }
          });
      }
    }
  }
};
</script>
