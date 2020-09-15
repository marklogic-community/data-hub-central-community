describe('Integrate Tab', () => {

	beforeEach(() => {
		cy.server()
		cy.route('/api/auth/status', {"appName":null,"authenticated":true,"username":"admin","disallowUpdates":false,"appUsersOnly":false})
		cy.route({
			method: 'PUT',
			url: '/api/models/',
			status: 204,
			response: {}
		})
		cy.route('GET', '/api/models/', [])
		cy.route('/api/auth/profile', {"username":"admin","fullname":null,"emails":null})
		cy.route('/api/models/current', 'fixture:model.json')
		cy.route('GET', '/api/entities', 'fixture:entities.json')
		cy.route('GET', '/api/flows/21232f297a57a5a743894a0e4a801fc3', 'fixture:flow-envision.json')
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfo.json')
		cy.route('POST', '/api/mastering/notifications', 'fixture:notificationsPage1.json')
		cy.route('GET', '/api/flows/mappings/functions', 'fixture:functions.json')
		cy.route('GET', '/api/flows/mappings/21232f297a57a5a743894a0e4a801fc3-MappingTest', 'fixture:maptest-mapping.json')
		cy.route('GET', '/v1/resources/mlCollections*', 'fixture:sample-doc-uris.json')
		cy.route('POST', '/api/flows/mappings', {})
		cy.route('POST', /\/api\/flows\/mappings\/validate.*/, 'fixture:validate-sample-doc.json')
		cy.route('POST', '/api/flows/mappings/sampleDoc', 'fixture:mapping-sample-doc.json')
		cy.route('POST', '/api/flows/mappings/preview', 'fixture:mapping-preview.json')
		cy.route('POST', '/api/flows/steps', {}).as('updateStep')
		cy.route('POST', '/api/flows/steps/delete', {}).as('deleteStep')
		cy.route('PUT', '/api/flows/21232f297a57a5a743894a0e4a801fc3', {}).as('saveFlow')
	})

	it('shows all the steps', () => {
		cy.visit('/integrate')
		cy.get('.step-wrapper').should('have.length', 3)
	})
})
