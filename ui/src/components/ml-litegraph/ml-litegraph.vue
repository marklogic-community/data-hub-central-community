<template>
    <div class="ml-litegraph">
      <canvas ref="lg" id='mycanvas' width='750' height='720' style='border: 1px solid'></canvas>
    </div>
</template>

<script>
import LiteGraph from 'litegraph.js';


export default {
  name: 'ml-litegraph',
  props: {
	  title: { type:String, required:false, default(){ return 'MyCanvas'; } }

  },
  data() {
    return {
	    //title:'MapPage
        graph:null
    };
  },
    methods:{

        registerNodeType: function(){}

    },
      mounted:  function() {


	      this.graph = new LiteGraph.LGraph();


          var canvas = new LiteGraph.LGraphCanvas("#mycanvas", this.graph);

          this.graph.start()


          this.registerNodeType = LiteGraph.registerNodeType
      /*    var node_const = LiteGraph.createNode("basic/const");
           node_const.pos = [200,200];
          this.graph.add(node_const);
           node_const.setValue(4.5);

          var node_watch = LiteGraph.createNode("basic/watch");
             node_watch.pos = [700,200];
          this.graph.add(node_watch);

             node_const.connect(0, node_watch, 0 );


          function sum(a,b)
          {
              return a+b;
          }
*/

	 }
};
</script>
<style src='../../../node_modules/litegraph.js/css/litegraph.css'></style>
