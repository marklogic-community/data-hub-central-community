const tde = require('/MarkLogic/tde.xqy');
const model = require('/envision/model.sjs').model();
const config = require('/envision/config.sjs');

// creates parent path refs for getting back to the
// entity's id
function createDots(path) {
	if (!path) {
		return ''
	}
	let dotCount = path.split('/').length - 1
	let dots = []
	for (let i = 0; i < dotCount; i++) {
		dots.push('..')
	}
	dots = dots.join('/')
	if (dots.length > 0) {
		dots += '/'
	}
	return dots
}

function createTdes() {
	if (!model) {
		fn.error('MISSING MODEL');
	}

	let subTemplates = {}
	for (let key in model.edges) {
		let edge = model.edges[key];
		const from = edge.from.split('/').pop()
		const to = edge.to.split('/').pop()
		const fromNode = model.nodes[from]
		const toNode = model.nodes[to]
		let subj = null
		const pred = `sem:iri("http://www.w3.org/2000/01/rdf-schema#${edge.label}")`
		let obj = null
		let node = null
		const hasConcept = ((fromNode && fromNode.type === 'concept') || (toNode && toNode.type === 'concept'))
		let subTempKey = null
		let concept = null
		let conceptType = null
		let keyFrom = edge.keyFrom || fromNode.idField
		let keyTo = edge.keyTo || toNode.idField
		if (hasConcept) {
			if (fromNode.type === 'entity') {
				let dots = createDots(keyTo)
				node = fromNode
				subj = `sem:iri(fn:replace(fn:concat("${from}#", fn:string-join((../../${dots}${keyFrom},../${dots}${keyFrom})[1], ';;')), " ", ""))`
				obj = `sem:iri(fn:concat("${toNode.baseUri || ''}${to}#", xs:string(.)))` // don't replace spaces here because this is displayed
				subTempKey = `./${keyTo}`
				concept = obj
				conceptType = toNode.entityName
			}
			else {
				let dots = createDots(keyFrom)
				node = toNode
				subj = `sem:iri(fn:concat("${fromNode.baseUri || ''}${from}#", xs:string(.)))`// don't replace spaces here because this is displayed
				obj = `sem:iri(fn:replace(fn:concat("${to}#", fn:string-join((../../${dots}${keyTo},../${dots}${keyTo})[1], ';;')), " ", ""))`
				subTempKey = `./${keyFrom}`
				concept = subj
				conceptType = fromNode.entityName
			}
		}
		else {
			if (!edge.keyFrom || fromNode.idField === keyFrom) {
				let dots = createDots(keyTo)
				node = toNode
				subj = `sem:iri(fn:replace(fn:concat("${from}#", xs:string(.)), " ", ""))`
				obj = `sem:iri(fn:replace(fn:concat("${to}#", fn:string-join((../../${dots}${toNode.idField},../${dots}${toNode.idField})[1], ';;')), " ", ""))`
				subTempKey = `./${keyTo}`
			}
			else if (!edge.keyTo || toNode.idField === keyTo) {
				let dots = createDots(keyFrom)
				node = fromNode
				subj = `sem:iri(fn:replace(fn:concat("${from}#", fn:string-join((../../${dots}${fromNode.idField},../${dots}${fromNode.idField})[1], ';;')), " ", ""))`
				obj = `sem:iri(fn:replace(fn:concat("${to}#", xs:string(.)), " ", ""))`
				subTempKey = `./${keyFrom}`
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

		// doesn't apply to concepts - eg ${entity.idField} won't exist, no concept of collection for concept
		if ( entity.type !== "concept") {
			const user = xdmp.getCurrentUser()
			let collections = [entity.entityName]

			// for a multi-tenant environment, limit the rule to only the current
			// user's collection. This keeps the rule from seeing other users' data
			if (config.isMultiTenant) {
				collections.push(`http://marklogic.com/envision/user/${user}`)
				collections = [{ collectionsAnd: collections}]
			}
			let template = {
				template: {
					context: '//*:instance/*:' + entity.entityName,
					collections: collections
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
			const permissions = [
				xdmp.permission("data-hub-developer", "update"),
				xdmp.permission("data-hub-operator", "read")
			]
			tde.templateInsert(`/${user}/${entity.id}-relationships-tde.json`, template, permissions, [user]);
		}
	}

	// not necessary, just return it for grins
	return templates;
}

module.exports.createTdes = module.amp(createTdes);
