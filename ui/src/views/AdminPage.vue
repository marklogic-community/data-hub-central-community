/* eslint-disable no-mixed-spaces-and-tabs */
<script>
import axios from 'axios';
import OSApi from '@/api/OSApi.js';

export default {
    name:'AdminPage',
    data: ()=> ({
        msg1: '',
        error1: '' ,
        flowMsg: '',
        flowError: '' ,
        datahub: ''  
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
                this.datahub=response.data;
				return response.data;
			})
			.catch(error => {
				console.error('Error getting DHS config:', error);
				return error;
			});
        },
        async deployRunFlows(){
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
        }},
    mounted() {
        this.getDataHubConfig();
    }
}

</script>

<template>
    <div id="adminContainer">
        <h1>Envision Admin Page</h1>
        <fieldset class="col-sm-9">
            <legend>Data Hub</legend>
             <p>These are the properties of your Data Hub:</p>
            <v-data-table
                :items="datahub"
            ></v-data-table>
            <v-simple-table dense>
                 <tbody>
                    <tr v-for="(value, key) in datahub" >
                        <td >{{key}}</td>
                        <td >{{value}}</td>
                        <td class="action"></td>
                    </tr>
                </tbody>
            </v-simple-table>
            <p class="error">{{ flowError }}</p>
            <p class="success">{{ flowMsg }}</p>
            <v-btn color="primary" class="right" v-on:click="deployRunFlows" aria-label="Run flows.">Run Flows</v-btn> 
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
            <legend>Backup and Restore</legend>
            <p>Run the commands below from your host operating system</p>
            <p><em>Backup:</em> the commands below will create a file in your current directory called EnvisionBackup.zip.
                (It will overwrite this file if it exists, so be careful).
                The file contains the Data Hub entities and flows, and the model.json file used by Envision.
            </p>
            <p class="code">
                docker exec -it envision_datahub_1 /bin/sh /envision/datahub/gradlew EnvisionBackup <br/>
                docker cp envision_datahub_1:/tmp/backup/EnvisionBackup.zip .
            </p>
            <hr>
            <p></p>
            <p><em>Restore:</em> the commands below will restore your Envision.
                The file we restore from must be called EnvisionBackup.zip and should be in your current directory.
            </p>
            <p class="code">
                docker cp EnvisionBackup.zip envision_datahub_1:/tmp<br/>
                docker exec -it envision_datahub_1 /bin/sh /envision/datahub/gradlew TGrestore
          </p>
          <p>After doing a restore you will need to refresh the Envision window to see your changes</p>
        </fieldset>
        <fieldset class="col-sm-9">
            <legend>Sashenka</legend>
            <p>Can't remember how to spell Sashenka? If you are running Envision using Docker (i.e. not in development mode),
                run the command below from your Mac/PC terminal to change Sashenka to an easier to remember name, such as
                Jane in this example.</p>
            <p class="code">
                docker exec -it envision_envision_1 sh -c
                "sed -i 's/Sashenka/Jane/g' /envision/datahub/data/BrandACustomers/CC-BrandA-Customers.csv &&
                 sed -i 's/Sashenka/Jane/g' /envision/datahub/data/BrandBCustomers/CC-BrandB-Customers.csv ;"
            </p>
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

</style>
