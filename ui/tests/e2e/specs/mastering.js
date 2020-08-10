/*
https://docs.cypress.io/api/introduction/api.html
select known entity
check its properties, check its relationships, check its provo
if they're what they expect, success!  If not, failure!

Approach - couldn't find a way for Cypress to interact with VisJS because
it uses canvas. Instead, output results from a search to a list and implemented
a click action on the results to populate the properties page
*/
describe('Mastering', () => {
	beforeEach(function () {
		cy.server()
		cy.route('/api/auth/status', {"appName":null,"authenticated":true,"username":"admin","disallowUpdates":false,"appUsersOnly":false,"needsInstall":false})
		cy.route('/api/models/activeIndexes', [])
		cy.route('PUT', '/api/models/', {})
		cy.route('/api/auth/profile', {"username":"admin","fullname":null,"emails":null})

		cy.route('/api/models/model.json', 'fixture:model.json')
		cy.route('GET', '/api/models/', 'fixture:models.json')
		cy.route('POST', '/api/explore/entities', 'fixture:searchResults.json')
		cy.route('POST', '/api/mastering/notifications', 'fixture:notificationsPage1.json')
	})

	it('Has 1 notification badge', () => {
		// return no results from api
		cy.route('POST', '/api/mastering/notifications',{
			"page": 1,
			"total": 1,
			"totalUnread": 0,
			"pageLength": 5,
			"notifications": []
		})
		cy.visit('/')
		cy.get('.v-badge__wrapper').should('not.contain', '1')
	})

	it('Has 0 notification badge', () => {
		// return no results from api
		cy.route('POST', '/api/mastering/notifications',{
			"page": 1,
			"total": 1,
			"totalUnread": 1,
			"pageLength": 5,
			"notifications": []
		})
		cy.visit('/')
		cy.get('.v-badge__wrapper').should('contain', '1')
	})

	it('Has 3 pages of notifications', () => {
		cy.visit('/notifications')

		cy.get('.v-pagination__item').should('contain', '1')
		cy.get('.v-pagination__item').should('contain', '2')
		cy.get('.v-pagination__item').should('contain', '3')
	})

	it('Should have menu disabled', () => {
		cy.visit('/notifications')
		cy.get('[data-cy="mastering.actionMenu"]').click()
		cy.get('.v-list-item--disabled').should('contain', 'Mark unread')
		cy.get('.v-list-item--disabled').should('contain', 'Merge')
		cy.get('.v-list-item--disabled').should('contain', 'Unmerge')
	})

	it('Should have menu enabled when checked', () => {
		cy.visit('/notifications')
		cy.get('[data-cy="mastering.checkAll"]').parentsUntil('.v-input__slot').click()
		cy.get('[data-cy="mastering.actionMenu"]').click()
		cy.get('.v-list-item--disabled').should('not.contain', 'Mark unread')
		cy.get('.v-list-item--disabled').should('not.contain', 'Merge')
		cy.get('.v-list-item--disabled').should('not.contain', 'Unmerge')
		cy.get('.v-list-item').should('contain', 'Mark unread')
		cy.get('.v-list-item').should('contain', 'Merge')
		cy.get('.v-list-item').should('contain', 'Unmerge')
	})

	it('should compare unmerged items for a notification', () => {
		cy.route('GET', '/api/mastering/doc?docUri=/doc1.json',{
			envelope: {
				instance: {
					info: {
						title: "Customer"
					},
					Customer: {
						firstName: 'Customer 1'
					}
				}
			}
		})
		cy.route('GET', '/api/mastering/doc?docUri=/doc2.json',{
			envelope: {
				instance: {
					info: {
						title: "Customer"
					},
					Customer: {
						firstName: 'Customer 2'
					}
				}
			}
		})

		cy.route('GET', '/api/mastering/notification?uri=/note1.xml', {
			"meta": {
				"dateTime": "2020-05-28T18:44:17.461166Z",
				"user": "admin",
				"uri": "/note1.xml",
				"status": "unread",
				"merged": false,
				"blocked": false
			},
			"flowInfo": {
				"flowName": "EmployeesMastering",
				"stepName": "MasterEmployees",
				"stepNumber": "3"
			},
			"thresholdLabel": "Might Match",
			"uris": [
				"/doc1.json",
				"/doc2.json"
			],
			"merged": {},
			"labels": {
				"/doc1.json": "doc 1",
				"/doc2.json": "doc 2"
			}
		})

		cy.route('POST', '/api/mastering/merge', {
			"success": true,
			"errors": [],
			"mergedURIs": [ "/doc1.json", "/doc2.json" ],
			"mergedDocument": {
				"value": {
					"envelope": {
						"instance": {
							"info": {
								"title": "Customer"
							},
							"Customer": {
								"firstName": ["Customer 1", "Customer 2"]
							}
						}
					}
				},
				"uri": "/mastered1.json",
				"previousUri": [
					"/doc1.json",
					"/doc2.json"
				]
			}
		})

		cy.route('POST', '/api/mastering/blocks', {"/doc1.json":[],"/doc2.json":[]})
		cy.route('PUT', '/api/mastering/notifications', {})
		cy.visit('/notifications/compare?notification=%2Fnote1.xml')

		cy.get('th.doc0').should('contain', '/doc1.json')
		cy.get('th.doc1').should('contain', '/doc2.json')
		cy.get('th.preview').should('contain', 'Merged: Preview')
		cy.get('.delta .doc0').should('contain', 'Customer 1')
		cy.get('.delta .doc1').should('contain', 'Customer 2')
		cy.get('.delta .preview').should('contain', '["Customer 1","Customer 2"]')
		cy.get('[data-cy="mastering.mergeButton"]').should('contain', 'Merge')
		cy.get('[data-cy="mastering.blockButton"]').should('contain', 'Block this Match')
		cy.get('[data-cy="mastering.unmergeButton"]').should('not.contain', 'UnMerge')
		cy.get('[data-cy="mastering.unblockButton"]').should('not.contain', 'Unblock this Match')
	})

	it('should compare merged items for a notification', () => {
		cy.route('GET', '/api/mastering/doc?docUri=/doc1.json',{
			envelope: {
				instance: {
					info: {
						title: "Customer"
					},
					Customer: {
						firstName: 'Customer 1'
					}
				}
			}
		})
		cy.route('GET', '/api/mastering/doc?docUri=/doc2.json',{
			envelope: {
				instance: {
					info: {
						title: "Customer"
					},
					Customer: {
						firstName: 'Customer 2'
					}
				}
			}
		})

		cy.route('GET', '/api/mastering/notification?uri=/note1.xml', {
			"meta": {
				"dateTime": "2020-05-28T18:44:17.461166Z",
				"user": "admin",
				"uri": "/note1.xml",
				"status": "unread",
				"merged": true,
				"blocked": false
			},
			"flowInfo": {
				"flowName": "EmployeesMastering",
				"stepName": "MasterEmployees",
				"stepNumber": "3"
			},
			"thresholdLabel": "Might Match",
			"uris": [
				"/doc1.json",
				"/doc2.json"
			],
			"merged": {
				"uri": "/mastered1.json",
				"doc": {
					"envelope": {
						"instance": {
							"info": {
								"title": "Customer"
							},
							"Customer": {
								"firstName": ["Customer 1", "Customer 2"]
							}
						}
					}
				}
			},
			"labels": {
				"/doc1.json": "doc 1",
				"/doc2.json": "doc 2"
			}
		})

		cy.route('POST', '/api/mastering/merge', {
			"success": true,
			"errors": [],
			"mergedURIs": [ "/doc1.json", "/doc2.json" ],
			"mergedDocument": {
				"value": {
					"envelope": {
						"instance": {
							"info": {
								"title": "Customer"
							},
							"Customer": {
								"firstName": ["Customer 1", "Customer 2"]
							}
						}
					}
				},
				"uri": "/mastered1.json",
				"previousUri": [
					"/doc1.json",
					"/doc2.json"
				]
			}
		})

		cy.route('POST', '/api/mastering/blocks', {"/doc1.json":[],"/doc2.json":[]})
		cy.route('PUT', '/api/mastering/notifications', {})
		cy.visit('/notifications/compare?notification=%2Fnote1.xml')
		cy.route('POST', '/api/mastering/notifications', 'fixture:notificationsWithMerge.json')

		cy.get('th.doc0').should('contain', '/doc1.json')
		cy.get('th.doc1').should('contain', '/doc2.json')
		cy.get('th.preview').should('contain', 'Merged: /mastered1.json')
		cy.get('.delta .doc0').should('contain', 'Customer 1')
		cy.get('.delta .doc1').should('contain', 'Customer 2')
		cy.get('.delta .preview').should('contain', '["Customer 1","Customer 2"]')
		cy.get('[data-cy="mastering.mergeButton"]').should('not.contain', 'Merge')
		cy.get('[data-cy="mastering.blockButton"]').should('not.contain', 'Block this Match')
		cy.get('[data-cy="mastering.unmergeButton"]').should('contain', 'UnMerge')
		cy.get('[data-cy="mastering.unblockButton"]').should('not.contain', 'Unblock this Match')
	})

	it('should compare blocked items for a notification', () => {
		cy.route('GET', '/api/mastering/doc?docUri=/doc1.json',{
			envelope: {
				instance: {
					info: {
						title: "Customer"
					},
					Customer: {
						firstName: 'Customer 1'
					}
				}
			}
		})
		cy.route('GET', '/api/mastering/doc?docUri=/doc2.json',{
			envelope: {
				instance: {
					info: {
						title: "Customer"
					},
					Customer: {
						firstName: 'Customer 2'
					}
				}
			}
		})

		cy.route('GET', '/api/mastering/notification?uri=/note1.xml', {
			"meta": {
				"dateTime": "2020-05-28T18:44:17.461166Z",
				"user": "admin",
				"uri": "/note1.xml",
				"status": "unread",
				"merged": false,
				"blocked": true
			},
			"flowInfo": {
				"flowName": "EmployeesMastering",
				"stepName": "MasterEmployees",
				"stepNumber": "3"
			},
			"thresholdLabel": "Might Match",
			"uris": [
				"/doc1.json",
				"/doc2.json"
			],
			"merged": {
				"uri": "/mastered1.json",
				"doc": {
					"envelope": {
						"instance": {
							"info": {
								"title": "Customer"
							},
							"Customer": {
								"firstName": ["Customer 1", "Customer 2"]
							}
						}
					}
				}
			},
			"labels": {
				"/doc1.json": "doc 1",
				"/doc2.json": "doc 2"
			}
		})

		cy.route('POST', '/api/mastering/merge', {
			"success": true,
			"errors": [],
			"mergedURIs": [ "/doc1.json", "/doc2.json" ],
			"mergedDocument": {
				"value": {
					"envelope": {
						"instance": {
							"info": {
								"title": "Customer"
							},
							"Customer": {
								"firstName": ["Customer 1", "Customer 2"]
							}
						}
					}
				},
				"uri": "/mastered1.json",
				"previousUri": [
					"/doc1.json",
					"/doc2.json"
				]
			}
		})

		cy.route('POST', '/api/mastering/blocks', {"/doc1.json":["/doc2.json"],"/doc2.json":["/doc1.json"]})
		cy.route('PUT', '/api/mastering/notifications', {})
		cy.route('POST', '/api/mastering/notifications', 'fixture:notificationsWithMerge.json')
		cy.visit('/notifications/compare?notification=%2Fnote1.xml')

		cy.get('th.doc0').should('contain', '/doc1.json')
		cy.get('th.doc1').should('contain', '/doc2.json')
		cy.get('th.preview').should('contain', 'Merged: /mastered1.json')
		cy.get('.delta .doc0').should('contain', 'Customer 1')
		cy.get('.delta .doc1').should('contain', 'Customer 2')
		cy.get('.delta .preview').should('contain', '["Customer 1","Customer 2"]')
		cy.get('[data-cy="mastering.mergeButton"]').should('not.contain', 'Merge')
		cy.get('[data-cy="mastering.blockButton"]').should('not.contain', 'Block this Match')
		cy.get('[data-cy="mastering.unmergeButton"]').should('not.contain', 'UnMerge')
		cy.get('[data-cy="mastering.unblockButton"]').should('contain', 'Unblock this Match')
	})
})
