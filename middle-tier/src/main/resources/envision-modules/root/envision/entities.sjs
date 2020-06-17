const provHelper = require('/envision/prov-helper.xqy');
const model = require('/envision/model.sjs');
const indexes = require('/envision/options.xqy');
const search = require('/MarkLogic/appservices/search/search');
const searchImpl = require('/MarkLogic/appservices/search/search-impl.xqy');
const sut = require('/MarkLogic/rest-api/lib/search-util.xqy');
const json = require('/MarkLogic/json/json.xqy');

/**
 * This method takes a given array of uris and returns the # of edges
 * for each uri
 *
 * @param uris an array of uris to get
 * @param opts an options object
 * Options:
 * 		start - start index for pagination
 * 		connectionLimit - # of connections to grab
 * 		labels - an array of edge labels to return, empty means all
 */
function getEntities(uris, opts) {
	let options = opts || {}
	let start = (!!options.start) ? options.start : 0
	let connectionLimit = (!!options.connectionLimit) ? options.connectionLimit : 10
	let labels = options.labels || []

	const resp = {
		nodes: {},
		edges: {}
	}
	if (uris.length < 1) {
		return resp
	}

	let labelFilter = ''
	if (labels && labels.length > 0) {
		labelFilter = `FILTER( ?lbl IN ( ${labels.map(label => `rdf:${label}`).join(', ')} ) )`
	}

	// this query gets all the edges for the given list of uris
	// with a limit applied to not return everything
	const sparql = `
		PREFIX rdf: <http://www.w3.org/2000/01/rdf-schema#>
		PREFIX fn: <http://www.w3.org/2005/xpath-functions#>

		select distinct
			# we use fromUri and toUri to determine which side is a concept
			?fromUri ?toUri

			# this returns the uri (for docs) or the id (for concepts) on the from side
			(fn:head((?fromUri, ?fromId)) as ?from)

			# this returns the uri (for docs) or the id (for concepts) on the to side
			(fn:head((?toUri, ?toId)) as ?to)

			# this is the id of the edge
			(fn:string-join((?from, ?lbl, ?to), "-") as ?id)

			# this is the label on the edge (right side of #)
			(fn:tail(fn:tokenize(?lbl, "#")) as ?label)

			# this is the entity|concept type of the from side (right side of the #)
			(fn:head(fn:tokenize(?fromId, "#")) as ?fromType)

			# this is the entity|concept type of the to side (right side of the #)
			(fn:head(fn:tokenize(?toId, "#")) as ?toType)

			# boolean for whether there is a concept or not
			(fn:not(fn:exists(?toUri) && fn:exists(?fromUri)) as ?isConcept) where {

			# this queries all the triples where the given uris on on the from side
			{
				?fromUri rdf:hasId ?fromId.
				?fromId ?lbl ?toId.
				?toId rdf:hasEntityType ?entityType.
				optional { ?toUri rdf:hasId ?toId }
				FILTER( ?fromUri IN ( ${uris.map(uri => `"${uri}"`).join(', ')} ) )
				${labelFilter}
			}

			# this queries all the triples where the given uris on on the to side
			UNION {
				?fromId ?lbl ?toId.
				?toUri rdf:hasId ?toId.
				?fromId rdf:hasEntityType ?entityType.
				optional { ?fromUri rdf:hasId ?fromId }
				FILTER( ?toUri IN ( ${uris.map(uri => `"${uri}"`).join(', ')} ) )
				${labelFilter}
			}
		}
		LIMIT ${connectionLimit}
		OFFSET ${start}`

	// prevent dupes for performance improvements
	const uriMap = {}
	uris.forEach(uri => uriMap[uri] = true)

	// build a cts.query that omits archived documents from smart mastering
	const archivedCollections = cts.collectionMatch('sm-*-archived').toArray()
	const query = cts.notQuery(cts.collectionQuery(archivedCollections))

	// run the sparql query while ignoring archived docs
	const triples = sem.sparql(sparql, null, null, sem.store(null, query))

	// iterate over these triples
	// add nodes for any concepts that are found
	// ensure uris both sides of the triple (from and to) are in our list of
	// uris to return. This allows us to grab any nodes for which we have edges, but
	// no node yet. Look below to see where we iterate over the uris to make nodes
	for (let t of triples) {
		resp.edges[t.id] = {
			from: t.from,
			to: t.to,
			id: t.id,
			label: t.label,
			fromType: t.fromType,
			toType: t.toType
		}

		if (t.isConcept) {
			// determine which "side" (from or to) has the concept
			if (fn.head(t.fromUri)) {
				// to side has concept
				resp.nodes[t.to] = {
					id: t.to,
					label: t.to.toString().replace(/(.+)#(.+)/, '$2'),
					entityName: model.getName(t.toType),
					isConcept: true,
					edgeCounts: {}
				}
			}
			else {
				// from side has concept
				resp.nodes[t.from] = {
					id: t.from,
					label: t.from.toString().replace(/(.+)#(.+)/, '$2'),
					entityName: model.getName(t.fromType),
					isConcept: true,
					edgeCounts: {}
				}
			}
		}
		else {
			// ensure uris from both sides of the triple are in the uris map
			if (t.to && t.to.length > 0) {
				uriMap[t.to] = true
			}
			if (t.from && t.from.length > 0) {
				uriMap[t.from] = true
			}
		}
	}

	// turn the uri map back into an array
	uris = Object.keys(uriMap)
	const edgeCounts = getEdgeCounts(uris)

	// iterate over the uris and create "nodes" for the graph ui
	// do this by opening the docs at each uri
	// then insert some additional metadata about each "node"
	uris.forEach(uri => {
		const doc = cts.doc(uri)

		// the actual entity
		let entity = fn.head(doc.xpath('*:envelope/*:instance/node()[local-name-from-QName(node-name(.)) = ../*:info/*:title]'))

		// use metadata from the model to grab the "label" out of the entity
		// the label is specified in envision's UI on the Modeler page
		const entityName = doc.xpath('*:envelope/*:instance/*:info/*:title/string()').toString();
		const entityId = entityName.toLowerCase()
		const modelNode = model.nodes[entityId] || {};
		const labelField = modelNode.labelField;

		// grab the label or default to the uri
		const label = (!!labelField) ? entity.xpath(`.//${labelField}/string()`) : uri

		// convert xml to json map
		if (entity instanceof Element) {
			entity = entity.xpath('node()').toArray().reduce((item, el) => {
				item[el.xpath('local-name(.)')] = el.xpath('string(.)')
				return item;
			}, {})
		}

		resp.nodes[uri] = {
			id: uri,
			uri: uri,
			entityName: entityName,
			label: label,
			entity: entity,
			edgeCounts: edgeCounts[uri] || {},

			// grab the DHF provenance data out of the jobs db
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
	const countSparql = `
	PREFIX rdf: <http://www.w3.org/2000/01/rdf-schema#>
	PREFIX fn: <http://www.w3.org/2005/xpath-functions#>
	SELECT DISTINCT
		(str(fn:head((?fromUri, ?s))) as ?x)
		?p
		(str(fn:head((?toUri, ?o))) as ?y)
		(REPLACE(str(?s), "#.+$", "") as ?from)
		(REPLACE(str(?p), "^.+#", "") as ?label)
		(REPLACE(str(?o), "#.+$", "") as ?to)
		(str(fn:lower-case(fn:string-join((?from, ?label, ?to), "-"))) as ?edgeId)
		where {
		{
			?s ?p ?o.
			?fromUri rdf:hasId ?s.
			optional { ?toUri rdf:hasId ?o }
			FILTER( ?fromUri IN ( ${uris.map(uri => `"${uri}"`).join(', ')} ) )
			FILTER( ?p NOT IN ( rdf:hasEntityType, rdf:hasId ) )
		}
		UNION
		{
			?s ?p ?o.
			?toUri rdf:hasId ?o.
			optional { ?fromUri rdf:hasId ?s }
			FILTER( ?toUri IN ( ${uris.map(uri => `"${uri}"`).join(', ')} ) )
			FILTER( ?p NOT IN ( rdf:hasEntityType, rdf:hasId ) )
		}
	}`

	const buildEdgeCount = (obj, key, t) => {
		let ex = obj[key] || {}
		const count = (ex[t.edgeId] && ex[t.edgeId].count) || 0
		ex[t.edgeId] = {
			from: t.from,
			to: t.to,
			label: t.label,
			count: count + 1
		}
		obj[key] = ex
		return obj
	}

	// build a cts.query that omits archived documents from smart mastering
	const archivedCollections = cts.collectionMatch('sm-*-archived').toArray()
	const query = cts.notQuery(cts.collectionQuery(archivedCollections))

	// run the sparql query while ignoring archived docs
	const trips = sem.sparql(countSparql, null, null, sem.store(null, query)).toArray()
	const edgeCounts = trips.reduce((obj, t) => {
		let newObj = buildEdgeCount(obj, t.x, t)
		newObj = buildEdgeCount(newObj, t.y, t)
		return newObj
	}, {})
	return edgeCounts
}

/**
 * Adds the index based sort to the structured query
 * @param query - a MarkLogic structured query: http://docs.marklogic.com/guide/search-dev/structured-query
 * @param sort - an object with property and sortDirection
 */
function addSortToQuery(query, sort) {
	const x = new NodeBuilder();
	x.startElement("query", "http://marklogic.com/appservices/search");
	if (sort.property && sort.sortDirection) {
		x.startElement('operator-state', 'http://marklogic.com/appservices/search');
		x.startElement('operator-name', 'http://marklogic.com/appservices/search');
		x.addText('sort');
		x.endElement();
		x.startElement('state-name', 'http://marklogic.com/appservices/search');
		const direction = sort.sortDirection === 'ascending' ? 'Asc' : 'Desc';
		x.addText(`${sort.property}${direction}`);
		x.endElement();
		x.endElement();
	}
	for (let y of query.xpath('*')) {
		x.addNode(y)
	}
	x.endElement();
	return x.toNode();
}

/**
 * runs the query
 * @param qtext - text query typed into the search box
 * @param query  - a MarkLogic structured query: http://docs.marklogic.com/guide/search-dev/structured-query
 * @param options - { start, pageLength, sort }
 */
function runQuery(qtext, query, options) {
	const opts = options || {}
	const start = opts.start || 1
	const pageLength = opts.pageLength || 10
	const rawSort = opts.sort || 'default'
	const sort = rawSort.startsWith('{') ? JSON.parse(rawSort) : rawSort
	const searchOptions = indexes.getSearchConfig()

	// inject the sort info into the structure query (if it's an index sort)
	query = addSortToQuery(query, sort);

	// combine the text query with the facets
	// this is using an internal MarkLogic function
	const newQuery = sut.makeStructuredQuery(query, qtext, searchOptions);

	// run the search.search and return the json
	// this is using an internal MarkLogic function for converting to json (sut.responseToJsonObject)
	return fn.head(
		sut.responseToJsonObject(
			search.resolve(newQuery, searchOptions, start, pageLength),
			'results'
		)
	).toObject();
}

function getValues(qtext, query, facetName) {
	const searchOptions = indexes.getSearchConfig()

	// combine the text query with the facets
	// this is using an internal MarkLogic function
	const newQuery = sut.makeStructuredQuery(query, qtext, searchOptions);
	const root = search.values(facetName, searchOptions, newQuery)
	return fn.head(xdmp.toJSON(json.transformToJsonObject(root, sut.buildValResultsConfig()))).root.toObject();
}

exports.getEntities = getEntities;
exports.runQuery = runQuery
exports.getValues = getValues
