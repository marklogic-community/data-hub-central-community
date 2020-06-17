'use strict';

function getTypeName(type) {
	switch(type) {
		case 'string':
			return 'String'
		case 'boolean':
			return 'Boolean'
		case 'string':
			return 'String'
		case 'integer':
			return 'Integer'
		case 'decimal':
			return 'Decimal'
		case 'date':
			return 'Date'
		default:
			return type;
	}
}

let entities = fn.collection('http://marklogic.com/entity-services/models').toArray().map(e => e.toObject())

let edges = {}
let nodes = {}
entities.forEach(e => {
	const entityName = e.info.title
	const entity = e.definitions[entityName]
	const props = entity.properties

	const properties = Object.keys(props).map(propName => {
		const prop = props[propName]
		const ref = prop['$ref'] || (prop.items && prop.items['$ref'])
		if (ref) {
			const from = entityName.toLowerCase()
			const to = ref.replace('#/definitions/', '').toLowerCase()
			const edgeName = `${from}-${propName}-${to}`
			const edge = {
				id: edgeName,
				label: propName,
				from: from,
				to: to,
				cardinality: prop.datatype === 'array' ? '1:Many': '1:1'
			}
			edges[edgeName] = edge
			return null;
		}
		return {
			name: propName,
			type: (prop.datatype === 'array' && prop.items && prop.items.datatype) ? getTypeName(prop.items.datatype) : getTypeName(prop.datatype),
			isArray: prop.datatype === 'array',
			isRequired: entity.required.indexOf(propName) >= 0,
			isPii: entity.pii.indexOf(propName) >= 0,
			isPrimaryKey: (entity.primaryKey && entity.primaryKey === propName),
			isElementRangeIndex: entity.elementRangeIndex.indexOf(propName) >= 0,
			isRangeIndex: entity.rangeIndex.indexOf(propName) >= 0,
			isWordLexicon: entity.wordLexicon.indexOf(propName) >= 0,
			_propId: sem.uuidString(),
			description: prop.description || null,
			collation: prop.collation || (prop.items && prop.items.collation)
		}
	}).filter(p => p !== null)

	nodes[entityName.toLowerCase()] = {
		label: entityName,
		entityName: entityName,
		type: "entity",
		id: entityName.toLowerCase(),
		version: e.info.version || '0.0.1',
		baseUri: e.info.baseUri || 'http://marklogic.com/envision/',
		description: e.info && e.info.description,
		properties
	}
})

const model = {
	name: `My Hub Model`,
	nodes,
	edges
}

model
