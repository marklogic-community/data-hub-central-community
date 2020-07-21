/* eslint-disable no-mixed-spaces-and-tabs */
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
        headers: [
          {
            text: 'Property',
            align: 'start',
            sortable: false,
            value: 'prop',
          },
          { text: 'Setting', value: 'val' }
        ],
    }),

    methods: {
        async resetDemo() {
            this.msg1 = ""
            this.error1 = ""
            try {
                let response = await axios.post("/api/system/reset")
                if (response.data.success) {
                    this.msg1 = "Data reset."
                }
                else {
                    this.error1 = response.data.error
                }
            }
            catch(error) {
                this.error1= error
            }
        },
        getDataHubConfig() {
			return axios
			.get('/api/os/getDHprojectConfig/')
			.then(response => {
            console.log('Returning ' + response.data);
            this.datahub= response.data;
			return response.data;
			})
			.catch(error => {
				console.error('Error getting DHS config:', error);
				return error;
			});
        },
        getFlowNames() {
			return axios
			.get('/api/os/getFlowNames/')
			.then(response => {
                console.log('Returning ' + response.data);
                this.flows=response.data;
				return response.data;
			})
			.catch(error => {
				console.error('Error getting flows:', error);
				return error;
			});
        },
        async runFlows(){
            this.flowMsg = "Running flows."
            this.flowError = ""
            axios.post("/api/os/runFlows/")
            .then(response => {
                this.flowMsg =response.statusText
                return response.data
            })
            .catch(error => {
                console.error('error:', error);
                this.flowError = error
                return error;
            });
        },
        async runFlow(flowName){
            this.flowMsg = "Running flow " + flowName + "."
            this.flowError = ""
            axios.post("/api/os/runFlow/", null, {params: {flowName}})
            .then(response => {
                this.flowMsg =response.statusText
                return response.data
            })
            .catch(error => {
                console.error('error:', error);
                this.flowError = error
                return error;
            });
        },
        runFlowsSequence(){
            
        },
        handleDataHubTableClick(event){
            console.log(event);
        }},
    mounted() {
        this.getDataHubConfig();
        this.getFlowNames();
    }
}

</script>

<template>
    <div id="adminContainer">
        <h1>Envision Admin Page</h1>
        <fieldset class="col-sm-9">
            <legend>Data Hub</legend>
            <p>These are the properties of your Data Hub:</p>
            <v-data-table dense 
                :headers="headers"
                :items="datahub"
                :items-per-page="5"
            ></v-data-table>
            <!-- <v-data-table dense 
                :items="datahub" disable-filtering disable-pagination hide-default-footer item-key="prop">
                <template v-slot:body="{ datahub }">
                    <tbody>
                        <tr v-for="dhprop in datahub" :key="dhprop.prop">
                            <td>{{ dhprop.prop }}</td>
                            <td>{{ dhprop.val }}</td>
                        </tr>
                    </tbody>
                </template>
            </v-data-table> -->
            <v-simple-table dense>
                 <tbody>
                    <tr v-for="dhprop in datahub" :key="dhprop.prop" class='clickable-row' @click="handleDataHubTableClick(dhprop)">
                        <td >{{dhprop.prop}}</td>
                        <td >{{dhprop.val}}</td>
                    </tr>
                </tbody>
            </v-simple-table>
            <p class="error">{{ flowError }}</p>
            <p class="success">{{ flowMsg }}</p>
            <v-btn color="primary" class="right" v-on:click="runFlows" aria-label="Run flows.">Run Flows</v-btn>
        </fieldset>
        <fieldset class="col-sm-9">
            <legend>Reset</legend>
            <v-btn color="primary" class="right" v-on:click="resetDemo">Reset</v-btn>
            <p>Press the reset button to delete documents created while demonstrating. This button clears the Jobs
                database but does not delete any documents in data-hub-STAGING/FINAL that are assiciated
                with entity services, flows, steps etc.</p>
            <p class="error">{{ error1 }}</p>
            <p class="success">{{ msg1 }}</p>
        </fieldset>
 		<fieldset class="col-sm-9">
			<legend>Enhancements</legend>
			<p>Contact the Envision team if you'd like other admin type features that would make all our lives easier!</p>
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
