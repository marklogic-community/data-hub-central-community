const tde = require('/MarkLogic/tde.xqy');

const model = require('/model.sjs');

if (!model) {
	fn.error('MISSING MODEL');
}

let subTemplates = {}
for (let key in model.edges) {
	let edge = model.edges[key];
	const fromNode = model.nodes[edge.from]
	const toNode = model.nodes[edge.to]
	let subj = null
	const pred = `sem:iri("http://www.w3.org/2000/01/rdf-schema#${edge.label}")`
	let obj = null
	let node = null
	const hasConcept = ((fromNode && fromNode.type === 'concept') || (toNode && toNode.type === 'concept'))
	let subTempKey = null
	let concept = null
	let conceptType = null
	if (hasConcept) {
		if (fromNode.type === 'entity') {
			node = fromNode
			subj = `sem:iri(fn:concat("${edge.from}#", fn:string-join((../../${edge.keyFrom},../${edge.keyFrom})[1], ';;')))`
			obj = `sem:iri(fn:concat("${edge.to}#", xs:string(.)))`
			subTempKey = `./${edge.keyTo}`
			concept = obj
			conceptType = toNode.entityName
		}
		else {
			node = toNode
			subj = `sem:iri(fn:concat("${edge.from}#", xs:string(.)))`
			obj = `sem:iri(fn:concat("${edge.to}#", fn:string-join((../../${edge.keyTo},../${edge.keyTo})[1], ';;')))`
			subTempKey = `./${edge.keyFrom}`
			concept = subj
			conceptType = fromNode.entityName
		}
	}
	else {
		if (fromNode.idField === edge.keyFrom) {
			node = toNode
			subj = `sem:iri(fn:replace(fn:concat("${edge.from}#", xs:string(.)), " ", ""))`
			obj = `sem:iri(fn:replace(fn:concat("${edge.to}#", fn:string-join((../../${toNode.idField},../${toNode.idField})[1], ';;')), " ", ""))`
			subTempKey = `./${edge.keyTo}`
		}
		else if (toNode.idField === edge.keyTo) {
			node = fromNode
			subj = `sem:iri(fn:replace(fn:concat("${edge.from}#", fn:string-join((../../${fromNode.idField},../${fromNode.idField})[1], ';;')), " ", ""))`
			obj = `sem:iri(fn:replace(fn:concat("${edge.to}#", xs:string(.)), " ", ""))`
			subTempKey = `./${edge.keyFrom}`
 		}
	}

	if (node) {
		const subTemp = subTemplates[node.id] || {}
		let triples = subTemp[subTempKey] || []
		const trip = {
			"subject": {
				"val": subj
			},
			"predicate": {
				"val": pred
			},
			"object": {
				"val": obj
			}
		}
		triples.push(trip);
		if (hasConcept) {
			triples.push({
				"subject": {
					"val": concept
				},
				"predicate": {
					"val": 'sem:iri("http://www.w3.org/2000/01/rdf-schema#hasEntityType")'
				},
				"object": {
					"val": `"${conceptType}"`
				}
			})
		}
		subTemp[subTempKey] = triples
		subTemplates[node.id] = subTemp
	}
}

let templates = [];
for (let key in model.nodes) {
	let entity = model.nodes[key];
	let subTemps = subTemplates[entity.id] || {};

	let template = {
		template: {
			context: '//*:instance/*:' + entity.entityName,
			collections: [entity.entityName]
		}
	}
	template.template.templates = []
	template.template.templates.push({
		context: `./${entity.idField}`,
		triples: [
			{
				"subject": {
					"val": "xdmp:node-uri(.)"
				},
				"predicate": {
					"val": 'sem:iri("http://www.w3.org/2000/01/rdf-schema#hasLabel")'
				},
				"object": {
					"val": `sem:iri(fn:concat("${entity.entityName}#", xs:string(.)))`
				}
			},
			{
				"subject": {
					"val": "xdmp:node-uri(.)"
				},
				"predicate": {
					"val": 'sem:iri("http://www.w3.org/2000/01/rdf-schema#hasId")'
				},
				"object": {
					"val": `sem:iri(fn:replace(fn:concat("${entity.entityName.toLowerCase()}#", xs:string(.)), " ", ""))`
				}
			},
			{
				"subject": {
					"val": `sem:iri(fn:replace(fn:concat("${entity.entityName.toLowerCase()}#", xs:string(.)), " ", ""))`
				},
				"predicate": {
					"val": 'sem:iri("http://www.w3.org/2000/01/rdf-schema#hasEntityType")'
				},
				"object": {
					"val": `"${entity.entityName}"`
				}
			}
		]
	})
	for (let key in subTemps) {
		let sub = subTemps[key]
		template.template.templates.push({
			context: key,
			triples: sub
		})
	}
	templates.push(template);
	// console.log('template', JSON.stringify(template))
	tde.templateInsert(`${entity.id}-relationships-tde.json`, template);
}

// not necessary, just return it for grins
templates;
