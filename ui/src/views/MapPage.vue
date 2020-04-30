<template>
    <section class="modeler-page">
        <div class="modeler row">

            <div class="col-md-8">
                <ml-litegraph :title='title' ref="liteGraphInstance"></ml-litegraph>
            </div>
            <div class="col-md-4">
                <H4>Available entities:</H4>
                <entity-pick-list
                        :nodesCache="nodesCache"
                        :currentNode="currentNode"
                        v-on:selectedNode="onSelectedNode($event)">
                </entity-pick-list>
                <button v-if="currentNode!=null" v-on:click="btnAddToDrawing">Add to drawing area</button>
            </div>

        </div>
    </section>
</template>

<script>

    import {LiteGraph} from 'litegraph.js';
    import EntityPickList from '@/components/ml-modeler/EntityPickList';
    //import litegraph.css;

    import mlLitegraph from '@/components/ml-litegraph/ml-litegraph.vue';   //how to tell webpack to use component

    var APP_LOG_LIFECYCLE_EVENTS = true;

    function doMLLoad(self) {
        self.$store
            .dispatch('model/view')
            .then(function (response) {
                if (!response.isError && response.response) {
                    var graph = response.response

                    self.nodesCache = JSON.parse(JSON.stringify(graph.nodes))
                    self.edgesCache = JSON.parse(JSON.stringify(graph.edges))
                } else {
                    console.log("Error loading model.json: " + JSON.stringify(response))
                    self.nodesCache = {}
                    self.edgesCache = {}
                }
            })
    }

    export default {
        name: 'MapPage',
        // props: ['type'],  //what you provide from outside to components - this page expecting expecting type property
        data() {
            return {
                title: 'Map Page',
                currentNode: null,
                nodesCache: {}

            };
        },
        components: {
            //mlFacets,
            mlLitegraph,
            EntityPickList

        },
        computed: {
            isLoggedIn() {
                //return this.$store.state.auth.authenticated;
                return true;
            },
            nodes() {
                return Object.values(this.nodesCache);
            },
            edges() {
                return Object.values(this.edgesCache);
            }
        },
        beforeCreate() {
            if (APP_LOG_LIFECYCLE_EVENTS) {
                console.log("beforeCreate");
            }
        },
        created() {
            if (APP_LOG_LIFECYCLE_EVENTS) {
                console.log("created");
            }
        },
        beforeMount() {
            if (APP_LOG_LIFECYCLE_EVENTS) {
                console.log("beforeMount");
            }
        },
        mounted: function () {
            if (APP_LOG_LIFECYCLE_EVENTS) {
                console.log("mounted");
            }
            console.log("mounted");
            console.log("init lib")
            this.initLibrary(LiteGraph)
            this.initLiteGGraph()
            doMLLoad(this)
        },
        beforeUpdate: function () {
            if (APP_LOG_LIFECYCLE_EVENTS) {
                console.log("beforeUpdate");
            }
        },
        updated: function () {
            if (APP_LOG_LIFECYCLE_EVENTS) {
                console.log("updated");
            }
        },
        beforeDestroyed: function () {
            if (APP_LOG_LIFECYCLE_EVENTS) {
                console.log("beforeDestroyed ");
            }
        },
        destroyed: function () {
            if (APP_LOG_LIFECYCLE_EVENTS) {
                console.log("destroyed");
            }
        },
        /*created() {
          if (this.isLoggedIn) {
            this.page = this.$store.getters['search/' + this.type + '/page'] || 1;
          }
        },
        mounted() {
          if (this.$route.params && this.$route.params.refresh) {
            this.search();
          }
        },
        watch: {
          isLoggedIn: function(isLoggedIn) {
            if (isLoggedIn) {
              this.search();
            }
          }
        },*/
        methods: {

            showModal() {
                console.log("show modal")
                this.$refs['my-modal'].show();

            },
            onSelectedNode: function (e) {
                console.log(e)
                this.currentNode = e;

            },
            btnAddToDrawing(){
                let blockId = this.registerBlockFromEntityDefinition(this.currentNode)
                let gOutput = LiteGraph.createNode(blockId);
                gOutput.pos = [550, 300];
                this.$refs.liteGraphInstance.graph.add(gOutput);

            },

            initLibrary(LiteGraph) {
                let isBrowser = true
                let configs = [

                    {
                        "functionName": "UpperCase",
                        "blockName": "UpperCase",
                        "library": "String",
                        "inputs": [
                            {
                                name: "string",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "STRING",
                                "type": "xs:string"
                            }
                        ],
                        "function": {
                            "ref": "fn.upperCase",
                            "code": null
                        }


                    },
                    {
                        "functionName": "LowerCase",
                        "blockName": "LowerCase",
                        "library": "String",
                        "inputs": [
                            {
                                name: "STRING",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "string",
                                "type": "xs:string"
                            }
                        ],
                        "function": {
                            "ref": "fn.lowerCase",
                            "code": null
                        }


                    }

                    ,
                    {
                        "functionName": "Gender LookUp",
                        "blockName": "Gender LookUp",
                        "library": "LookUp",
                        "inputs": [
                            {
                                name: "srcGender",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "Gender",
                                "type": "xs:string"
                            }
                        ],
                        "function": {
                            "ref": "cts.collectionQuery",
                            "code": null
                        }


                    }
                    ,
                    {
                        "functionName": "Title LookUp",
                        "blockName": "Title LookUp",
                        "library": "LookUp",
                        "inputs": [
                            {
                                name: "srcTitle",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "Title",
                                "type": "xs:string"
                            }
                        ],
                        "function": {
                            "ref": "cts.collectionQuery",
                            "code": null
                        }


                    }

                    ,
                    {
                        "functionName": "CustomerCRM",
                        "blockName": "CustomerCRM",
                        "library": "Sources",
                        "inputs": [],
                        events: [
                            {
                                event: "onSelected",
                                code: null
                            }


                        ],
                        "outputs": [
                            {
                                name: "ID",
                                type: "xs:string"
                            },
                            {
                                name: "FNAME",
                                type: "xs:string"
                            },
                            {
                                name: "FAMILYNAME",
                                type: "xs:string"
                            },
                            {
                                name: "SEX",
                                type: "xs:string"
                            },
                            {
                                name: "LOYALTYDATE",
                                type: "xs:string"
                            },
                            {
                                name: "CITY",
                                type: "xs:string"
                            },
                            {
                                name: "POSTAL",
                                type: "xs:string"
                            },
                            {
                                name: "ADDRESS",
                                type: "xs:string"
                            }
                        ],
                        "function": {
                            "ref": "cts.jsonPropertyValueQuery",
                            "code": null
                        }


                    }
                    ,
                    {
                        "functionName": "Customer",
                        "blockName": "Customer",
                        "library": "BusinessEntities",
                        "inputs": [
                            {
                                name: "FirstName",
                                type: "xs:string"
                            },
                            {
                                name: "LastName",
                                type: "xs:string"
                            },
                            {
                                name: "Gender",
                                type: "xs:string"
                            },
                            {
                                name: "FirstPurchaseDate",
                                type: "xs:string"
                            },
                            {
                                name: "City",
                                type: "xs:string"
                            },
                            {
                                name: "Zip",
                                type: "xs:string"
                            },
                            {
                                name: "StreetAddress",
                                type: "xs:string"
                            },
                            {
                                name: "State",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [],
                        "function": {
                            "ref": "cts.jsonPropertyValueQuery",
                            "code": null
                        }


                    },
                    {
                        "functionName": "fn_doc",
                        "blockName": "doc",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "uri",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "doc",
                                "type": "node"
                            }
                        ],
                        "function": {
                            "ref": "fn.doc",
                            "code": null
                        }


                    },
                    {
                        "functionName": "fn_collection",
                        "blockName": "collection",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "collectionName",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "docs",
                                "type": "node()*"
                            }
                        ],
                        "function": {
                            "ref": "fn.collection",
                            "code": null
                        }


                    },
                    {
                        "functionName": "fn_baseUri",
                        "blockName": "baseUri",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "node",
                                type: "node"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "uri",
                                "type": "xs:string"
                            }
                        ],
                        "function": {
                            "ref": "fn.baseUri",
                            "code": null
                        }


                    },
                    {
                        "functionName": "fn_head",
                        "blockName": "head",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "nodes",
                                type: null
                            }
                        ],
                        "outputs": [
                            {
                                "name": "node",
                                "type": null
                            }
                        ],
                        "function": {
                            "ref": "fn.head",
                            "code": null
                        }


                    },

                    {
                        "functionName": "fn_count",
                        "blockName": "count",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "list",
                                type: null
                            }
                        ],
                        "outputs": [
                            {
                                "name": "nbItems",
                                "type": "number"
                            }
                        ],
                        "function": {
                            "ref": "fn.count",
                            "code": null
                        }


                    },
                    {
                        "functionName": "cts_andQuery",
                        "blockName": "andQuery",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "query1",
                                type: "cts:query"
                            },
                            {
                                name: "query2",
                                type: "cts:query"
                            },
                            {
                                name: "query3",
                                type: "cts:query"
                            },
                            {
                                name: "query4",
                                type: "cts:query"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "query",
                                "type": "cts:query"
                            }
                        ],
                        "function": {
                            "ref": null,
                            "code": "let queries = [];" +
                                "    if(this.getInputData(0)!=undefined) queries.push(this.getInputData(0));" +
                                "    if(this.getInputData(1)!=undefined) queries.push(this.getInputData(1));" +
                                "    if(this.getInputData(2)!=undefined) queries.push(this.getInputData(2));" +
                                "    if(this.getInputData(3)!=undefined) queries.push(this.getInputData(3));" +
                                "    this.setOutputData( 0, cts.andQuery(queries));"
                        }


                    },
                    {
                        "functionName": "cts_orQuery",
                        "blockName": "orQuery",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "query1",
                                type: "cts:query"
                            },
                            {
                                name: "query2",
                                type: "cts:query"
                            },
                            {
                                name: "query3",
                                type: "cts:query"
                            },
                            {
                                name: "query4",
                                type: "cts:query"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "query",
                                "type": "cts:query"
                            }
                        ],
                        "function": {
                            "ref": null,
                            "code": "let queries = [];" +
                                "    if(this.getInputData(0)!=undefined) queries.push(this.getInputData(0));" +
                                "    if(this.getInputData(1)!=undefined) queries.push(this.getInputData(1));" +
                                "    if(this.getInputData(2)!=undefined) queries.push(this.getInputData(2));" +
                                "    if(this.getInputData(3)!=undefined) queries.push(this.getInputData(3));" +
                                "    this.setOutputData( 0, cts.orQuery(queries));"
                        }


                    },
                    {
                        "functionName": "cts_search",
                        "blockName": "search",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "query",
                                type: "cts:query"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "results",
                                "type": "node*"
                            }
                        ],
                        "function": {
                            "ref": "cts.search",
                            "code": null
                        }


                    },
                    {
                        "functionName": "cts_collectionQuery",
                        "blockName": "collectionQuery",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "collectionName",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "query",
                                "type": "cts:query"
                            }
                        ],
                        "function": {
                            "ref": "cts.collectionQuery",
                            "code": null
                        }


                    },
                    {
                        "functionName": "cts_jsonPropertyValueQuery",
                        "blockName": "jsonPropertyValueQuery",
                        "library": "MarkLogic",
                        "inputs": [
                            {
                                name: "property",
                                type: "xs:string"
                            },
                            {
                                name: "value",
                                type: "xs:string"
                            }
                        ],
                        "outputs": [
                            {
                                "name": "query",
                                "type": "cts:query"
                            }
                        ],
                        "function": {
                            "ref": "cts.jsonPropertyValueQuery",
                            "code": null
                        }


                    }


                ]

                LiteGraph.registered_node_types = {}
                for (let config of configs)
                    this.registerBlockFromConfig(config)

                /*   let code = ""
                   for (let config of configs) {

                       code += "function " + config.functionName + "(){"
                       code += config.inputs.map((input) => {
                           return "this.addInput('" + input.name + ((input.type) ? "','" + input.type + "');" : "');")
                       }).join("")
                       code += config.outputs.map((output) => {
                           return "this.addOutput('" + output.name + ((output.type) ? "','" + output.type + "');" : "');")
                       }).join("")
                       code += (config.properties) ? "config.properties = " + config.properties + ";" : "";
                       code += "};"

                       code += config.functionName + ".title = '" + config.blockName + "';";
                       code += config.functionName + ".prototype.onSelected = function(){ this.showModal()}.bind(this);";
                       if(!isBrowser) {
                           code += config.functionName + ".prototype.onExecute = function(){ ";


                           if (config.function.ref != null) {
                               let i = 0;
                               code += "this.setOutputData( 0, " + config.function.ref + "(" + config.inputs.map((input) => {
                                   return "this.getInputData(" + i++ + ")"
                               }).join(",") + "));"
                           } else {
                               code += config.function.code;

                           }
                           code += "};"
                       }
         //register in the syst em
                       code += "LiteGraph.registerNodeType('" + config.library + "/" + config.blockName + "', " + config.functionName + " );"
                   }

                   eval(code)   */


                this.InitLibraryDefaultNodes()
            },
            registerBlockFromConfig(config) {


                let isBrowser = true
                let myClass = function () {
                    this.config.inputs.map(item => {
                        this.addInput(item.name, item.type);
                    })
                    this.config.outputs.map(item => {
                        this.addOutput(item.name, item.type);
                    })
                }
                myClass.prototype.getInputByName = function (name) {
                }


                if (!isBrowser) {


                    if (config.function.ref != null) {

                        body = "this.setOutputData( 0, " + this.config.function.ref + "(" + Object.keys(this.config.inputs).map((key) => {
                            return "this.getInputData(" + i++ + ")"
                        }).join(",") + "));"
                    } else {
                        body = config.function.code;

                    }
                    myClass.prototype.onExecute = new Function(null, body)

                }

                myClass.title = config.blockName
                myClass.prototype.config = config


                LiteGraph.registerNodeType(myClass.prototype.config.library + "/" + myClass.prototype.config.blockName, myClass);
            },
            registerBlockFromEntityDefinition(config) {


                let isBrowser = true
                let myClass = function () {
                    this.config.properties.map(item => {
                        this.addInput(item.name, item.type);
                    })
                   /* this.config.outputs.map(item => {
                        this.addOutput(item.name, item.type);
                    })*/
                }
                myClass.prototype.getInputByName = function (name) {
                }


                if (!isBrowser) {


                    if (config.function.ref != null) {

                        body = "this.setOutputData( 0, " + this.config.function.ref + "(" + Object.keys(this.config.inputs).map((key) => {
                            return "this.getInputData(" + i++ + ")"
                        }).join(",") + "));"
                    } else {
                        body = config.function.code;

                    }
                    myClass.prototype.onExecute = new Function(null, body)

                }

                myClass.title = config.entityName
                myClass.prototype.config = config

                myClass.color = "#62809B"
                myClass.bgcolor = "rgba(98,128,155,0.49)"
                LiteGraph.registerNodeType("Entities/" + myClass.prototype.config.entityName, myClass);
                return "Entities/" + myClass.prototype.config.entityName
            },
            InitLibraryDefaultNodes() {

                function GlobalInput() {

                    //random name to avoid problems with other outputs when added
                    var input_name = "collector_input" //+ (Math.random()*1000).toFixed();

                    this.addOutput(input_name, null);

                    this.properties = {name: input_name, type: null};

                    var that = this;

                    Object.defineProperty(this.properties, "name", {
                        get: function () {
                            return input_name;
                        },
                        set: function (v) {
                            if (v == "")
                                return;

                            var info = that.getOutputInfo(0);
                            if (info.name == v)
                                return;
                            info.name = v;
                            if (that.graph)
                                that.graph.renameGlobalInput(input_name, v);
                            input_name = v;
                        },
                        enumerable: true
                    });

                    Object.defineProperty(this.properties, "type", {
                        get: function () {
                            return that.outputs[0].type;
                        },
                        set: function (v) {
                            that.outputs[0].type = v;
                            if (that.graph)
                                that.graph.changeGlobalInputType(input_name, that.outputs[0].type);
                        },
                        enumerable: true
                    });
                }

                GlobalInput.title = "Flow Input";
                GlobalInput.desc = "Input of the graph";

//When added to graph tell the graph this is a new global input
                GlobalInput.prototype.onAdded = function () {
                    this.graph.addGlobalInput(this.properties.name, this.properties.type);
                }

                GlobalInput.prototype.onExecute = function () {
                    var name = this.properties.name;

                    //read from global input
                    var data = this.graph.global_inputs[name];
                    if (!data) return;

                    //put through output
                    this.setOutputData(0, data.value);
                }

                LiteGraph.registerNodeType("MarkLogic/FlowInput", GlobalInput);


//Output for a subgraph
                function GlobalOutput() {
                    //random name to avoid problems with other outputs when added
                    var output_name = "BusinessEntity" //+ (Math.random()*1000).toFixed();

                    this.addInput(output_name, null);

                    this._value = null;

                    this.properties = {name: output_name, type: null};

                    var that = this;

                    Object.defineProperty(this.properties, "name", {
                        get: function () {
                            return output_name;
                        },
                        set: function (v) {
                            if (v == "")
                                return;

                            var info = that.getInputInfo(0);
                            if (info.name == v)
                                return;
                            info.name = v;
                            if (that.graph)
                                that.graph.renameGlobalOutput(output_name, v);
                            output_name = v;
                        },
                        enumerable: true
                    });

                    Object.defineProperty(this.properties, "type", {
                        get: function () {
                            return that.inputs[0].type;
                        },
                        set: function (v) {
                            that.inputs[0].type = v;
                            if (that.graph)
                                that.graph.changeGlobalInputType(output_name, that.inputs[0].type);
                        },
                        enumerable: true
                    });
                }

                GlobalOutput.title = "Flow Output";
                GlobalOutput.desc = "Output of the graph";

                GlobalOutput.prototype.onAdded = function () {
                    var name = this.graph.addGlobalOutput(this.properties.name, this.properties.type);
                }

                GlobalOutput.prototype.getValue = function () {
                    return this._value;
                }

                GlobalOutput.prototype.onExecute = function () {
                    this._value = this.getInputData(0);
                    this.graph.setGlobalOutputData(this.properties.name, this._value);
                }

                LiteGraph.registerNodeType("MarkLogic/FlowOutput", GlobalOutput);


                function featureLookupBlock() {

                    this.addInput("value", "xs:string");
                    this.addOutput("result", "xs:string");


                    this.addProperty("lookupMap", "xs:string");
                    this.addProperty("value", "xs:string");
                    this.addProperty("searchKey", "xs:string");
                    this.addProperty("resultKey", "xs:string");


                }

                featureLookupBlock.title = "Expert Lookup";
                /*featureLookupBlock.prototype.onExecute = function()
                {
                    //let output = "lookup(" + this.getInputData(0) + "," + this.getInputData(1) + "," + this.getInputData(2) + ")"
                    xdmp.log(this.properties["lookupMap"])
                    let output = fn.head(cts.search(
                        cts.andQuery([
                            cts.collectionQuery(this.properties["lookupMap"]   ),
                            cts.jsonPropertyValueQuery(this.properties["searchKey"]  , this.getInputData(0)   )

                        ])
                    ))
                    if(output!=null) this.setOutputData( 0, output.toObject()[ this.properties["resultKey"]])


                }*/

//register in the system
                LiteGraph.registerNodeType("LookUp/ExpertLookUp", featureLookupBlock);


            },
            initLiteGGraph() {

                var gInput = LiteGraph.createNode("Sources/CustomerCRM");
                gInput.pos = [50, 200];
                this.$refs.liteGraphInstance.graph.add(gInput);


                var gOutput = LiteGraph.createNode("BusinessEntities/Customer");
                gOutput.pos = [550, 200];
                this.$refs.liteGraphInstance.graph.add(gOutput);


            },
            mounted: function () {



            }

        }

    };
</script>

<style lang="less">
    .modeler-page {

        margin: 20px;
    }

    .search-row {
        margin-top: 20px;

        form {
            padding-bottom: 0;
        }
    }

    .graph-controls {
        display: none !important;
    }

    #btnTrigger {
        display: none;
    }

    .panel {
        margin-bottom: 2px;
    }

    .panel-heading {
        font-weight: bold;
        font-size: 1.1em;
        background-color: lightblue;
        padding: 0px;
        border-top-right-radius: 10px;
        border-top-left-radius: 10px;
        border-bottom-right-radius: 10px;
        border-bottom-left-radius: 10px;

    }

    .panel-block button {
        background-color: lightskyblue;
        border-top-left-radius: 8px;
        border-top-right-radius: 8px;
        border-bottom-left-radius: 8px;
        border-bottom-right-radius: 8px;
        margin-top: 5px;
        margin-right: 3px;
    }
    //Can't figure out how to make dialog-drag styles work in the component file
    .dialog-drag {
        -webkit-animation-duration: .2s;
        -webkit-animation-name: dialog-anim;
        -webkit-animation-timing-function: ease-in;
        -webkit-box-shadow: 1px 1px 1px rgba(0, 0, 0, .5);
        animation-duration: .2s;
        animation-name: dialog-anim;
        animation-timing-function: ease-in;
        background-color: #e9eef4;
        border: 2px solid #3f51b5;
        border-radius: 4px;
        box-shadow: 0 2px 1px -1px rgba(0, 0, 0, .2), 0 1px 1px 0 rgba(0, 0, 0, .14), 0 1px 3px 0 rgba(0, 0, 0, .12);
        height: auto;
        position: absolute;
        width: auto;
        z-index: 101
    }

    .dialog-drag .dialog-header {
        background-color:#3f51b5;
        color: #e9eef4;
        font-size: .9em;
        padding: .25em 3em .25em 1em;
        position: relative;
        text-align: left;
        width: auto
    }

    .dialog-drag .dialog-header .buttons {
        margin: .25em .25em 0 0;
        position: absolute;
        right: 0;
        top: 0;
        z-index: 105
    }

    .dialog-drag .dialog-header button.close, .dialog-drag .dialog-header button.pin {
        -webkit-box-shadow: none;
        background: transparent;
        border: none;
        box-shadow: none;
        color: #fff
    }

    .dialog-drag .dialog-header button.close:hover, .dialog-drag .dialog-header button.pin:hover {
        color: #e3a826
    }

    .dialog-drag .dialog-header button.close:after {
        content: "\2716"
    }

    .dialog-drag .dialog-header button.pin:after {
        content: "\1F513"
    }

    .dialog-drag .dialog-body {
        padding: 1em
    }

    .dialog-drag.fixed {
        -moz-user-select: auto;
        -ms-user-select: auto;
        -webkit-user-select: auto;
        border-color: #e3a826;
        user-select: auto
    }

    .dialog-drag.fixed button.pin {
        font-weight: 700
    }

    .dialog-drag.fixed button.pin:after {
        content: "\1F512"
    }

    /*
    @-webkit-keyframes dialog-anim {

        0% {
            -webkit-transform: scaleX(.1);
            opacity: 0;
            transform: scaleX(.1)
        }
        50% {
            -webkit-transform: rotate(1deg);
            transform: rotate(1deg)
        }
        to {
            opacity: 1
        }
    }

    @keyframes dialog-anim {
        0% {
            -webkit-transform: scaleX(.1);
            opacity: 0;
            transform: scaleX(.1)
        }
        50% {
            -webkit-transform: rotate(1deg);
            transform: rotate(1deg)
        }
        to {
            opacity: 1
        }
    }
    */
    //TODO these picker list styles don't work for some reason
    //Entity picker list styles
    .v-select {
        position: relative;
        width: 100%;
        height: 30px;
        cursor: pointer;

        &.disabled {
            cursor: not-allowed;

            .v-select-toggle {
                background-color: #f8f9fa;
                border-color: #f8f9fa;
                opacity: 0.65;
                cursor: not-allowed;

                &:focus {
                    outline: 0 !important;
                }
            }
        }
    }

    .v-select-toggle {
        display: flex;
        justify-content: space-between;
        user-select: none;
        padding: 0.375rem 0.75rem;
        color: #212529;
        background-color: #f8f9fa;
        border-color: #d3d9df;
        width: 100%;
        text-align: right;
        white-space: nowrap;
        border: 1px solid transparent;
        padding: 0.375rem 0.75rem;
        font-size: 12px;
        font-family: inherit, sans-serif;
        line-height: 1.5;
        border-radius: 0.25rem;
        transition: background-color, border-color, box-shadow, 0.15s ease-in-out;
        cursor: pointer;

        &:hover {
            background-color: #e2e6ea;
            border-color: #dae0e5;
        }
    }

    .arrow-down {
        display: inline-block;
        width: 0;
        height: 0;
        margin-left: 0.255em;
        margin-top: 7px;
        vertical-align: 0.255em;
        content: "";
        border-top: 0.3em solid;
        border-right: 0.3em solid transparent;
        border-bottom: 0;
        border-left: 0.3em solid transparent;
    }

    .v-dropdown-container {
        position: absolute;
        width: 100%;
        padding: 0.5rem 0;
        margin: 0.125rem 0 0;
        color: #212529;
        text-align: left;
        list-style: none;
        background-color: #306bff;
        background-clip: padding-box;
        border-radius: 0.25rem;
        border: 1px solid rgba(0, 0, 0, 0.15);
        z-index: 1000;
    }

    .v-dropdown-item {
        text-decoration: none;
        line-height: 25px;
        padding: 0.5rem 1.25rem;
        user-select: none;

        &:hover:not(.default-option) {
            background-color: #f8f9fa;
        }

        &.disabled {
            color: #9a9b9b;
        }

        &.selected {
            background-color: #45ff2e;
            color: #fff;

            &:hover {
                background-color: #007bff;
                color: #fff;
            }
        }

        &.disabled {
            cursor: not-allowed;

            &:hover {
                background-color: #fff;
            }
        }
    }

    .bs-searchbox {
        padding: 4px 8px;

        .form-control {
            display: block;
            width: 100%;
            padding: 0.375rem 0.75rem;
            font-size: 1rem;
            line-height: 1.5;
            color: #495057;
            background-color: #fff;
            background-clip: padding-box;
            border: 1px solid #ced4da;
            border-radius: 0.25rem;
            transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
        }
    }
</style>

