<template>
    <div>
	    <div>   {{title}} </div>
      <div id="rete" class="node-editor" style="height:200px" ref="reteContainer"></div>
    </div>
</template>

<script>
    import Rete from "rete";
    import ConnectionPlugin from 'rete-connection-plugin';
    import VueRenderPlugin from 'rete-vue-render-plugin';


    var numSocket = new Rete.Socket('Number value');
    var VueNumControl = {
        props: ['readonly', 'emitter', 'ikey', 'getData', 'putData'],
        template: '<input type="number" :readonly="readonly" :value="value" @input="change($event)" @dblclick.stop="" @pointermove.stop=""/>',
        data() {
            return {
                value: 0,
            }
        },
        methods: {
            change(e){
                this.value = +e.target.value;
                this.update();
            },
            update() {
                if (this.ikey)
                    this.putData(this.ikey, this.value)
                this.emitter.trigger('process');
            }
        },
        mounted() {
            this.value = this.getData(this.ikey);
        }
    }

    class NumControl extends Rete.Control {

        constructor(emitter, key, readonly) {
            super(key);
            this.component = VueNumControl;
            this.props = { emitter, ikey: key, readonly };
        }

        setValue(val) {
            this.vueContext.value = val;
        }
    }

    class NumComponent extends Rete.Component {

        constructor(){
            super("Number");
        }

        builder(node) {
            var out1 = new Rete.Output('num', "Number", numSocket);

            return node.addControl(new NumControl(this.editor, 'num')).addOutput(out1);
        }

        worker(node, inputs, outputs) {
            outputs['num'] = node.data.num;
        }
    }

    class AddComponent extends Rete.Component {
        constructor(){
            super("Add");
        }

        builder(node) {
            var inp1 = new Rete.Input('num1',"Number", numSocket);
            var inp2 = new Rete.Input('num2', "Number2", numSocket);
            var out = new Rete.Output('num', "Number", numSocket);

            inp1.addControl(new NumControl(this.editor, 'num1'))
            inp2.addControl(new NumControl(this.editor, 'num2'))

            return node
                .addInput(inp1)
                .addInput(inp2)
                .addControl(new NumControl(this.editor, 'preview', true))
                .addOutput(out);
        }

        worker(node, inputs, outputs) {
            var n1 = inputs['num1'].length?inputs['num1'][0]:node.data.num1;
            var n2 = inputs['num2'].length?inputs['num2'][0]:node.data.num2;
            var sum = n1 + n2;

            this.editor.nodes.find(n => n.id == node.id).controls.get('preview').setValue(sum);
            outputs['num'] = sum;
        }
    }




    export default {
  name: 'ml-rete',
  props: {
	  title: { type:String, required:false, default(){ return 'MyCanvas'; } }

  },
  data() {
    return {
	    //title:'MapPage
        editor:null
    };
  },
    methods:{



    },

        components: {
            //mlFacets
            ConnectionPlugin,
            VueRenderPlugin
        },
      mounted:  function() {



          var components = [new NumComponent(), new AddComponent()];

          var editor = new Rete.NodeEditor('demo@0.1.0', this.$refs["reteContainer"]);

          let readyMenu = [10, 12, 14];
          let dontHide = ['click'];
         /* editor.use(ContextMenuPlugin.default);
          editor.use(AreaPlugin);
          editor.use(CommentPlugin.default);
          editor.use(HistoryPlugin);
          editor.use(ConnectionMasteryPlugin.default);*/

          var engine = new Rete.Engine('demo@0.1.0');

          components.map(c => {
              editor.register(c);
              engine.register(c);
          });



          editor.view.resize();

          editor.trigger('process');



	 }
};

</script>

<style>
    #rete {
        height: 100% !important
    }

    #modules {
        position: absolute;
        left: 0;
        top: 0;
        z-index: 5;
    }

    .module-list {
        padding: 5px;
        cursor: pointer;
    }

    .module:hover {
        color: #a167e7;
    }

    .node .socket.number {
        background: #96b38a
    }

    .node .socket.float {
        background: red;
    }
</style>

