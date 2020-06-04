'use strict';

fn.collection(['http://marklogic.com/data-hub/flow']).toArray()
	.filter(flow => !xdmp.nodeUri(flow).match('/default-'))
