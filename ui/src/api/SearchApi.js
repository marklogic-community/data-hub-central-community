import axios from 'axios';
const CancelToken = axios.CancelToken;

export default {
	name: 'SearchApi',

	cancelEntities: null,
	getFacets(searchType, qtext, activeFacets) {
		searchType = searchType !== undefined ? searchType : 'all';
		qtext = qtext !== undefined ? qtext : '';

		var facets = Object.keys(activeFacets || {}).map(function(facetName) {
			var constraintType = activeFacets[facetName].type;
			if (constraintType && constraintType.substring(0, 3) === 'xs:') {
				constraintType = 'range';
			}
			return {
				type: 'selection',
				constraint: facetName,
				constraintType: constraintType,
				mode: 'and',
				value: activeFacets[facetName].values.map(function(facetValue) {
					if (facetValue.negated) {
						return { not: facetValue.value };
					} else {
						return facetValue.value;
					}
				})
			};
		});

		return axios.post('/api/search/' + searchType, {
			filters: {
				and: [
					{
						type: 'queryText',
						value: qtext
					}
				].concat(facets)
			},
			options: {
				start: 1,
				pageLength: 0
			}
		}).then(response => {
			return { response: response.data };
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},
	getResults(searchType, qtext, activeFacets, start, pageLength) {
		searchType = searchType !== undefined ? searchType : 'all';
		qtext = qtext !== undefined ? qtext : '';
		start = start || 1;
		pageLength = pageLength || 10;

		var facets = Object.keys(activeFacets || {}).map(function(facetName) {
			var constraintType = activeFacets[facetName].type;
			if (constraintType && constraintType.substring(0, 3) === 'xs:') {
				constraintType = 'range';
			}
			return {
				type: 'selection',
				constraint: facetName,
				constraintType: constraintType,
				mode: 'and',
				value: activeFacets[facetName].values.map(function(facetValue) {
					if (facetValue.negated) {
						return { not: facetValue.value };
					} else {
						return facetValue.value;
					}
				})
			};
		});

		return axios.post('/api/search/' + searchType, {
			filters: {
				and: [
					{
						type: 'queryText',
						value: qtext
					}
				].concat(facets)
			},
			options: {
				start: start,
				pageLength: pageLength
			}
		}).then(response => {
				return { response: response.data };
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},
	suggest(searchType, ptext, activeFacets) {
		searchType = searchType !== undefined ? searchType : 'all';
		ptext = ptext !== undefined ? ptext : '';

		/*var facets =*/ Object.keys(activeFacets || {}).map(function(facetName) {
			var constraintType = activeFacets[facetName].type;
			if (constraintType && constraintType.substring(0, 3) === 'xs:') {
				constraintType = 'range';
			}
			return {
				type: 'selection',
				constraint: facetName,
				constraintType: constraintType,
				mode: 'and',
				value: activeFacets[facetName].values.map(function(facetValue) {
					if (facetValue.negated) {
						return { not: facetValue.value };
					} else {
						return facetValue.value;
					}
				})
			};
		});

		// TODO: expose /api/search/{type}/suggest in middle-tier, and call that
		//			 as /v1/suggest doesn't understand search filters
		return axios.get(
			'/v1/suggest?options=' +
				searchType +
				'&partial-q=' +
				encodeURIComponent(ptext)
			).then(response => response.data)
			.catch(error => {
				console.error('error:', error);
				return error;
			});
	},
	getEntities(entities, qtext, page, pageLength, sort) {
		qtext = qtext !== undefined ? qtext : '';

		if (this.cancelEntities) {
			this.cancelEntities()
		}

		return axios.post(
			'/api/explore/entities/',
			{
				qtext,
				entities,
				page,
				pageLength,
				sort
			},
			{
				cancelToken: new CancelToken(c => this.cancelEntities = c)
			}
		)
		.then(response => {
			this.cancelEntities = null
			return response.data
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},
	getRelatedEntities({ uri, label, page, pageLength }) {
		return axios.post('/api/explore/related-entities/', { uri, label, page, pageLength })
		.then(response => response.data)
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	}
};
