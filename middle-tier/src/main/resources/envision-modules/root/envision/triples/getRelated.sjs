'use strict';

const triplesLib = require('triplesLib.sjs');

var item;
var itemId;
var isIRI;
var qtext;
var predicate;
var maxRelated;
var filterText;

var isString = typeof item == 'string';

/**
 * Find the distinct triples based on the user item being in either the subject or object.
 * Filter the results also based on the user's query text.
 */
let textFilter = ""
if (qtext && qtext !== '' && predicate == null && filterText == true) {
	textFilter = `
	filter (
		cts:contains(?s, cts:word-query(("${qtext}"), "case-insensitive")) ||
		cts:contains(?p, cts:word-query(("${qtext}"), "case-insensitive")) ||
		cts:contains(?o, cts:word-query(("${qtext}"), "case-insensitive"))
	)
	`
}

let predicateBind = '';
if (predicate != null) {
	predicateBind = 'BIND(<' + predicate + '> as ?p)';
}

let limit = '';
if (maxRelated > 0) {
	limit = `LIMIT ${maxRelated}`
}

let bindItem = item;
if (isIRI) {
	bindItem = '<' + item + '>'
} else if (isString) {
	bindItem = '"' + item + '"';
}

const query = `
PREFIX cts: <http://marklogic.com/cts#>
SELECT DISTINCT ?s ?p ?o {
    {
      SELECT ?s ?p ?o {
        BIND(${bindItem} as ?o)
        ${predicateBind}
        ?s ?p ?o
        FILTER isIri(?s)
      }
    } UNION {
       SELECT * {
         BIND(${bindItem} as ?s)
         ${predicateBind}
         ?s ?p ?o
       }
    }
	${textFilter}
}
${limit}
`

/**
 * Iterate the result set and create the nodes and edges to plot the graph.
 */
let resp = triplesLib.buildNodesAndEdges(sem.sparql(query));

resp
