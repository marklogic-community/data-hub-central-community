'use strict'

const ents = require('/entities.sjs');

var qtext;
var page;
var pageLength;
var entities;
var sort;

entities = entities.toObject();
let collectionQuery = cts.trueQuery()
if (entities.length > 0) {
	collectionQuery = cts.collectionQuery(entities)
}

let wordQuery = cts.trueQuery()
if (qtext && qtext !== '') {
	wordQuery = cts.wordQuery(qtext)
}

const archivedCollections = cts.collectionMatch('sm-*-archived').toArray()
let finalCollectionQuery = cts.andNotQuery(
	collectionQuery,
	cts.collectionQuery(archivedCollections)
)
let query =
	cts.andQuery([
		finalCollectionQuery,
		cts.orQuery([
			cts.jsonPropertyScopeQuery("instance", wordQuery),
			cts.elementQuery(fn.QName('http://marklogic.com/entity-services', 'instance'), wordQuery)
		])
	])

let start = ((page - 1) * pageLength);

const sorted = ents.runQuery(query, {start, pageLength, sort})

const allowedEntities = entities
const connectionLimit = 100
const results = ents.getEntities(sorted.uris, { connectionLimit, allowedEntities, sort })

const result = {
	page: page,
	total: sorted.total,
	pageLength: pageLength,
	...results
}

result;
