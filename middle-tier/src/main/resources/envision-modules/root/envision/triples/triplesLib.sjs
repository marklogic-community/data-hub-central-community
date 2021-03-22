'use strict';

/**
 * Create a title based on the subject IRI.
 * @param subject - Subject of a Triple
 * @returns a string representation of a title. Anything after the hash mark of a IRI if present.
 */
function createTitle(subject) {
	let label = subject.toString()
	if (label.match('#')) {
		label = label.split('#')[1]
	}
	return label
}

/**
 * Creates a label for a node. If a RDF Schema label is available it is used.
 * @param subject - Subject of a Triple
 * @returns a string representation of a label.
 */
function createLabel(subject) {
	let rdfLabel = fn.head(sem.sparql(`
	SELECT DISTINCT ?label
	WHERE {
		 ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label
	}
	LIMIT 1
 `, { 's': (sem.isIRI(subject)) ? sem.iri(subject) : subject}))

	let label = null;
	if (rdfLabel != null) {
		label = rdfLabel.label;
	} else {
		label = createTitle(subject)
		if (label.match('/')) {
			const splits = label.split('/')
			label = splits[splits.length - 1]
		}
	}

	if (label.length <= 15) {
		return label
	}

	return (label) ? fn.substring(label, 0, 15)  + '...' : '';
}

/**
 * Finds all predicates for a given subject.
 * @param subject - Subject of a Triple
 * @returns a distinct list of predicates for a given subject.
 */
function getPredicates(subject) {
	const allPredicatesQuery = `
		SELECT DISTINCT ?p
		where {
			?s ?p ?o .
		}
	`
	let bindings = {
		's': subject
	}

	let result = sem.sparql(allPredicatesQuery, bindings);
	let predicates = [];
	for (let triple of result) {
		predicates.push(triple.p);
	}

	return predicates;
}

/**
 * Builds a set of Nodes and Edges for a given sequence of triples.
 * @param triples - A Sequence of Semantic Triples.
 * @returns an object containing the nodes and edges to be plotted on the Web UI.
 */
function buildNodesAndEdges(triples) {
	let nodes = {}
	let edges = {}

	for (let triple of triples) {

		const fromId = xdmp.md5(triple.s)
		nodes[fromId] = {
			'id': fromId,
			'label': createLabel(triple.s),
			'title': createTitle(triple.s),
			'orig': triple.s,
			'isIRI': sem.isIRI(triple.s)
		}
		nodes[fromId].predicates = getPredicates(triple.s)

		const toId = sem.isIRI(triple.o) ? xdmp.md5(triple.o) : xdmp.md5(`${triple.s}-${triple.p}-${triple.o}`)
		nodes[toId] = {
			'id': toId,
			'label': createLabel(triple.o),
			'title': createTitle(triple.o),
			'orig': triple.o,
			'isIRI': sem.isIRI(triple.o)
		};

		if (sem.isIRI(triple.o)) {
			nodes[toId].predicates = getPredicates(triple.o);
		}

		const edgeId = xdmp.md5(`${triple.s}-${triple.p}-${triple.o}`)
		edges[edgeId] = {
			'id': edgeId,
			'label': createLabel(triple.p),
			'title': createTitle(triple.p),
			'from': fromId,
			'to': toId,
			'orig': triple.p,
			'isIRI': sem.isIRI(triple.p)
		};
	}

	return {
		'nodes': nodes,
		'edges': edges
	};
}

module.exports = {
	buildNodesAndEdges: buildNodesAndEdges
};
