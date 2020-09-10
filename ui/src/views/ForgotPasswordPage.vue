<template>
	<v-container fluid fill-height class="forgot-password-page">
		<v-layout align-center justify-center>
			<v-flex xs12 sm8 md4>
				<v-card class="elevation-12">
					<v-card-title>Reset Password</v-card-title>
					<v-card-text v-if="requested">
						<div class="request-sent">
							Check your inbox for the next steps. If you don't receive an email, and it's not in your spam folder this could mean you signed up with a different address.
						</div>
					</v-card-text>
					<v-form v-else v-on:submit.prevent="resetPassword()">
						<v-card-text>
							<v-text-field
								autofocus
								outlined
								prepend-inner-icon="email"
								name="email"
								v-model="email"
								placeholder="Email"
								type="email"
								:error-messages="inputErrors('email', 'Email')"
								@blur="$v.email.$touch()"></v-text-field>
							<div class="buttons">
								<v-btn id="submit-btn" type="submit" color="primary">Reset Password</v-btn>
							</div>
						</v-card-text>
					</v-form>
					<v-card-actions>
						<router-link to="/login">Log in</router-link>
						<span>or</span>
						<router-link to="/signup">Sign Up</router-link>
					</v-card-actions>
				</v-card>
			</v-flex>
		</v-layout>
	</v-container>
</template>

<script>
import { required, email } from 'vuelidate/lib/validators'
import authApi from '../api/AuthApi';

export default {
  data() {
    return {
			email: '',
      requested: false
    }
  },
  methods: {
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('required') && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('email') && !this.$v[field].email && errors.push(`Must be an email address.`)
			return errors
		},
    resetPassword() {
			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			authApi.resetPassword(this.email)
        .then(() => {
          this.requested = true
        })
    }
	},
	validations: {
    email: {
			required,
			email
		}
  },
}
</script>
<style lang="less" scoped>
.forgot-password-page {
	background: linear-gradient(0deg, black, #555);
}
h2 {
	margin-bottom: 2em;
}
.v-card__actions {
	justify-content: center;
}

.buttons {
	margin-top: 1em;
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
		margin-left: 10px;
		margin-right: 10px;
	}
	background: #eee;
	border-bottom-right-radius: 4px;
	border-bottom-left-radius: 4px;
	min-height: 50px;
}

.request-sent {
	text-align: center;
	border: 1px solid #ddd;
	padding: 1em;
}
</style>
