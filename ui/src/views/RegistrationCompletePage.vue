<template>
	<v-container fluid fill-height class="registration-complete-page">
		<v-layout align-center justify-center>
			<v-flex xs12 sm8 md4>
				<v-card class="elevation-12">
					<v-card-text v-if="accountVerified" class="text-center">
						<h1>Your account has been verified</h1>
					</v-card-text>
					<v-card-text v-else-if="error">
						<v-alert dense color="error">{{error}}</v-alert>
					</v-card-text>
					<v-card-actions>
						<router-link to="/login">Log in</router-link>
					</v-card-actions>
				</v-card>
			</v-flex>
		</v-layout>
	</v-container>
</template>

<script>

export default {
	computed: {
		error() {
			return this.$route.query.error
		},
		accountVerified() {
			return this.$route.query.accountVerified
		}
	}
}
</script>
<style lang="less" scoped>
.registration-complete-page {
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

// .request-sent {
// 	text-align: center;
// 	border: 1px solid #ddd;
// 	padding: 1em;
// }
</style>
