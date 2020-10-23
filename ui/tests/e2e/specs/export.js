describe('Export Tab', () => {
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
		cy.route('POST', '/api/export/runExports', {}).as('export')
		cy.route('GET', '/api/export/getExports/', 'fixture:fileExports.json')
		cy.route('GET', '/api/export/deleteExport/?exportId=abc123&token=null', {})
	})

	describe('Export', () => {
		beforeEach(() => {
			cy.visit('/export')
		})

		it('should select all in header', () => {
			cy.get('[data-cy="export.checkAll"]').should('not.be.checked')
			const x = ['Customer', 'Order', 'Department', 'Employee', 'JobOpening', 'Resume', 'JobReview', 'Product']
			x.forEach(e => {
				cy.get(`[data-cy="export.${e}"]`).parentsUntil('.v-input__control').first().click()
			})

			cy.get('[data-cy="export.checkAll"]').should('be.checked')
			cy.get('[data-cy="export.Customer"]').parentsUntil('.v-input__control').first().click()
			cy.get('[data-cy="export.checkAll"]').should('not.be.checked')
		})

		it('export all', () => {
			cy.get('[data-cy="export.checkAll"]').should('not.be.checked')
			cy.get('[data-cy="export.exportButton"]').should('be.disabled')
			cy.get('[data-cy="export.checkAll"]').parentsUntil('.v-input__control').first().click()
			cy.get('[data-cy="export.checkAll"]').should('be.checked')
			cy.get('[data-cy="export.exportButton"]').click()
			cy.wait('@export')
				.its('request.body')
				.should((body) => {
					expect(body).to.deep.equal(['Customer', 'Order', 'Department', 'Employee', 'JobOpening', 'Resume', 'JobReview', 'Product'])
				})
		})

		it('export two', () => {
			cy.get('[data-cy="export.exportButton"]').should('be.disabled')
			cy.get('[data-cy="export.Customer"]').parentsUntil('.v-input__control').first().click()
			cy.get('[data-cy="export.Product"]').parentsUntil('.v-input__control').first().click()
			cy.get('[data-cy="export.exportButton"]').click()
			cy.wait('@export')
				.its('request.body')
				.should((body) => {
					expect(body).to.deep.equal(['Customer', 'Product'])
				})
		})

		it('should delete an export', () => {
			cy.get('[data-cy="export.abc123"]').find('[data-cy="deleteDataConfirm.deleteButton"]').click()
			cy.get('button').contains('Cancel').click()

			cy.get('[data-cy="export.abc123"]').find('[data-cy="deleteDataConfirm.deleteButton"]').click()
			cy.get('button').contains('Delete').click()
			cy.get('[data-cy="export.abc123"]').should('not.exist')
		})
	})
})
