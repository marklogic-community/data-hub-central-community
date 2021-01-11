var qtext;
var page;
var subjectsPerPage;
var linksPerSubject;
var sort;

let start = ((page - 1) * subjectsPerPage)
let queries = []

if (qtext && qtext !== '') {
	queries.push(cts.wordQuery(qtext));
}

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

let nodes = {}
let edges = {}

let textFilter = ""
if (qtext && qtext !== '') {
	textFilter = `
	filter (
		cts:contains(?s, cts:word-query(("${qtext}"), "case-insensitive")) ||
		cts:contains(?p, cts:word-query(("${qtext}"), "case-insensitive")) ||
		cts:contains(?o, cts:word-query(("${qtext}"), "case-insensitive"))
	  )
	`
}

let count = sem.sparql(`
PREFIX cts: <http://marklogic.com/cts#>
SELECT distinct (COUNT(?s) AS ?count) where {
	?s ?p ?o
	filter isIRI(?s)
	${textFilter}
}
`).toArray()[0].count

sort = sort || 'DESC'

let subjectQuery = `
PREFIX cts: <http://marklogic.com/cts#>
SELECT distinct ?s (COUNT(?p) AS ?count) where {
	?s ?p ?o
	filter isIRI(?s)
	${textFilter}
}
GROUP BY ?s
ORDER BY ${sort}(?count)
LIMIT ${subjectsPerPage}
OFFSET ${start}
`
let iris = sem.sparql(subjectQuery)
  .toArray()
  .map(t => t.s)

let limit = ''
if (linksPerSubject > 0) {
	limit = `LIMIT ${linksPerSubject}`
}

sem.sparql(`
	PREFIX cts: <http://marklogic.com/cts#>
	SELECT distinct ?s ?o ?p
		where {
		?s ?p ?o .
		filter(?s = ?x)
		${textFilter}
	}
	${limit}
`, { x: iris })
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

			const allPredicatesQuery = `
			PREFIX cts: <http://marklogic.com/cts#>
			SELECT distinct ?p
			where {
				?s ?p ?o
			}
			`
			nodes[fromId].predicates = sem.sparql(allPredicatesQuery, { s: t.s })
				.toArray()
				.map(t => t.p)
		}

		const toId = sem.isIRI(t.o) ? xdmp.md5(t.o) : xdmp.md5(`${t.s}-${t.p}-${t.o}`)
		nodes[toId] = {
			id: toId,
			label: createLabel(t.o),
			title: createTitle(t.o),
			orig: t.o,
			isIRI: sem.isIRI(t.o)
		}

		const edgeId = xdmp.md5(`${t.s}-${t.p}-${t.o}`)
		edges[edgeId] = {
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
	page: page,
	subjectsPerPage: subjectsPerPage,
	linksPerSubject: linksPerSubject || 'all' ,
	nodes: nodes,
	edges: edges,
	total: count
}
resp
