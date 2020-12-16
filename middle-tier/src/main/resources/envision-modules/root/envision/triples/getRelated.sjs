var item;
var itemId;
var isIRI;
var qtext;
var predicate;
var maxRelated;

function createTitle(s) {
	let label = s.toString()
	if (label.match('#')) {
		label = label.split('#')[1]
	}
	return label
}

function createLabel(s) {
	let label = createTitle(s)
	if (label.match('/')) {
		const splits = label.split('/')
		label = splits[splits.length - 1]
	}
	if (label.length <= 15) {
		return label
	}
	return label.slice(0, 15) + '...'
}

function getPredicates(iri) {
	const allPredicatesQuery = `
	PREFIX cts: <http://marklogic.com/cts#>
	SELECT distinct ?p
	where {
		?s ?p ?o .
		?x ?p ?o .
		filter(?s = ?x)
	}
	`
	let bindings = {
		x: iri
	}
	return sem.sparql(allPredicatesQuery, bindings)
		.toArray().map(t => t.p)
}

let nodes = {}
let edges = {}

let textFilter = ""
if (qtext && qtext !== '' && predicate == null) {
	textFilter = `
	filter (
		cts:contains(?s, cts:word-query(("${qtext}"), "case-insensitive")) ||
		cts:contains(?p, cts:word-query(("${qtext}"), "case-insensitive")) ||
		cts:contains(?o, cts:word-query(("${qtext}"), "case-insensitive"))
	)
	`
}

let predFilter = ""
if (predicate != null) {
	predFilter = `
	filter (?p = <${predicate}>)
	`
}

let limit = ''
if (maxRelated > 0) {
	limit = `LIMIT ${maxRelated}`
}


let bindings = {
    x: isIRI ? sem.iri(item) : item
}
const query = `
PREFIX cts: <http://marklogic.com/cts#>
SELECT * where {
	?s ?p ?o .
	FILTER (?o = ?x || ?s = ?x)
	FILTER isIRI(?s)
	${textFilter}
	${predFilter}
}
${limit}
`
let iris = sem.sparql(query, bindings)
  .toArray()
  .forEach(t => {
	const fromId = xdmp.md5(t.s)
	if (!nodes[fromId]) {
		nodes[fromId] = {
			id: fromId,
			label: createLabel(t.s),
			title: createTitle(t.s),
			orig: t.s,
			isIRI: sem.isIRI(t.s)
		}

		nodes[fromId].predicates = getPredicates(t.s)
	}

	const toId = sem.isIRI(t.o) ? xdmp.md5(t.o) : xdmp.md5(`${t.s}-${t.p}-${t.o}`)
	if (!nodes[toId]) {
		nodes[toId] = {
			id: toId,
			label: createLabel(t.o),
			title: createTitle(t.o),
			orig: t.o,
			isIRI: sem.isIRI(t.o)
		}

		if (sem.isIRI(t.o)) {
			nodes[toId].predicates = getPredicates(t.o)
		}
	}

	const edgeId = xdmp.md5(`${t.s}-${t.p}-${t.o}`)
    edges[edgeId] ={
		id: edgeId,
		label: createLabel(t.p),
		title: createTitle(t.p),
		from: fromId,
		to: toId,
		orig: t.p,
		isIRI: sem.isIRI(t.p)
	}
  })

let resp = {
	nodes: nodes,
	edges: edges,
}
resp
