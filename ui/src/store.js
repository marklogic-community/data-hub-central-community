import Vue from 'vue';
import Vuex from 'vuex';

import authApi from './api/AuthApi';
import searchApi from './api/SearchApi';
import modelApi from './api/ModelApi';
import triplesApi from './api/TriplesApi';
import masteringApi from './api/MasteringApi';
import axios from 'axios';
import * as _ from 'lodash';

Vue.use(Vuex);

const debug = true; //(process !== undefined) ? process.env.NODE_ENV !== "production" : true;

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
			if (state.needsInstall) {
				return
			}
			return masteringApi.getNotifications(newPage, newPageLength, extractions || []).then(result => {
				commit('setPagination', { page: newPage, pageLength: result.pageLength, total: result.total, totalUnread: result.totalUnread })
				commit('setNotifications', result.notifications)
			})
		},
		async updateNotification({ state, dispatch }, { uris, readStatus, mergeStatus, blockStatus }) {
			await masteringApi.updateNotification(uris, readStatus, mergeStatus, blockStatus)
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
		getDocs({ commit }, uris) {
			uris.forEach(uri => {
				masteringApi.getDoc(uri).then(doc => {
					commit('setDoc', { uri, doc })
				})
			})
		},
		merge({ commit }, { uris, flowName, stepNumber, preview }) {
			return masteringApi.merge(uris, flowName, stepNumber, preview).then(resp => {
				console.log('resp', resp)
				// console.log('commit', commit)
				commit('setMergedDoc', resp.mergedDocument.value)
			})
		},
		unmerge({ commit }, doc) {
			return masteringApi.unmerge(doc)
				// .then(response => {
				// 	// commit('addDocs', response)
				// 	// commit('removeDoc', doc)
				// })
		}
	}
}

const auth = {
	namespaced: true,
	state: {
		initialized: false,
		authenticated: false,
		username: undefined,
		profile: undefined,
		needsInstall: false,
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
		loggedIn(state, { username, needsInstall }) {
			state.authenticated = true;
			state.username = username;
			state.needsInstall = needsInstall;
		},
		loggedOut(state) {
			state.authenticated = false;
			state.username = undefined;
			state.profile = undefined;
		},
		setProfile(state, { profile }) {
			state.profile = profile || {};
		},
		setNeedsInstall(state, { needsInstall }) {
			state.needsInstall = needsInstall;
		}
	},
	actions: {
		init({ commit, dispatch }) {
			return authApi.status().then(result => {
				if (result.isError) {
					// error
					return result;
				} else {
					commit('isInitialized', { initialized: true });
					commit('isAuthenticated', {
						authenticated: result.authenticated
					});
					if (result.authenticated) {
						dispatch(
							'loggedIn',
							{
								username: result.username,
								needsInstall: result.needsInstall
							},
							{ root: true }
						);
						dispatch('getProfile')
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
							needsInstall: result.needsInstall
						},
						{ root: true }
					);
					dispatch('getProfile')
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
		pageLength: 10,
		entities: [],
		qtext: null,
		sort: 'default'
	},
	mutations: {
		setDocs(state, response) {
			if (response) {
				state.total = response.total
				state.page = response.page
				state.pageLength = response.pageLength
				state.colors = {};
				state.edges = response.edges
				state.nodes = response.nodes
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
		}
	},
	actions: {
		search({ commit, state }) {
			return searchApi
				.getEntities(state.entities, state.qtext, state.page, state.pageLength, state.sort)
				.then(response => {
					if (response) {
						commit('setDocs', response)
					}
				})
		},
		getRelatedEntities({ commit, state }, { uri, label, page, pageLength }) {
			return searchApi
				.getRelatedEntities({ uri, label, page, pageLength })
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
		}
	},
	actions: {
		async init({ commit, dispatch }) {
			if (auth.state.needsInstall) {
				await authApi.install()
				await commit('auth/setNeedsInstall', { needsInstall: false }, { root: true })
			}
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
				else {
					await dispatch('save', state.models[0])
				}
			}
			catch(err) {
				await dispatch('save', state.models[0])
			}
		},
		async save({ commit, dispatch }, data) {
			commit('setModel', data);
			await modelApi.save(data);
			return dispatch('getAll');
		},
		async delete({ state, commit, dispatch }, data) {
			await modelApi.deleteModel(data);
			dispatch('save', state.models.filter(m => m.name !== data.name)[0]);
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

export default new Vuex.Store({
	strict: debug,
	state: {
		initialized: false
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
			dispatch('model/init');
			dispatch('mastering/getNotifications', {});
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
		mastering
	}
});
