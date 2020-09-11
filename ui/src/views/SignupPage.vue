<template>
	<v-container fluid fill-height class="signup-page">
		<v-layout align-center justify-center>
			<v-flex xs12 sm8 md4>
				<v-card class="elevation-12">
					<v-card-title>Sign Up</v-card-title>
					<v-card-text v-if="signupComplete">
						<div class="request-sent">
							Check your inbox for the next steps.
						</div>
					</v-card-text>
					<v-form v-else v-on:submit.prevent="register()">
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
								@change="delayTouch($v.email)"
								@blur="$v.email.$touch()"></v-text-field>
							<v-text-field
								outlined
								prepend-inner-icon="person"
								name="name"
								v-model="name"
								placeholder="Name"
								type="text"
								:error-messages="inputErrors('name', 'Name')"
								@blur="$v.name.$touch()"></v-text-field>
							<v-text-field
								outlined
								prepend-inner-icon="lock"
								name="password"
								placeholder="Password"
								v-model="password"
								type="password"
								:error-messages="inputErrors('password', 'Password')"
								@blur="$v.password.$touch()"></v-text-field>
							<div class="buttons">
								<v-btn id="submit-btn" :disabled="submitting" type="submit" color="primary">Register</v-btn>
							</div>
						</v-card-text>
						<v-card-actions>
							<span>Already have an account?</span><router-link to="/login">Log In</router-link>
						</v-card-actions>
					</v-form>
				</v-card>
			</v-flex>
		</v-layout>
	</v-container>
</template>

<script>
import { required, email, minLength } from 'vuelidate/lib/validators'
import authApi from '../api/AuthApi'

const touchMap = new WeakMap()

export default {
  data() {
    return {
			email: '',
			name: '',
			password: '',
			signupComplete: false,
			submitting: false
    }
  },
  methods: {
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('required') && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('email') && !this.$v[field].email && errors.push(`Must be an email address.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('minLength') && !this.$v[field].minLength && errors.push(`${fieldName} must be at least 8 characters long.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('isUnique') && !this.$v[field].isUnique && errors.push(`${fieldName} already exists.`)
			return errors
		},
    register() {
			if (this.submitting) {
				return
			}

			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			this.submitting = true
      this.$store
        .dispatch('auth/signup', {
					email: this.email,
					name: this.name,
          password: this.password
        })
        .then(() => {
					this.signupComplete = true
				})
				.finally(() => {
					this.submitting = false
				})
		},
		delayTouch($v) {
			$v.$reset()
			if (touchMap.has($v)) {
				clearTimeout(touchMap.get($v))
			}
			touchMap.set($v, setTimeout($v.$touch, 1250))
		}
	},
	validations: {
    email: {
			required,
			email,
			async isUnique(value) {
        // standalone validator ideally should not assume a field is required
        if (value === '') return true

				const exists = await authApi.userExists(value)
				return !exists
			}
		},
		name: { required },
		password: { required, minLength: minLength(8) }
  },
}
</script>

<style lang="less" scoped>
.signup-page {
	background: linear-gradient(0deg, black, #555);
}
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
.request-sent {
	text-align: center;
	border: 1px solid #ddd;
	padding: 1em;
}
</style>
