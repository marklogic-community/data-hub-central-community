<script>
import axios from 'axios';

export default {
	name:'AdminPage',
	data: ()=> ({
		msg1: '',
		error1: '' ,
		flowMsg: '',
		flowError: '',
		datahub: '',
		flows: '',
		showFlowStatus: false
	}),

	methods: {
		async resetDemo() {
			this.flowMsg = ""
			this.flowError = ""
			try {
				let response = await axios.post("/api/system/reset")
				if (response.data.success) {
					this.flowMsg = "Data reset."
					this.showFlowStatus = true;
				}
				else {
					this.flowError = response.data.error
					this.showFlowStatus = true;
				}
			} catch(error) {
				this.flowError= error
				this.showFlowStatus=true;
			}
		},
		handleDataHubTableClick(event){
			console.log(event);
		}
	}
}
</script>

<template>
	<div id="adminContainer">
		<v-snackbar
			v-model="showFlowStatus"
			right
			top
		>
			{{ flowMsg + " " + flowError }}

			<template v-slot:action="{ attrs }">
				<v-btn
				color="red"
				text
				v-bind="attrs"
				@click="snackbar = false"
				>
				Close
				</v-btn>
			</template>
		</v-snackbar>
		<h1>Data Hub Central Community Edition Admin Page</h1>
		<fieldset class="col-sm-9">
			<legend>Reset</legend>
			<p>Press the reset button to delete documents created while demonstrating. This button clears the Jobs
				database but does not delete any documents in data-hub-STAGING/FINAL that are assiciated
				with entity services, flows, steps etc.</p>
			<v-btn color="primary" class="right" v-on:click="resetDemo">Reset</v-btn>
		</fieldset>
		<fieldset class="col-sm-9">
			<legend>Enhancements</legend>
			<p>Please <a href="https://github.com/marklogic-community/data-hub-central-community/issues/new/choose" target="_blank">file an issue on Github</a> if you'd like to report a bug or request enhancements.</p>
		</fieldset>
	</div>
</template>

<style scoped>
	#adminContainer {
		padding-left: 50px;
		padding-right: 50px;
		margin-top: 10px;
	}
	.right {
		float: right;
		margin-left:10px;
	}
	h1 {
		padding-bottom: 30px;
	}
	.adminItem {
		padding: 10px;
		border : 1px solid black;
		border-radius: 5px;
		margin-bottom: 10px;
	}
	fieldset {
		padding: 10px;
		border : 1px solid black;
		border-radius: 5px;
		margin-bottom: 10px;
	}
	fieldset legend {
		font-weight: bold;
	}
	.success, .error{
		border-radius: 3px;
		padding-left: 5px;
	}
	.code {
		padding-left: 20px;
		font-size: 1.1em;
		color: darkred;
	}
	.clickable-row {
		cursor: pointer;
	}
</style>
