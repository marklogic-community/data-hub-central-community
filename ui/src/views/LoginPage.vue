<template>
	<v-container fluid fill-height>
		<v-layout align-center justify-center>
			<v-flex xs12 sm8 md4>
				<v-card dark class="elevation-12">
					<v-form v-on:submit.prevent="isLoggedIn ? doLogout() : doLogin()">
						<v-card-text>
							<v-alert type="error" v-show="hasLoginError" v-cloak>Username and/or Password Incorrect</v-alert>
							<v-alert type="success" v-show="hasLoginSuccess" v-cloak>You successfully logged in</v-alert>
							<v-alert type="error" v-show="hasLogoutError" v-cloak>Logout failed</v-alert>
							<v-alert type="success" v-show="hasLogoutSuccess" v-cloak>You successfully logged out</v-alert>
							<v-text-field autofocus prepend-icon="person" name="login" v-model="user" label="Login" type="text"></v-text-field>
							<v-text-field prepend-icon="lock" name="password" label="Password" v-model="pass" type="password"></v-text-field>
						</v-card-text>
						<v-card-actions>
							<v-spacer></v-spacer>
							<v-btn type="submit" color="primary">Login</v-btn>
						</v-card-actions>
					</v-form>
				</v-card>
			</v-flex>
		</v-layout>
	</v-container>
</template>

<script>
export default {
  name: 'LoginPage',
  beforeRouteUpdate(to, from, next) {
    this.user = '';
    this.pass = '';
    this.pending = false;
    this.hasLoginSuccess = false;
    this.hasLoginError = false;
    this.hasLogoutSuccess = false;
    this.hasLogoutError = false;
    next();
  },
  data() {
    return {
      user: '',
      pass: '',
      pending: false,
      hasLoginSuccess: false,
      hasLoginError: false,
      hasLogoutSuccess: false,
      hasLogoutError: false
    };
  },
  computed: {
    isLoggedIn() {
      return this.$store.state.auth.authenticated;
    },
    showCancel() {
      return this.$route.params && this.$route.params.state;
    }
  },
  methods: {
    doLogin() {
      this.pending = true;

      this.hasLoginSuccess = false;
      this.hasLoginError = false;
      this.hasLogoutSuccess = false;
      this.hasLogoutError = false;

      this.$store
        .dispatch('auth/login', {
          user: this.user,
          pass: this.pass
        })
        .then((error) => {
          this.pending = false;
          if (error) {
            this.hasLoginError = true;
          } else {
						this.hasLoginSuccess = true;
						this.$router.push({ name: 'root.landing' });
          }
        });
    },
    doLogout() {
      this.pending = true;

      this.hasLoginSuccess = false;
      this.hasLoginError = false;
      this.hasLogoutSuccess = false;
      this.hasLogoutError = false;

      this.$store.dispatch('auth/logout').then((error) => {
        this.pending = false;
        if (error) {
          this.hasLogoutError = true;
        } else {
          this.hasLogoutSuccess = true;
        }
      });
    }
  }
};
</script>
