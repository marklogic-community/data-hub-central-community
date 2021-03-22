'use strict';

const triplesLib = require('triplesLib.sjs');

var qtext;
var page;
var subjectsPerPage;
var linksPerSubject;
var sort;
var dedup;

let start = ((page - 1) * subjectsPerPage)
dedup = (dedup == 'on') ? 'on' : 'off';
sort = sort || 'default'
subjectsPerPage = subjectsPerPage ? subjectsPerPage : 10;
linksPerSubject = linksPerSubject ? linksPerSubject : 10;

/**
 * Find the distinct subjects based on their predicate frequency and query text.
 */
const op = require('/MarkLogic/optic');
const s = op.col('s');
const p = op.col('p');
const o = op.col('o');

let trip =
	  op.fromTriples([op.pattern(s, p, o)], null, null, { 'dedup': dedup })
	    .where(op.as('isIRI', op.sem.isIRI(s)));

if (qtext) {
	trip = trip.where(cts.wordQuery(qtext, ['case-insensitive']));
}

let countR = trip.groupBy(null, [op.count('count', s)]).result();
let count = fn.head(countR).count

trip = trip.groupBy(s, [op.count('pCount', p)]);

if (sort == 'most-connected') {
	trip = trip.orderBy(op.desc('pCount'));
} else if (sort == 'least-connected') {
	trip = trip.orderBy(op.asc('pCount'));
}

trip = trip.offset(start);

if (subjectsPerPage && subjectsPerPage > -1) {
	trip = trip.limit(subjectsPerPage);
}

let iris = [];
for (let item of trip.result()) {
	iris.push(item.s)
}

/**
 * Find the distinct links to these node filtered by the query text.
 */
const s2 = op.col('s');
const p2 = op.col('p');
const o2 = op.col('o');

let r2 = op.fromTriples([op.pattern(s2, p2, o2)], null, null, { 'dedup': dedup });

if (iris && iris.length > 0) {
	r2 = r2.where(op.in(s2, iris));
}
if (qtext && qtext != '') {
	r2 = r2.where(cts.wordQuery(qtext));
}

r2 = r2.where(op.as('isIRI', op.sem.isIRI(s2)));
r2 = (linksPerSubject && linksPerSubject > 0) ? r2.limit(linksPerSubject) : r2;

/**
 * Iterate the result set and create the nodes and edges to plot the graph.
 */
let nodesAndEdges = triplesLib.buildNodesAndEdges(r2.result());

/**
 * Build response payload.
 */
let resp = {
	page: page,
	subjectsPerPage: subjectsPerPage,
	linksPerSubject: linksPerSubject || 'all',
	nodes: nodesAndEdges.nodes,
	edges: nodesAndEdges.edges,
	total: count
}

resp
