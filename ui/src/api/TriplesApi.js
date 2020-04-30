import axios from 'axios'
const CancelToken = axios.CancelToken

export default {
	name: 'TriplesApi',

	cancelTriples: null,
	cancelRelated: null,
	getTriples(qtext, page, subjectsPerPage, linksPerSubject, database, sort) {
		qtext = qtext !== undefined ? qtext : ''

		if (this.cancelTriples) {
			this.cancelTriples()
		}

		return axios.post(
			'/api/triples/browse',
			{
				qtext: qtext,
				page: page,
				subjectsPerPage: subjectsPerPage,
				linksPerSubject: linksPerSubject,
                database: database,
                sort: sort
			},
			{
				cancelToken: new CancelToken(c => this.cancelTriples = c)
			}
		)
		.then(response => {
			this.cancelTriples = null
			return response.data
		})
		.catch(error => {
			console.error('error:', error)
			return error
		})
    },
    getRelated(item, itemId, isIRI, database, maxRelated, qtext, predicate) {
        if (this.cancelRelated) {
			this.cancelRelated()
        }
        
		return axios.post(
			'/api/triples/related',
			{
                item: item,
                itemId: itemId,
				isIRI: isIRI,
				maxRelated: maxRelated,
				database: database,
				qtext: qtext,
				predicate: predicate
			},
			{
				cancelToken: new CancelToken(c => this.cancelRelated = c)
			}
		)
		.then(response => {
			this.cancelRelated = null
			return response.data
		})
		.catch(error => {
			console.error('error:', error)
			return error
		})
	}
}
