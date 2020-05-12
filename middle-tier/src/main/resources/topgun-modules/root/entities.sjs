const provHelper = require('/prov-helper.xqy');
const model = require('/model.sjs');

/**
 * This method takes a given array of uris and returns the # of edges
 * for each uri
 *
 * @param uris an array of uris to get
 * @param opts an options object
 * Options:
 * 		start - start index for pagination
 * 		connectionLimit - # of connections to grab
 *    allowedEntities - an array of entities that are allowed, empty means all
 * 		labels - an array of edge labels to return, empty means all
 */
function getEntities(uris, opts) {
	let options = opts || {}
	let start = (!!options.start) ? options.start : 0
	let connectionLimit = (!!options.connectionLimit) ? options.connectionLimit : 10
	let allowedEntities = options.allowedEntities || []
	let labels = options.labels || []

	const resp = {
		nodes: {},
		edges: {}
	}
	if (uris.length < 1) {
		return resp
	}

	let entityFilter = ''
	if (allowedEntities && allowedEntities.length > 0) {
		entityFilter = `FILTER( ?entityType IN ( ${allowedEntities.map(et => `"${et}"`).join(', ')} ) )`
	}

	let labelFilter = ''
	if (labels && labels.length > 0) {
		labelFilter = `FILTER( ?lbl IN ( ${labels.map(label => `rdf:${label}`).join(', ')} ) )`
	}

	const sparql = `
		PREFIX rdf: <http://www.w3.org/2000/01/rdf-schema#>
		PREFIX fn: <http://www.w3.org/2005/xpath-functions#>

		select ?from
			(fn:head((?toUri, ?toId)) as ?to)
			(fn:string-join((?from, ?lbl, ?to), "-") as ?id)
			(fn:tail(fn:tokenize(?lbl, "#")) as ?label)
			(fn:head(fn:tokenize(?fromId, "#")) as ?fromType)
			(fn:head(fn:tokenize(?toId, "#")) as ?toType)
			(fn:not(fn:exists(?toUri)) as ?isConcept) where {

			{
				?from rdf:hasId ?fromId.
				?fromId ?lbl ?toId.
				?toId rdf:hasEntityType ?entityType.
				optional { ?toUri rdf:hasId ?toId }
				FILTER( ?from IN ( ${uris.map(uri => `"${uri}"`).join(', ')} ) )
				${entityFilter}
				${labelFilter}
			}
			UNION {
				?fromId ?lbl ?toId.
				?toUri rdf:hasId ?toId.
				?from rdf:hasId ?fromId.
				?fromId rdf:hasEntityType ?entityType.
				optional { ?from rdf:hasId ?fromId }
				FILTER( ?toUri IN ( ${uris.map(uri => `"${uri}"`).join(', ')} ) )
				${entityFilter}
				${labelFilter}
			}
		}
		LIMIT ${connectionLimit}
		OFFSET ${start}`

	const archivedCollections = cts.collectionMatch('sm-*-archived').toArray()
	const query = cts.notQuery(cts.collectionQuery(archivedCollections))
	const triples = sem.sparql(sparql, null, null, sem.store(null, query))
	for (let t of triples) {
		resp.edges[t.id] = t

		if (t.isConcept) {
			resp.nodes[t.to] = {
				id: t.to,
				label: t.to.toString().replace(/(.+)#(.+)/, '$2'),
				entityName: model.getName(t.toType),
				isConcept: true,
				edgeCounts: {}
			}
		}
		else {
			uris.push(t.to)
		}
	}

	const edgeCounts = getEdgeCounts(uris)

	uris.forEach(uri => {
		const doc = cts.doc(uri).root
		const entity = fn.head(cts.doc(uri).xpath('envelope/instance/node()[node-name(.) = ../info/title]')).toObject()
		const entityName = doc.xpath('envelope/instance/info/title/string()').toString();
		const entityId = entityName.toLowerCase()
		const modelNode = model.nodes[entityId];
		const labelField = modelNode.labelField;
		const label = (!!labelField) ? entity[labelField] : uri

		resp.nodes[uri] = {
			id: uri,
			uri: uri,
			entityName: entityName,
			label: label,
			entity: entity,
			edgeCounts: edgeCounts[uri] || {},
			prov: provHelper.getProv(uri),
			isConcept: false
		}
	})

	return resp
}

/**
 * This method takes a given array of uris and returns the # of edges
 * for each uri
 *
 * @param uris an array of uris
 */
function getEdgeCounts(uris) {
	let edgeCounts = {}
	const countSparql = `
	PREFIX rdf: <http://www.w3.org/2000/01/rdf-schema#>
	SELECT (str(?uri) as ?id) (REPLACE(str(?s), "#.+$", "") as ?from) (REPLACE(str(?p), "^.+#", "") as ?label) (REPLACE(str(?o), "#.+$", "") as ?to) (COUNT(?p) as ?count) where {
		?s ?p ?o.
		?uri rdf:hasId ?s.
		FILTER( ?uri IN ( ${uris.map(uri => `"${uri}"`).join(', ')} ) )
		FILTER( ?p NOT IN ( rdf:hasEntityType ) )
	}
	GROUP BY ?uri ?p
	ORDER BY DESC(?count)`

	sem.sparql(countSparql).toArray().forEach(t => {
		let ec = edgeCounts[t.id] || {}
		const edgeId = `${t.from}-${t.label}-${t.to}`.toLowerCase()
		ec[edgeId] = {
			from: t.from,
			to: t.to,
			label: t.label,
			count: t.count
		}
		edgeCounts[t.id] = ec
	})
	return edgeCounts
}

function runQuery(query, options) {
	const opts = options || {}
	const start = opts.start || 1
	const pageLength = opts.pageLength || 10
	const sort = opts.sort || 'default'
	console.log('sort', sort)
	if (sort === 'default') {
		let allUris = cts.uris(null, null, query)
		return {
			total: fn.count(allUris),
			uris: fn.subsequence(allUris, start, pageLength).toArray()
		}
	}

	const orderClause = (sort === 'mostConnected') ? 'ORDER BY DESC(?count)' : 'ORDER BY ASC(?count)'
	const countSparql = `
	PREFIX rdf: <http://www.w3.org/2000/01/rdf-schema#>
	SELECT ?uri (COUNT(?p) as ?count) where {
		?s ?p ?o.
		?uri rdf:hasId ?s.
		FILTER( ?p NOT IN ( rdf:hasEntityType ) )
	}
	GROUP BY ?uri
	${orderClause}`

	let response = sem.sparql(countSparql, null, null, sem.store(null, query))
	return {
		total: fn.count(response),
		uris: fn.subsequence(response, start, pageLength)
			.toArray()
			.map(t => t.uri.toString())
	}
}

exports.getEntities = getEntities;
exports.runQuery = runQuery
