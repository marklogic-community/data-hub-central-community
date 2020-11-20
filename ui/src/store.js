import Vue from 'vue';
import Vuex from 'vuex';

import authApi from './api/AuthApi';
import searchApi from './api/SearchApi';
import modelApi from './api/ModelApi';
import triplesApi from './api/TriplesApi';
import masteringApi from './api/MasteringApi';
import flowsApi from './api/FlowsApi';
import entitiesApi from './api/EntitiesApi';
import crudApi from './api/CRUDApi';
import axios from 'axios';
import * as _ from 'lodash';
import SearchApi from './api/SearchApi';
import md5 from 'md5'

Vue.use(Vuex);

const debug = true; //(process !== undefined) ? process.env.NODE_ENV !== "production" : true;
const isHosted = process.env.VUE_APP_IS_HOSTED === 'true'

const mastering = {
	namespaced: true,
	state: {
		page: 1,
		pageLength: 10,
		total: 0,
		totalUnread: 0,
		allNotifications: {},
		pagedNotifications: [],
		docs: {},
		blocks: {},
		mergedDoc: null
	},
	mutations: {
		setPagination(state, { page, pageLength, total, totalUnread }) {
			state.page = page || state.page
			state.total = _.isNumber(total) ? total : state.total
			state.totalUnread = _.isNumber(totalUnread) ? totalUnread : state.totalUnread
			state.pageLength = _.isNumber(pageLength) ? pageLength :  state.pageLength
		},
		setNotification(state, notification) {
			Vue.set(state.allNotifications, notification.meta.uri, notification)
		},
		setNotifications(state, notifications) {
			state.pagedNotifications = notifications
			notifications.forEach(n => {
				Vue.set(state.allNotifications, n.meta.uri, n)
			});
		},
		setBlocks(state, blocks) {
			state.blocks = Object.assign({}, state.blocks, blocks)
		},
		resetDocs(state) {
			state.docs = {}
		},
		setDoc(state, {uri, doc}) {
			Vue.set(state.docs, uri, doc)
		},
		setMergedDoc(state, doc) {
			state.mergedDoc = doc
		}
	},
	actions: {
		getNotification({ commit, dispatch }, uri) {
			return masteringApi.getNotification(uri).then(notification => {
				commit('setNotification', notification)
				dispatch('getBlocks', notification.uris)
			})
		},
		getNotifications({ state, commit }, { page, pageLength, extractions }) {
			const newPage = page || 1
			const newPageLength = pageLength || state.pageLength
			return masteringApi.getNotifications(newPage, newPageLength, extractions || []).then(result => {
				commit('setPagination', { page: newPage, pageLength: result.pageLength, total: result.total, totalUnread: result.totalUnread })
				commit('setNotifications', result.notifications)
			})
		},
		async updateNotification({ state, dispatch }, { uris, status }) {
			await masteringApi.updateNotification(uris, status)
			dispatch('getNotifications', { page: state.page })
		},
		getBlocks({ commit }, uris) {
			masteringApi.getBlocks(uris).then(blocks => {
				commit('setBlocks', blocks)
			})
		},
		block({ commit }, uris) {
			masteringApi.block(uris).then(blocks => {
				commit('setBlocks', blocks)
			})
		},
		unblock({ commit }, uris) {
			masteringApi.unBlock(uris).then(blocks => {
				commit('setBlocks', blocks)
			})
		},
		getDocs({ state, commit }, { uris, force }) {
			uris.forEach(uri => {
				if (force || !state.docs[uri]) {
					masteringApi.getDoc(uri).then(doc => {
						commit('setDoc', { uri, doc })
					})
				}
			})
		},
		merge({ commit }, { uris, flowName, stepNumber, preview }) {
			return masteringApi.merge(uris, flowName, stepNumber, preview).then(resp => {
				if (resp.success) {
					commit('setMergedDoc', resp.mergedDocument.value)
				}
			})
		},
		unmerge({ commit }, doc) {
			return masteringApi.unmerge(doc)
		}
	}
}

const auth = {
	namespaced: true,
	state: {
		initialized: false,
		authenticated: false,
		username: undefined,
		authorities: [],
		profile: undefined
	},
	mutations: {
		isInitialized(state, { initialized }) {
			state.initialized = initialized;
		},
		isAuthenticated(state, { authenticated }) {
			state.authenticated = authenticated;
			if (!authenticated) {
				state.username = undefined;
				state.profile = undefined;
			}
		},
		async loggedIn(state, { username, authorities }) {
			state.authenticated = true;
			state.username = username;
			state.authorities = authorities;
			await this.$ws.connect()
		},
		loggedOut(state) {
			state.authenticated = false;
			state.username = undefined;
			state.profile = undefined;
			this.$ws.disconnect()
		},
		setProfile(state, { profile }) {
			state.profile = profile || {};
			this.$logRocket.identify(profile.email, {
				...profile,
				environment: process.env.NODE_ENV
			})
		}
	},
	actions: {
		init({ commit, dispatch }) {
			return authApi.status().then(async result => {
				if (result.isError) {
					// error
					return result;
				} else {
					commit('isInitialized', { initialized: true });
					commit('isAuthenticated', {
						authenticated: result.authenticated
					});
					if (result.authenticated) {
						await dispatch(
							'loggedIn',
							{
								username: result.username,
								authorities: result.authorities
							},
							{ root: true }
						);
						return dispatch('getProfile')
					}
				}
			});
		},
		getProfile({ commit }) {
			authApi.profile().then(result => {
				if (result.isError) {
					// error
					return result;
				} else {
					commit('setProfile', {
						profile: result
					});
				}
			});
		},
		login({ dispatch }, { user, pass }) {
			return authApi.login(user, pass).then(result => {
				if (result.isError) {
					// error
					return result;
				} else {
					dispatch(
						'loggedIn',
						{
							username: user,
							authorities: result.authorities
						},
						{ root: true }
					);
					dispatch('getProfile')
				}
			});
		},
		signup({ dispatch }, { email, name, password }) {
			return authApi.signup(email, name, password).then(result => {
				if (result.isError) {
					// error
					return result;
				}
			});
		},
		cancelLogin() {
			return new Promise(resolve => {
				resolve();
			});
		},
		logout({ dispatch }) {
			localStorage.removeItem('access_token')
			delete axios.defaults.headers.common['Authorization'];
			dispatch('loggedOut', null, { root: true });
		},
		initProfile({ dispatch }, payload) {
			return authApi.profile(payload).then(result => {
				if (result.isError) {
					// error
					return result;
				} else {
					dispatch('getStatus');
				}
			});
		}
	}
};

const explore = {
	namespaced: true,
	state: {
		docs: {},
		edges: {},
		nodes: {},
		total: 0,
		page: 1,
		database: 'final',
		pageLength: 10,
		results: [],
		qtext: null,
		sort: 'default',
		facets: {},
		activeFacets: {}
	},
	mutations: {
		setDocs(state, response) {
			if (response) {
				state.total = response.total
				state.page = response.page || Math.ceil (response.start / response['page-length']) // different server response between staging/final
				state.pageLength = response.pageLength || response['page-length']
				state.colors = {};
				state.edges = response.edges
				state.nodes = response.nodes
				state.results = response.results
				state.facets = response.facets
			}
		},
		addDocs(state, docs) {
			if (docs) {
				state.nodes = _.clone(Object.assign(state.nodes, docs.nodes))
				state.edges = _.clone(Object.assign(state.edges, docs.edges))
			}
		},
		removeDoc(state, uri) {
			Vue.delete(state.nodes, uri)
		},
		setSort(state, sort) {
			state.sort = sort
		},
		setPage(state, page) {
			state.page = page
		},
		addEntity(state, { entity }) {
			if (state.entities.indexOf(entity) < 0) {
				state.entities.push(entity)
			}
		},
		removeEntity(state, { entity }) {
			let idx = state.entities.indexOf(entity)
			if (idx >= 0) {
				state.entities.splice(idx, 1)
			}
		},
		setEntities(state, entities) {
			state.entities = entities
		},
		setText(state, { qtext }) {
			if (qtext !== undefined) {
				state.qtext = qtext;
			}
		},
		setFacetValue(state, { facetName, facets }) {
			if (facets) {
				let values = state.facets[facetName].facetValues;

				if (facets) {
					facets.forEach(function(newFacetValue) {
						//check if not existing
						for (let i = 0; i < values.length; i++) {
							if (values[i].name === newFacetValue._value) {
								Vue.set(state.facets[facetName], 'displayingAll', true);
								return;
							}
						}
						values.push({
							name: newFacetValue._value || 'blank',
							count: facetName.endsWith('*')?'':newFacetValue.frequency,
							value: newFacetValue._value || ''
						});
					});
				}

				Vue.set(state.facets[facetName], 'facetValues', values);
			} else {
				Vue.set(state.facets[facetName], 'displayingAll', true);
			}
		},
		setFacetDisplayingAll(state, { facetName }) {
			Vue.set(state.facets[facetName], 'displayingAll', true);
		},
		setDatabase(state, database) {
			state.database = database
		},
		clearActiveFacets(state) {
			state.activeFacets = {}
		},
		toggleActiveFacet(state, { facet, type, value, negated }) {
			const activeFacet = state.activeFacets[facet];
			if (activeFacet) {
				const activeValue = activeFacet.values.filter(facetValue => {
					return facetValue.value === value;
				});
				if (activeValue.length) {
					activeFacet.values = activeFacet.values.filter(facetValue => {
						return facetValue.value !== value;
					});
				} else {
					activeFacet.values.push({
						value: value,
						negated: negated
					});
				}
				Vue.set(state.activeFacets, facet, activeFacet)
			} else {
				Vue.set(state.activeFacets, facet, {
					type: type,
					values: [{ value: value, negated: negated }]
				})
			}
		}
	},
	actions: {
		search({ commit, state }) {
			return searchApi
				.getEntities(state.database, state.activeFacets, state.qtext, state.page, state.pageLength, state.sort)
				.then(response => {
					if (response) {
						commit('setDocs', response)
					}
				})
		},
		toggleFacet({ commit, dispatch }, { facet, type, value, negated }) {
			commit('toggleActiveFacet', { facet, type, value, negated });
			dispatch('search')
		},
		showMore({ commit, state }, facetName) {
			return SearchApi.getValues(state.database, state.activeFacets, state.qtext, facetName)
				.then(response => {
					let newFacets = response && response['values-response'] && response['values-response']['distinct-value']
					if (newFacets) {
						commit('setFacetValue', { facetName, facets: newFacets })
					} else {
						commit('setFacetDisplayingAll', { facetName })
					}
				})
		},
		getRelatedEntities({ commit }, { uri, label, page, pageLength }) {
			return searchApi
				.getRelatedEntities({ uri, label, page, pageLength })
				.then(response => {
					commit('addDocs', response)
				})
		},
		getEntitiesRelatedToConcept({ commit }, { concept, page, pageLength }) {
			return searchApi
				.getEntitiesRelatedToConcept({ concept, page, pageLength })
				.then(response => {
					commit('addDocs', response)
				})
		},
		getHistory({ commit }, uri) {
			return masteringApi.getHistory(uri)
				.then(response => {
					commit('addDocs', response)
				})
		},
		unmerge({ commit }, doc) {
			return masteringApi.unmerge(doc)
				.then(response => {
					commit('addDocs', response)
					commit('removeDoc', doc)
				})
		}
	}
};

const model = {
	namespaced: true,
	state: {
		models: [],
		model: null,
		activeIndexes: []
	},
	mutations: {
		setModel(state, model) {
			state.model = model;
		},
		setModels(state, models) {
			state.models = models;
		},
		setActiveIndexes(state, indexes) {
			state.activeIndexes = indexes
		},
		setModelNewName(state, data) {
			let myModel = data.model
			myModel.name = data.newname
			state.model = myModel
		}
	},
	actions: {
		async init({ dispatch }) {
			await dispatch('getAll')
			await dispatch('getModel')
		},
		async getActiveIndexes({ commit }) {
			let activeIndexes = await modelApi.getActiveIndexes()
			commit('setActiveIndexes', activeIndexes)
		},
		async getAll({ commit }) {
			let models = await modelApi.getAllModels()
			commit('setModels', models);
		},
		async getModel({ state, commit, dispatch }) {
			try {
				let model = await modelApi.view();
				if (model && state.models.findIndex(m => m.name === model.name) >= 0) {
					commit('setModel', model);
				}
				else if (state.models.length > 0) {
					await dispatch('save', state.models[0])
				}
			}
			catch(err) {
				if (state.models.length > 0) {
				await dispatch('save', state.models[0])
			}
			}
		},
		async save({ commit, dispatch }, data) {
			commit('setModel', data);
			await modelApi.save(data);
			return dispatch('getAll');
		},
		async delete({ state, dispatch }, data) {
			await modelApi.deleteModel(data);
			dispatch('save', state.models.filter(m => m.name !== data.name)[0]);
		},
		async rename({ commit, dispatch }, data) {
			commit('setModelNewName', data);

			await modelApi.rename(data);
			return dispatch('getAll');
		}
	}
};

const triples = {
	namespaced: true,
	state: {
		nodes: {},
		edges: {},
		total: 0,
		page: 1,
		subjectsPerPage: 1,
		linksPerSubject: 10,
		maxRelated: 10,
		qtext: null
	},
	mutations: {
		setDocs(state, response) {
			if (response) {
				state.total = response.total
				state.page = response.page
				state.subjectsPerPage = response.subjectsPerPage
				state.linksPerSubjects = response.linksPerSubjects
				state.nodes = response.nodes
				state.edges = response.edges
			}
		},
		addDocs(state, response) {
			if (response) {
				state.nodes = Object.assign({}, state.nodes, response.nodes)
				state.edges = Object.assign({}, state.edges, response.edges)
			}
		},
		setNodes(state, nodes) {
			state.nodes = nodes
		},
		setEdges(state, edges) {
			state.edges = edges
		},
		setPage(state, page) {
			state.page = page
		},
		setSubjectsPerPage(state, subjectsPerPage) {
			state.subjectsPerPage = subjectsPerPage
		},
		setLinksPerSubject(state, linksPerSubject) {
			state.linksPerSubject = linksPerSubject
		},
		setMaxRelated(state, maxRelated) {
			state.maxRelated = maxRelated
		},
		setText(state, { qtext }) {
			if (qtext !== undefined) {
				state.qtext = qtext;
			}
		}
	},
	actions: {
		browse({ commit, state }, { database, sort }) {
			return triplesApi
				.getTriples(state.qtext, state.page, state.subjectsPerPage, state.linksPerSubject, database, sort)
				.then(response => {
					if (response) {
						commit('setDocs', response)
					}
				});
		},
		getRelated({ commit, state }, { item, itemId, isIRI, database, predicate }) {
			return triplesApi
				.getRelated(item, itemId, isIRI, database, state.maxRelated, state.qtext, predicate)
				.then(response => {
					if (response) {
						commit('addDocs', response)
					}
				})
		}
	}
};

const crud = {
	namespaced: true,
	actions: {
		metadata(context, { uri, db }) {
			return crudApi.metadata(uri, db)
		},
		doc(context, { uri, db }) {
			return crudApi.doc(uri, db)
		}
	}
}

const flows = {
	namespaced: true,
	state: {
		entities: [],
		flows: {}
	},
	mutations: {
		setFlows(state, flows) {
			state.flows = flows.reduce((output, flow) => {
				output[flow.name] = flow
				return output
			}, {})
		},
		setFlow(state, flow) {
			Vue.set(state.flows, flow.name, flow)
		},
		deleteFlow(state, flowName) {
			Vue.delete(state.flows, flowName)
		},
		setFlowSteps(state, {flowName, steps}) {
			Vue.set(state.flows[flowName], 'steps', steps)
		},
		setEntities(state, entities) {
			//state.entities = entities
			state.entities = entities.reduce((output, entity) => {
				output[entity.info.title] = {
					properties: entity.definitions.definitions[entity.info.title].properties,
					filename: entity.filename,
					info: entity.info
				}
				return output
			}, {})
		}
	},
	actions: {
		getFlows({ commit }) {
			return flowsApi.getFlows().then(result => {
				commit('setFlows', result)
			})
		},
		getFlow({ rootState, commit }, flowName) {
			flowName = flowName || md5(rootState.auth.username)
			return flowsApi.getFlow(flowName).then(result => {
				return commit('setFlow', result)
			})
		},
		async saveFlow({ commit }, flow) {
			await flowsApi.saveFlow(flow)
			return flowsApi.getFlow(flow.name).then(result => {
				return commit('setFlow', result)
			})
		},
		async deleteFlow({ commit }, flowName) {
			await flowsApi.deleteFlow(flowName)
			commit('deleteFlow', flowName)
		},
		getEntities({ commit }) {
			return entitiesApi.getEntities().then(result => {
				commit('setEntities', result)
			})
		}
	}
}

export default new Vuex.Store({
	strict: debug,
	state: {
		initialized: false,
		isHosted: process.env.VUE_APP_IS_HOSTED === 'true'
	},
	mutations: {
		isInitialized(state, { initialized }) {
			state.initialized = initialized;
		}
	},
	actions: {
		init({ commit, dispatch }) {
			return dispatch('auth/init').then(function() {
				commit('isInitialized', { initialized: true });
			});
		},
		loggedIn({ commit, dispatch }, payload) {
			commit('auth/loggedIn', payload);
			dispatch('model/init').then(() => {
				dispatch('mastering/getNotifications', {})
			})
		},
		loggedOut({ commit }, payload) {
			commit('auth/loggedOut', payload);
		}
	},
	modules: {
		auth,
		explore,
		model,
		triples,
		mastering,
		crud,
		flows
	}
});
