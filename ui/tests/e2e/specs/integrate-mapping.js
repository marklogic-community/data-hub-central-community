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
	describe('Mapping Step', () => {
		beforeEach(() => {
			cy.visit('/integrate')
			cy.get('.step-wrapper').contains('MappingTest').click()
		})

		it('can filter properties', () => {
			cy.get('.stepsPane tbody tr').should('have.length', 4)
			cy.get('.stepsPane [data-cy="mappingStep.filterProps"]').type('cat')
			cy.get('.stepsPane tbody tr').should('have.length', 1)
			cy.get('.stepsPane .v-input__icon--clear').click()
			cy.get('.stepsPane tbody tr').should('have.length', 4)
		})

		it('can paginate through samples', () => {
			cy.get('.v-pagination__navigation.v-pagination__navigation--disabled .mdi-chevron-left').should('exist')
			cy.get('.v-pagination__navigation .mdi-chevron-right').should('exist')
			cy.get('.v-pagination__item.v-pagination__item--active.primary').contains('1').should('exist')
			cy.get('.v-pagination__item').contains('13').should('exist')

			cy.get('.v-pagination__navigation .mdi-chevron-right').click()
			cy.get('.v-pagination__navigation.v-pagination__navigation--disabled .mdi-chevron-left').should('not.exist')
			cy.get('.v-pagination__item.v-pagination__item--active.primary').contains('1').should('not.exist')
			cy.get('.v-pagination__item.v-pagination__item--active.primary').contains('2').should('exist')

			cy.get('.v-pagination__item').contains('13').click()
			cy.get('.v-pagination__navigation.v-pagination__navigation--disabled .mdi-chevron-right').should('exist')
			// cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{}}])
			// cy.get('[data-cy="cardMenu.createModelButton"]').click()
			// cy.get('[data-cy="createModelVue.createModelNameField"]').type('Test Model')
			// cy.get('[data-cy="createModelVue.createSubmitButton"]').click()
			// cy.get('[data-cy="createModelVue.currentModelLabel"]').should('have.text', 'Test Model')
		})

		it('can clear test values', () => {
			cy.get('.value-column').contains('55006').should('exist')
			cy.wait(500)
			cy.get('[data-cy="mappingStep.clearBtn"]').click()
			cy.get('.value-column').contains('55006').should('not.exist')

			cy.get('[data-cy="mappingStep.testBtn"]').click()
			cy.get('.value-column').contains('55006').should('exist')
		})

		it('can preview a doc', () => {
			cy.get('.v-dialog--active').should('not.exist')
			cy.get('[data-cy="mappingStep.previewBtn"]').click()
			cy.get('.v-dialog--active').contains('Mapping Preview').should('exist')
			cy.get('[data-cy="previewDlg.close"]').click()
			cy.get('.v-dialog--active').should('not.exist')
		})

		it('can choose a function', () => {
			cy.get('.v-dialog--active').should('not.exist')
			cy.get('td input').first().should('not.have.value', 'parseDate(value,pattern)')
			cy.get('[data-cy="function.button"]').first().click()
			cy.get('[data-cy="functions.insertBtn"]').should('not.exist')
			cy.get('.v-dialog--active .v-list-item').contains('parseDate').click()
			cy.get('[data-cy="functions.insertBtn"]').click()
			cy.get('.v-dialog--active').should('not.exist')
			cy.get('td input').first().should('have.value', 'parseDate(value,pattern)')
		})

		it('can filter functions', () => {
			cy.get('.v-dialog--active').should('not.exist')
			cy.get('[data-cy="function.button"]').first().click()
			cy.get('.v-dialog--active .v-list-item').should('have.length', 6)
			cy.get('[data-cy="functions.filter"]').type('un')
			cy.get('.v-dialog--active .v-list-item').should('have.length', 1)
			cy.get('[data-cy="functions.filter"]').clear().type('fkafld')
			cy.get('.v-dialog--active .v-list-item').should('have.length', 0)
			cy.get('.v-dialog--active div.v-input__icon.v-input__icon--clear').click()
			cy.get('.v-dialog--active .v-list-item').should('have.length', 6)
			cy.get('.v-dialog--active').type('{esc}')
			cy.get('.v-dialog--active').should('not.exist')
		})

		it('open docs link', () => {
			cy.get('.v-dialog--active').should('not.exist')
			cy.get('[data-cy="function.button"]').first().click()
			cy.get('.v-dialog--active .v-list-item').should('have.length', 6)
			cy.get('[data-cy="functions.filter"]').type('un')
			cy.get('.v-dialog--active .v-list-item').should('have.length', 1)
			cy.get('.v-dialog--active .v-list-item').click()
			cy.get('[data-cy="functions.docsLink"]').should('have.attr', 'href', 'https://docs.marklogic.com/fn:unparsed-text')
		})

		it('can choose an xpath', () => {
			cy.get('.v-dialog--active').should('not.exist')
			cy.get('td input').first().should('not.have.value', 'its/nested')
			cy.wait(500)
			cy.get('[data-cy="mapping.xpathButton"]').first().click()
			cy.get('.v-treeview-node__label').should('have.length', 7)
			cy.get('.v-treeview-node__label').contains('nested').click()
			cy.get('td input').first().should('have.value', 'its/nested')
		})

		it('can edit the step', () => {
			cy.get('.v-dialog--active').should('not.exist')
			cy.get('[data-cy="flowStep.editButton"]').click()
			cy.get('.v-dialog--active [data-cy="addStepDialog.stepNameField"]').should('have.value', 'MappingTest')
			cy.get('.v-dialog--active [data-cy="addStepDialog.stepTypeField"]').parent().within(() => {
				cy.get('.v-select__selection').should('have.text', 'Mapping')
			})
			cy.get('.v-dialog--active [data-cy="addStepDialog.entityTypeField"]').parent().within(() => {
				cy.get('.v-select__selection').should('have.text', 'Resume')
			})
			cy.get('.v-dialog--active [data-cy="addStepDialog.dataSourceField"]').parent().within(() => {
				cy.get('.v-select__selection').should('have.text', 'DataSource1')
			})

			cy.get('[data-cy="addStepDialog.advancedBtn"]').click()

			cy.get('[data-cy="addStepDialog.stepDescField"]').should('have.value', 'Mapping Test rules!')
			// cy.get('.v-dialog--active [data-cy="addStepDialog.sourceDatabaseField"]').parent().within(() => {
			// 	cy.get('.v-select__selection').should('have.text', 'Staging')
			// })
			// cy.get('.v-dialog--active [data-cy="addStepDialog.targetDatabaseField"]').parent().within(() => {
			// 	cy.get('.v-select__selection').should('have.text', 'Final')
			// })
			cy.get('.v-dialog--active [data-cy="addStepDialog.dataFormatField"]').parent().within(() => {
				cy.get('.v-select__selection').should('have.text', 'json')
			})

			cy.get('[data-cy="addStepDialog.stepDescField"]').clear({force: true}).type('Updated!')
			cy.get('.v-dialog--active [data-cy="addStepDialog.dataFormatField"]').parent().click()
			cy.get('.v-menu__content:visible .v-list-item:visible').contains('xml').parentsUntil('.v-list-item').first().click()
			cy.get('[data-cy="addStepDialog.saveBtn"]').click()
			cy.wait('@updateStep')
				.its('request.body')
				.should((body) => {
					expect(body.step.description).to.equal('Updated!')
					expect(body.step.options.outputFormat).to.equal('xml')
				})
		})

		it('can delete a step', () => {
			cy.get('#flow-step').should('exist')
			cy.get('[data-cy="flowStep.deleteButton"]').click()
			cy.get('button').contains('Cancel').click()

			cy.get('#flow-step').should('exist')

			cy.get('[data-cy="flowStep.deleteButton"]').click()
			cy.get('button').contains('Delete').click()
			cy.wait('@deleteStep')
				.its('request.body')
				.should(body => {
					expect(body.flowName).to.equal('21232f297a57a5a743894a0e4a801fc3')
					expect(body.stepName).to.equal('MappingTest')
				})
			cy.get('#flow-step').should('not.exist')
		})
	})
})
