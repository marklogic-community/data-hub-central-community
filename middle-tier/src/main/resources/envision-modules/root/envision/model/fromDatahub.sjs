'use strict';

let entities = fn.collection('http://marklogic.com/entity-services/models').toArray().map(e => e.toObject())
let edges = {}
let nodes = {}

function arrayHasValue(arr, value) {
	return arr ? arr.indexOf(value) >= 0: false;
}

function buildDependentDefinitions(definition, allDefinitions, accumulatedDefinitions = {}) {
	if (definition && definition.properties) {
		Object.values(definition.properties).forEach((prop) => {
			const ref = prop['$ref'] || (prop.items && prop.items['$ref'])
			const isStructured = ref && ref.startsWith('#/definitions')
			if (isStructured) {
				const otherName = ref.replace(/.*\/([^\/]+)/, '$1')
				if (!Object.keys(accumulatedDefinitions).includes(otherName)) {
					accumulatedDefinitions[otherName] = allDefinitions[otherName];
					buildDependentDefinitions(accumulatedDefinitions[otherName], allDefinitions, accumulatedDefinitions)
				}
			}
		});
	}
	return accumulatedDefinitions;
}

function buildProperties(entityName, entity, definitions) {
	const props = entity.properties

	return Object.keys(props).map(propName => {
		const prop = props[propName]
		const ref = prop['$ref'] || (prop.items && prop.items['$ref'])
		const isExternal = ref && !ref.startsWith('#/definitions')
		if (ref && isExternal) {
			const from = entityName.toLowerCase()
			const to = ref.replace(/.*\/([^\/]+)/, '$1').toLowerCase()
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
		let type = null
		let isStructured = false
		let structureDefinitions = null
		if (ref && !isExternal) {
			const otherName = ref.replace(/.*\/([^\/]+)/, '$1')
			type = otherName;
			isStructured = true;
			let definition = definitions[otherName]
			structureDefinitions = buildDependentDefinitions(definition, definitions, {[otherName]: definition})
		}
		else if (prop.datatype === 'array' && prop.items && prop.items.datatype) {
			type = prop.items.datatype;
		}
		else {
			type = prop.datatype;
		}

		let newProp = {
			_propId: sem.uuidString(),
			name: propName,
			type: type
		}
		if (structureDefinitions) {
			newProp.structureDefinitions = structureDefinitions;
		}
		newProp = {
			...newProp,
			isArray: prop.datatype === 'array',
			isStructured: isStructured,
			isRequired: arrayHasValue(entity.required, propName),
			isPii: arrayHasValue(entity.pii, propName),
			isPrimaryKey: (entity.primaryKey && entity.primaryKey === propName),
			isElementRangeIndex: arrayHasValue(entity.elementRangeIndex, propName),
			isRangeIndex: arrayHasValue(entity.rangeIndex, propName),
			isWordLexicon: arrayHasValue(entity.wordLexicon, propName),
			description: prop.description || null,
			collation: prop.collation || (prop.items && prop.items.collation)
		}

		return newProp
	}).filter(p => p !== null)
}
entities.forEach(e => {
	const entityName = e.info.title
	const entity = e.definitions[entityName]
	const properties = buildProperties(entityName, entity, e.definitions)

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
