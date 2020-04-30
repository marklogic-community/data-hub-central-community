<template>
  <div v-if="authenticated" class="profile">
    <div class="row">
      <div class="col-md-2"></div>
      <h2 class="col-md-10">Edit profile of {{ username }}</h2>
    </div>
    <form class="form-horizontal" name="profileForm">
      <div class="form-group">
        <div class="row">
          <div class="col-md-2">
            <label class="control-label">Full name</label>
          </div>
          <div class="col-md-2">
            <input type="text" class="form-control" placeholder="Full name of user" v-model.trim="tmpProfile.fullname">
          </div>
        </div>
      </div>
      <div class="form-group" v-bind:class="{ 'has-error': $v.tmpProfile.$invalid }">
        <div class="row">
          <div class="col-md-2">
            <label class="control-label">E-mail(s)</label>
          </div>
          <div class="col-md-1">
            <button class="btn btn-primary" v-on:click.prevent="addEmail()">
              <span class="glyphicon glyphicon-plus"></span>
            </button>
          </div>
        </div>
      </div>
      <div class="form-group">
        <!-- always show add button -->
        <!-- repeat if there are emails -->
        <div class="row" v-if="tmpProfile.emails" v-for="(email, $index) in tmpProfile.emails" :key="$index">
          <div class="col-md-offset-2 col-md-6">
            <div class="input-group">
              <input type="text" class="form-control" placeholder="e-mail of user" v-model.trim="tmpProfile.emails[$index]" v-on:input="$v.tmpProfile.emails.$each[$index].$touch"/>
              <span class="input-group-addon btn-danger" v-on:click.prevent="removeEmail($index)">
                <span class="glyphicon glyphicon-remove"></span>
              </span>
            </div>
            <div class="row" v-if="$v.tmpProfile.emails.$each[$index].$error">
              <div class="col-md-8 error text-danger">Not valid email!</div>
            </div>
          </div>
        </div>
      </div>
      <div class="row">
        <div class="col-md-offset-7 col-md-5">
          <router-link :to="{name: previousRoute.name, params: previousRoute.params}" class="btn btn-default">Cancel</router-link>
          <button class="btn btn-primary" v-on:click.prevent="submit()">Submit</button>
        </div>
      </div>
    </form>
  </div>
</template>

<script>
import { required, email } from 'vuelidate/lib/validators';

export default {
  name: 'ProfilePage',
  data() {
    return {
      tmpProfile: this.initProfile(this.$store.state.auth.profile)
    };
  },
  validations: {
    tmpProfile: {
      emails: {
        $each: {
          required,
          email
        }
      }
    }
  },
  computed: {
    previousRoute() {
      return this.$store.state.route.from;
    },
    authenticated() {
      return this.$store.state.auth.authenticated;
    },
    username() {
      return this.$store.state.auth.username;
    }
  },
  methods: {
    initProfile(profile) {
      var tmpProfile = JSON.parse(JSON.stringify(profile || {}));
      if (!tmpProfile.emails) {
        tmpProfile.emails = [];
      }
      return tmpProfile;
    },
    addEmail() {
      this.tmpProfile.emails.push('');
    },
    removeEmail(index) {
      this.tmpProfile.emails.splice(index, 1);
    },
    submit() {
      var self = this;
      if (self.$v.$invalid) {
        return;
      }
      const toast = self.$parent.$refs.toast;
      self.$store.dispatch('auth/update', self.tmpProfile).then(error => {
        if (error) {
          toast.showToast('Failed to update the user profile', {
            theme: 'error'
          });
        } else {
          toast.showToast('Successfully updated the user profile', {
            theme: 'success'
          });
          self.$router.push({
            name: self.previousRoute.name,
            params: self.previousRoute.params
          });
        }
      });
    }
  }
};
</script>
