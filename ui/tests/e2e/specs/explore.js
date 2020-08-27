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
		cy.route('/api/auth/profile', {"username":"admin","fullname":null,"emails":null})

		cy.route('/api/models/current', 'fixture:model.json')
		cy.route('GET', '/api/models/', 'fixture:models.json')
		cy.route('POST', '/api/explore/entities', 'fixture:searchResults.json')
		cy.route('GET', '/api/crud/metadata**?uri=/com.marklogic.smart-mastering/merged/5d83f304b366fd804a4afccbd33e4b24.json&database=final', 'fixture:metadata.json')
		cy.route('GET', '/api/crud/metadata**?uri=/com.marklogic.smart-mastering/merged/5d83f304b366fd804a4afccbd33e4b24.json&database=staging', 'fixture:metadata.json')
		cy.route('GET', '/api/crud?uri=/com.marklogic.smart-mastering/merged/5d83f304b366fd804a4afccbd33e4b24.json&database=final', 'fixture:crud.json')
		cy.route('GET', '/api/crud?uri=/com.marklogic.smart-mastering/merged/5d83f304b366fd804a4afccbd33e4b24.json&database=staging', 'fixture:crud.json')
		cy.route('POST', '/api/mastering/notifications', 'fixture:notificationsPage1.json')
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
		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&q=Sashenka&page=1&db=final')

		cy.get('[data-cy=tabGrid]').click()
		cy.url().should('equal', 'http://localhost:9999/explore?tab=1&q=Sashenka&page=1&db=final')
	})

  it('Finds one search result and properties right panel populated from graph', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.contains('Showing results 1 to 5 of 31')
		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&q=Sashenka&page=1&db=final')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("/com.marklogic.smart-mastering/merged/5d83f304b366fd804a4afccbd33e4b24.json").click()

		cy.get('[data-cy=entityTitle]').contains("Sashenka")
		cy.contains('571 Grayhawk Court')
	})

	it('Clears Properties area on db switch', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.contains('Showing results 1 to 5 of 31')
		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&q=Sashenka&page=1&db=final')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("/com.marklogic.smart-mastering/merged/5d83f304b366fd804a4afccbd33e4b24.json").click()

		cy.get('[data-cy=entityTitle]').contains("Sashenka")
		cy.contains('571 Grayhawk Court')

		cy.get('[data-cy="explore.database"]').parentsUntil('.v-select__slot').click()
		cy.get('.databaseArray .v-list-item').contains('Staging').parentsUntil('.v-list-item').click()
		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&q=Sashenka&page=1&db=staging')
		cy.get('[data-cy=tabGrid]').should('not.exist')
		cy.get('[data-cy=tabGraph]').should('not.exist')

		cy.get('[data-cy=entityTitle]').should('not.exist')
	})

	it('Searches staging db', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=tabGrid]').should('exist')
		cy.get('[data-cy=tabGraph]').should('exist')

		cy.get('[data-cy="explore.database"]').parentsUntil('.v-select__slot').click()
		cy.get('.databaseArray .v-list-item').contains('Staging').parentsUntil('.v-list-item').click()
		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&page=1&db=staging')
		cy.get('[data-cy=tabGrid]').should('not.exist')
		cy.get('[data-cy=tabGraph]').should('not.exist')

		cy.get('h3').contains('Sashenka').parent().click()
		cy.url().should('equal', 'http://localhost:9999/detail?uri=%2Fcom.marklogic.smart-mastering%2Fmerged%2F5d83f304b366fd804a4afccbd33e4b24.json&db=staging')
		cy.get('code.json.hljs').contains('envelope')
	})

	it('Finds more facets', () => {
		cy.route('POST', '/api/explore/values', 'fixture:values.json')
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.contains('Showing results 1 to 5 of 31')
		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&q=Sashenka&page=1&db=final')
		cy.get('[data-cy="facet.Collections"] .facet-item').should('have.length', 5)
		cy.get('[data-cy="facet.Collections"] a.seemore').click()
		cy.get('[data-cy="facet.Collections"] .facet-item').should('have.length', 12)
		cy.get('[data-cy="facet.Collections"] a.seemore').should('not.exist')
	})

  it('Finds one search result and properties right panel populated from grid', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.contains('Showing results 1 to 5 of 31')

		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&q=Sashenka&page=1&db=final')
		cy.get('[data-cy=tabGrid]').click()
		cy.url().should('equal', 'http://localhost:9999/explore?tab=1&q=Sashenka&page=1&db=final')

		cy.get('h3').contains('Sashenka').parent().click()
		cy.get('[data-cy=entityTitle]').contains("Sashenka")
		cy.contains('571 Grayhawk Court')
	})

	it('Grid Result open details page', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.contains('Showing results 1 to 5 of 31')

		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&q=Sashenka&page=1&db=final')
		cy.get('[data-cy=tabGrid]').click()
		cy.url().should('equal', 'http://localhost:9999/explore?tab=1&q=Sashenka&page=1&db=final')

		cy.get('h3').contains('Sashenka').parent().click()
		cy.get('[data-cy=entityTitle]').contains("Sashenka")
		cy.contains('571 Grayhawk Court')
		cy.get('[data-cy="entity.detailsBtn"]').click()
		cy.url().should('equal', 'http://localhost:9999/detail?uri=%2Fcom.marklogic.smart-mastering%2Fmerged%2F5d83f304b366fd804a4afccbd33e4b24.json&db=final')
		cy.get('code.json.hljs').contains('envelope')
	})

	it('Grid Result close details page', () => {
		cy.visit('/explore')
		cy.get('[data-cy=searchInput]').clear()
		cy.get('[data-cy=searchInput]').type('Sashenka{enter}')
		cy.contains('Showing results 1 to 5 of 31')

		cy.url().should('equal', 'http://localhost:9999/explore?tab=0&q=Sashenka&page=1&db=final')
		cy.get('[data-cy=tabGrid]').click()
		cy.url().should('equal', 'http://localhost:9999/explore?tab=1&q=Sashenka&page=1&db=final')

		cy.get('h3').contains('Sashenka').parent().click()
		cy.get('[data-cy=entityTitle]').contains("Sashenka")
		cy.contains('571 Grayhawk Court')
		cy.get('[data-cy="entity.hideBtn"]').click()
		cy.get('[data-cy="entity.entityTitle"]').should('not.be.visible')
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
		it('shows no range indexes in sort by when none exist', () => {
			cy.visit('/explore')
			cy.get('[data-cy="menuBtn.Default"]').click()
			cy.get('.v-menu__content .v-list-item__title').should('contain', 'Default')
			cy.get('.v-menu__content .v-list-item--link').should('not.contain', 'Advanced')
		})
	})

	describe('Sort Options', () => {
		beforeEach(function () {
			cy.route('/api/models/current', 'fixture:modelWithRangeIndexes.json')
			cy.route('GET', '/api/models/', 'fixture:modelsWithRangeIndexes.json')
			cy.route('POST', '/api/explore/entities', 'fixture:searchResults.json')
			cy.visit('/')
			cy.url().should('include', '/model')
		})

		it('shows range indexes in sort when some exist', () => {
			cy.visit('/explore')
			cy.get('[data-cy="menuBtn.Default"]').click()
			cy.get('.v-menu__content .v-list-item__title').should('contain', 'Default')
			cy.get('.v-menu__content .v-list-item--link').should('contain', 'Advanced')
			cy.get('[data-cy="menuBtn.Advanced"]').click()
			cy.get('[data-cy="menuBtn.Customer.age"]').should('contain', 'Customer.age')
			cy.get('[data-cy="item.Order.orderDate"]').should('contain', 'Order.orderDate')
			cy.get('i').should('contain', 'priority_high')
		})
	})
})
