<template>
<div class="background">
<v-app-bar dark color="#2B333C" >
          <img src="@/assets/images/MarkLogic-avatar.svg" height="60%"/>
       <div class="vertical"></div>
          <img src="@/assets/images/hub-central-community-edition.png" height="60%"/>
          <v-spacer></v-spacer>
       <div>
					<a target="_blank" href="https://marklogic-community.github.io/data-hub-central-community/" class="docsLink">
							<v-icon>help_outline</v-icon>
          </a>
       </div>
  </v-app-bar>
	<v-container fluid fill-height >
		<v-layout align-center justify-center class="loginContainer">
			<v-flex xs12 sm8 md4>
				<v-card class="elevation-12">
					<v-card-title></v-card-title>
					<v-form v-on:submit.prevent="isLoggedIn ? doLogout() : doLogin()">
						<v-card-text>
							<v-alert type="error" v-show="hasLoginError" v-cloak>Username and/or Password Incorrect</v-alert>
							<v-alert type="success" v-show="hasLoginSuccess" v-cloak>You successfully logged in</v-alert>
							<v-alert type="error" v-show="hasLogoutError" v-cloak>Logout failed</v-alert>
							<v-alert type="success" v-show="hasLogoutSuccess" v-cloak>You successfully logged out</v-alert>
							<v-text-field autofocus outlined prepend-inner-icon="person" name="login" v-model="user" :label="isHosted ? 'Enter email address' : 'Enter username'" type="text"></v-text-field>
							<v-text-field outlined prepend-inner-icon="lock" name="password" label="Enter password" v-model="pass" type="password"></v-text-field>
							<div class="buttons">
								<v-btn id="submit-btn" type="submit" :disabled="pending" color="primary">LogIn</v-btn>
							</div>
						</v-card-text>
						<v-card-actions v-if="isHosted">
							<span>New Here?</span><router-link to="/signup">Sign Up</router-link>
						</v-card-actions>
					</v-form>
				</v-card>
				<div v-if="isHosted" class="other-actions">
					<router-link to="/forgotPassword">Forgot Your Password?</router-link>
				</div>
			</v-flex>
		</v-layout>
	</v-container>
	<div class="mainFooter">
          <span>© {{new Date().getUTCFullYear()}} MarkLogic Corporation</span>
          |
          <span>
            <a href="https://www.marklogic.com/privacy/" class="linkStyle">Privacy</a>
          </span>
  </div>
	</div>
</template>

<script>
import { mapState } from 'vuex'

export default {
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
		},
		...mapState({
			isHosted: state => state.isHosted
		}),
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

<style lang="less" scoped>

h2 {
	margin-bottom: 2em;
}

.v-card__actions {
	justify-content: center;
}

.buttons {
	text-align: center;
}
#submit-btn {
	min-width: 300px;
	min-height: 50px;
}
/deep/ .v-card__title {
	justify-content: center;
}
/deep/ .v-card__actions {
	span {
		margin-right: 10px;
	}
	background: #eee;
	border-bottom-right-radius: 4px;
	border-bottom-left-radius: 4px;
	min-height: 50px;
}

.other-actions {
	margin-top: 1em;
	text-align: center;
	a {
		color: white;
	}
}

.background {
   width: 100%;
   height: 100%;
   background-image: url("../assets/images/background-fast-lines.jpg");
   background-position: center center;
   background-size: cover;
   background-repeat: no-repeat;
   position:fixed;
}

.loginContainer {
   width: 100%;
   margin-top: -200px;
}

.docsLink{
  text-decoration:none;
}

.vertical{
   display: inline-block;
   position: relative;
   top: 3px;
   border-left: dotted 1px rgba(255, 255, 255, 0.65);
   height: 24px;
   margin: -6px 16px;
   cursor: default;
}

 .page-title {
   margin-left: 10px;
}

.mainFooter {
		text-align: center;
    margin-top: -110px;
    color: rgb(255, 255, 255);
    font-size: 14px;
    font-weight: 500;
}

.linkStyle {
  color: rgb(255, 255, 255);
  text-decoration: none;
}

.linkStyle:hover{
  color: #7fade3;
 }

</style>
