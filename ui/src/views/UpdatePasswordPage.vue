<template>
	<v-container fluid fill-height class="update-password-page">
		<v-layout align-center justify-center>
			<v-flex xs12 sm8 md4>
				<v-card class="elevation-12">
					<v-card-text v-if="passwordUpdated" class="text-center">
						<h1>Your password has been updated</h1>
					</v-card-text>
					<v-form v-else v-on:submit.prevent="updatePassword()">
						<v-card-title>New Password</v-card-title>
						<v-card-text>
							<v-alert dense color="error" v-if="error">{{error}}</v-alert>
							<template v-else>
								<v-text-field
									autofocus
									outlined
									prepend-inner-icon="lock"
									name="password"
									placeholder="New Password"
									v-model="password"
									type="password"
									:error-messages="inputErrors('password', 'New Password')"
									@blur="$v.password.$touch()"></v-text-field>
								<div class="buttons">
									<v-btn id="submit-btn" type="submit" :disabled="submitting" color="primary">Update Password</v-btn>
								</div>
							</template>
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
import { required, minLength } from 'vuelidate/lib/validators'
import authApi from '../api/AuthApi';

export default {
  data() {
    return {
			password: '',
			error: null,
			passwordUpdated: false,
			submitting: false
    }
	},
	computed: {
		token() {
			return this.$route.query.token
		}
	},
	mounted() {
		authApi.validateResetToken(this.token).then(resp => {
			if (!resp.valid) {
				this.error = resp.error
			}
		})
	},
  methods: {
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('required') && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('minLength') && !this.$v[field].minLength && errors.push(`${fieldName} must be at least 8 characters long.`)
			return errors
		},
    updatePassword() {
			if (this.submitting) {
				return
			}

			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			this.submitting = true
			authApi.setPassword(this.token, this.password)
        .then(() => {
					this.passwordUpdated = true
				})
				.finally(() => {
					this.submitting = false
				})
    }
	},
	validations: {
    password: { required, minLength: minLength(8) }
  },
}
</script>
<style lang="less" scoped>
.update-password-page {
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
