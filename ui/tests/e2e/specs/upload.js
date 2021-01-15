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
		cy.route('GET', '/api/entities', [])
		cy.route('GET', '/api/flows/21232f297a57a5a743894a0e4a801fc3', 'fixture:flow-no-steps.json')
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfoEmptyData.json')
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
		cy.route('POST', '/api/upload', {}).as('uploadFile')
		cy.route('POST', '/api/system/deleteCollection', {}).as('deleteCollection')
	})

	it('should show sample data', () => {
		cy.visit('/upload')
		cy.contains('MyDataSource.csv').should('exist')
		cy.contains('MyOtherDataSource.csv').should('exist')
	})

	it('should show real data', () => {
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfo.json')
		cy.visit('/upload')
		cy.contains('DataSource1')
		cy.contains('DataSource2')
		cy.contains('DataSource3')
		cy.contains('DataSource4')
	})

	it('should upload a csv with default collection', () => {
		cy.visit('/upload')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.fixture('test.csv', 'base64').then(fileContent => {
			cy.get('[data-cy="uploadfileInput"]').should('exist')
        .attachFile(
					{ fileContent, fileName: 'test.csv', mimeType: 'text/csv' },
					{ subjectType: 'drag-n-drop' },
				);

			cy.get('[data-cy="uploadCollectionDlg.cancelBtn"]').click()

			cy.get('[data-cy="uploadfileInput"]').should('exist')
        .attachFile(
					{ fileContent, fileName: 'test.csv', mimeType: 'text/csv' },
					{ subjectType: 'drag-n-drop' },
				);

			cy.get('.v-messages__message').should('not.exist')
			cy.get('[data-cy="uploadCollectionDlg.saveBtn"]').click()
			cy.get('.v-messages__message').should('not.exist')

			cy.wait('@uploadFile')
				.its('request.body')
					.should(body => {
						expect(body.get('collection')).to.eq('test.csv')
						expect(body.get('database')).to.eq('staging')
						expect(body.get('files').name).to.eq('test.csv')
					})
		})
	})

	it('should upload a csv with changed collection', () => {
		cy.visit('/upload')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.fixture('test.csv', 'base64').then(fileContent => {
			cy.get('[data-cy="uploadfileInput"]').should('exist')
				.attachFile(
					{ fileContent, fileName: 'test.csv', mimeType: 'text/csv' },
					{ subjectType: 'drag-n-drop' },
				);

			cy.get('[data-cy="uploadCollection.collectionName"]').clear().type('My Collection Is Rad')
			cy.get('[data-cy="uploadCollectionDlg.cancelBtn"]').click()

			cy.get('[data-cy="uploadfileInput"]').should('exist')
				.attachFile(
					{ fileContent, fileName: 'test.csv', mimeType: 'text/csv' },
					{ subjectType: 'drag-n-drop' },
				);

			cy.get('[data-cy="uploadCollection.collectionName"]').clear().type('My Collection Is Rad')
			cy.get('[data-cy="uploadCollectionDlg.saveBtn"]').click()

			cy.get('.v-messages__message').should('contain', 'Collection cannot contain spaces. Only letters, numbers, and underscore')

			cy.get('[data-cy="uploadCollection.collectionName"]').clear().type('MyCollectionIsRad')
			cy.get('.v-messages__message').should('not.exist')

			cy.get('[data-cy="uploadCollectionDlg.saveBtn"]').click()
			cy.get('.v-messages__message').should('not.exist')

			cy.wait('@uploadFile')
				.its('request.body')
					.should(body => {
						expect(body.get('collection')).to.eq('MyCollectionIsRad')
						expect(body.get('database')).to.eq('staging')
						expect(body.get('files').name).to.eq('test.csv')
					})
		})
	})

	it('should upload a csv with changed database', () => {
		cy.visit('/upload')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.fixture('test.csv', 'base64').then(fileContent => {
			cy.get('[data-cy="uploadfileInput"]').should('exist')
				.attachFile(
					{ fileContent, fileName: 'test.csv', mimeType: 'text/csv' },
					{ subjectType: 'drag-n-drop' },
				);

			cy.get('[data-cy="uploadCollection.collectionName"]').clear().type('My Collection Is Rad')
			cy.get('[data-cy="uploadCollectionDlg.cancelBtn"]').click()

			cy.get('[data-cy="uploadfileInput"]').should('exist')
				.attachFile(
					{ fileContent, fileName: 'test.csv', mimeType: 'text/csv' },
					{ subjectType: 'drag-n-drop' },
				);

			cy.get('[data-cy="uploadCollection.collectionName"]').clear().type('My Collection Is Rad')
			cy.get('[data-cy="uploadCollectionDlg.saveBtn"]').click()

			cy.get('.v-messages__message').should('contain', 'Collection cannot contain spaces. Only letters, numbers, and underscore')

			cy.get('[data-cy="uploadCollection.collectionName"]').clear().type('MyCollectionIsRad')
			cy.get('.v-messages__message').should('not.exist')

			cy.get('[data-cy="uploadCollection.advancedBtn"]').click()

			cy.get('[data-cy="uploadCollection.database"]').parentsUntil('.v-select__slot').click()
			cy.get('.databaseArray .v-list-item').contains('Final').parentsUntil('.v-list-item').click()

			cy.get('[data-cy="uploadCollectionDlg.saveBtn"]').click()
			cy.get('.v-messages__message').should('not.exist')

			cy.wait('@uploadFile')
				.its('request.body')
					.should(body => {
						expect(body.get('collection')).to.eq('MyCollectionIsRad')
						expect(body.get('database')).to.eq('final')
						expect(body.get('files').name).to.eq('test.csv')
					})
		})
	})

	it('delete a data source', () => {
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfo.json')
		cy.visit('/upload')

		cy.get('[data-cy="manageSources.table"] tr').should('have.length', 4)
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]').should('have.length', 4)
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]:disabled').should('have.length', 0)

		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]').first().click()
		cy.get('button').contains('Cancel').click()
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfoPostDelete.json')
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]').first().click()
		cy.get('button').contains('Delete').click()
		cy.wait('@deleteCollection')
			.its('request.body')
				.should(body => {
					expect(body).to.deep.equal({"collections": ["DataSource1"], "database": "staging"})
				})
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]').should('have.length', 3)
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]:disabled').should('have.length', 0)
	})

	it('delete all data sources', () => {
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfo.json')
		cy.visit('/upload')

		cy.contains('MyDataSource.csv').should('not.exist')
		cy.contains('MyOtherDataSource.csv').should('not.exist')
		cy.get('[data-cy="manageSources.table"] tr').should('have.length', 4)
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]').should('have.length', 4)
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]:disabled').should('have.length', 0)

		cy.get('[data-cy="manageSources.deleteAll"] [data-cy="deleteDataConfirm.deleteButton"]').first().click()
		cy.get('button').contains('Cancel').click()
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfoPostDelete.json')
		cy.get('[data-cy="manageSources.deleteAll"] [data-cy="deleteDataConfirm.deleteButton"]').first().click()
		cy.get('button').contains('Delete').click()
		cy.wait('@deleteCollection')
			.its('request.body')
				.should(body => {
					expect(body).to.deep.equal({"collections": ["DataSource1","DataSource2","DataSource3","DataSource4"], "database": "staging"})
				})
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]').should('have.length', 2)
		cy.get('[data-cy="manageSources.table"] [data-cy="deleteDataConfirm.deleteButton"]:disabled').should('have.length', 2)
		cy.contains('MyDataSource.csv').should('exist')
		cy.contains('MyOtherDataSource.csv').should('exist')
	})
})
