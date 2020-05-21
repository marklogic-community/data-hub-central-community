/*
https://docs.cypress.io/api/introduction/api.html
select known entity
check its properties, check its relationships, check its provo
if they're what they expect, success!  If not, failure!

Approach - couldn't find a way for Cypress to interact with VisJS because
it uses canvas. Instead, output results from a search to a list and implemented
a click action on the results to populate the properties page
*/
describe('Explore', () => {
	beforeEach(function () {
		cy.server()
		cy.route('/api/auth/status', {"appName":null,"authenticated":true,"username":"admin","disallowUpdates":false,"appUsersOnly":false,"needsInstall":false})
		cy.route('/api/models/activeIndexes', ['age'])
		cy.route('PUT', '/api/models/', {})
		// cy.route('GET', '/api/models/', [])
		cy.route('/api/auth/profile', {"username":"admin","fullname":null,"emails":null})

		return cy.readFile('tests/e2e/data/model.json')
			.then(file => {
				cy.route('/api/models/model.json', file)
				cy.route('GET', '/api/models/', [file])
				return cy.readFile('tests/e2e/data/searchResults.json')
			})
			.then(file => {
				cy.route('POST', '/api/explore/entities', file)
				cy.visit('/')
				cy.url().should('include', '/model')
			})
	})

	it('Finds no search results', () => {
		// return no results from api
		cy.route('POST', '/api/explore/entities/', {
			"page": 1,
			"total": 0,
			"pageLength": 30,
			"nodes": {},
			"edges": {}
		})
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').type('WrightWrong{enter}')
		cy.contains('No Results found', { timeout: 10000 })
	})

	it('Finds one search result', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.contains('Showing results 1 to 5 of 31')
	})

  it('Finds one search result and properties right panel populated', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.contains('Showing results 1 to 5 of 31')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("/com.marklogic.smart-mastering/merged/5d83f304b366fd804a4afccbd33e4b24.json").click()

		cy.get('[data-cy=entityTitle]').contains("Sashenka")
		cy.contains('571 Grayhawk Court')
	})

	it('Property panel cleared after search', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("/com.marklogic.smart-mastering/merged/5d83f304b366fd804a4afccbd33e4b24.json").click()

		cy.route('POST', '/api/explore/entities/', {
			"page": 1,
			"total": 0,
			"pageLength": 30,
			"nodes": {},
			"edges": {}
		})
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'hidden')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('{enter}')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=searchInput]').type('DoesNotExist{enter}')
		cy.get('[data-cy=entityTitle]').should('not.contain', 'Sashenka', { timeout: 10000 })
	})

	describe('Sort Options', () => {
		beforeEach(function () {
			return cy.readFile('tests/e2e/data/modelWithRangeIndexes.json')
			.then(file => {
				cy.route('/api/models/model.json', file)
				cy.route('GET', '/api/models/', [file])
				return cy.readFile('tests/e2e/data/searchResults.json')
			})
			.then(file => {
				cy.route('POST', '/api/explore/entities', file)
				cy.visit('/')
				cy.url().should('include', '/model')
			})
		})

		it('shows no range indexes in sort by when none exist', () => {
			cy.visit('/explore')
			cy.get('[data-cy="menuBtn.Default"]').click()
			cy.get('.v-menu__content .v-list-item__title').should('contain', 'Default')
			cy.get('.v-menu__content .v-list-item__title').should('contain', 'Most Connected First')
			cy.get('.v-menu__content .v-list-item__title').should('contain', 'Least Connected First')
			cy.get('.v-menu__content .v-list-item--link').should('not.contain', 'Advanced')
		})

		it.only('shows range indexes in sort when some exist', () => {
			cy.visit('/explore')
			cy.get('[data-cy="menuBtn.Default"]').click()
			cy.get('.v-menu__content .v-list-item__title').should('contain', 'Default')
			cy.get('.v-menu__content .v-list-item__title').should('contain', 'Most Connected First')
			cy.get('.v-menu__content .v-list-item__title').should('contain', 'Least Connected First')
			cy.get('.v-menu__content .v-list-item--link').should('contain', 'Advanced')
			cy.get('[data-cy="menuBtn.Advanced"]').click()
			cy.get('[data-cy="menuBtn.Customer.age"]').should('contain', 'Customer.age')
			cy.get('[data-cy="item.Order.orderDate"]').should('contain', 'Order.orderDate')
			cy.get('i').should('contain', 'priority_high')
		})
	})
})
