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
		cy.route('/api/models/model.json', 'fixture:model.json')
		cy.route('GET', '/api/entities', 'fixture:entities.json')
		cy.route('GET', '/api/flows/admin', 'fixture:flow-envision.json')
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfo.json')
		cy.route('POST', '/api/mastering/notifications', 'fixture:notificationsPage1.json')
		cy.route('GET', '/api/flows/mappings/functions', 'fixture:functions.json')
		cy.route('GET', '/api/flows/mappings/admin-MappingTest', 'fixture:maptest-mapping.json')
		cy.route('GET', '/v1/resources/mlCollections*', 'fixture:sample-doc-uris.json')
		cy.route('POST', '/api/flows/mappings', {})
		cy.route('POST', /\/api\/flows\/mappings\/validate.*/, 'fixture:validate-sample-doc.json')
		cy.route('POST', '/api/flows/mappings/sampleDoc', 'fixture:mapping-sample-doc.json')
		cy.route('POST', '/api/flows/mappings/preview', 'fixture:mapping-preview.json')
		cy.route('POST', '/api/flows/steps', {}).as('updateStep')
		cy.route('POST', '/api/flows/steps/delete', {}).as('deleteStep')
		cy.route('PUT', '/api/flows/admin', {}).as('saveFlow')
	})

	describe('Merging Step', () => {
		beforeEach(() => {
			cy.visit('/integrate')
			cy.get('.step-wrapper').contains('MergeTest').click()
		})

		describe('Add Options', () => {
			it('can add an option', () => {
				cy.get('#optionsTable td').contains('departmentId').should('not.exist')
				cy.get('[data-cy="merging.addOptionBtn"]').click()
				cy.wait(500)

				cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Property to Merge is required.').should('exist')
				cy.get('.v-messages__message').contains('Strategy is required.').should('exist')

				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.propertyNameField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('departmentId').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').contains('Property to Merge is required.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('My Strategy').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').contains('Strategy is required.').should('not.exist')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.merging[1])
							.to.deep.equal({
								algorithmRef: 'standard',
								length: {
									weight: null
								},
								propertyName: 'departmentId',
								sourceWeights: [],
								strategy: 'My Strategy'
							})
						expect(body.steps['3'].options.mergeOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "address", name: "address"},
								{localname: "departmentId", name: "departmentId"}
							])
					})
				cy.get('#optionsTable td').contains('departmentId').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'departmentId')
					cy.get('td').eq(1).should('have.text', 'My Strategy')
					cy.get('td').eq(2).should('have.text', '2')
					cy.get('td').eq(3).should('have.text', '3')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(5).should('have.text', '55')
				})
			})
		})

		describe('Edit Options', () => {
			it('should change an options property', () => {
				cy.get('#optionsTable td').contains('address').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address')
					cy.get('td').eq(1).should('have.text', 'My Strategy')
					cy.get('td').eq(2).should('have.text', '2')
					cy.get('td').eq(3).should('have.text', '3')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(5).should('have.text', '55')
				})

				cy.get('[data-cy="mergeStep.editOption"]').click()
				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.propertyNameField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'address')
				})
				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'My Strategy')
				})

				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.propertyNameField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('departmentId').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.merging[0])
							.to.deep.equal({
								algorithmRef: 'standard',
								length: {
									weight: null
								},
								propertyName: 'departmentId',
								sourceWeights: [],
								strategy: 'My Strategy'
							})
						expect(body.steps['3'].options.mergeOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "departmentId", name: "departmentId"}
							])
					})
				cy.get('#optionsTable td').contains('departmentId').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'departmentId')
					cy.get('td').eq(1).should('have.text', 'My Strategy')
					cy.get('td').eq(2).should('have.text', '2')
					cy.get('td').eq(3).should('have.text', '3')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(5).should('have.text', '55')
				})
			})

			it('should change an options strategy', () => {
				cy.get('#optionsTable td').contains('address').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address')
					cy.get('td').eq(1).should('have.text', 'My Strategy')
					cy.get('td').eq(2).should('have.text', '2')
					cy.get('td').eq(3).should('have.text', '3')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(5).should('have.text', '55')
				})

				cy.get('[data-cy="mergeStep.editOption"]').click()
				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.propertyNameField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'address')
				})
				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'My Strategy')
				})

				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('My Strategy2').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.merging[0])
							.to.deep.equal({
								algorithmRef: 'standard',
								length: {
									weight: null
								},
								propertyName: 'address',
								sourceWeights: [],
								strategy: 'My Strategy2'
							})
						expect(body.steps['3'].options.mergeOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "address", name: "address"}
							])
					})
				cy.get('#optionsTable td').contains('address').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address')
					cy.get('td').eq(1).should('have.text', 'My Strategy2')
					cy.get('td').eq(2).should('have.text', '22')
					cy.get('td').eq(3).should('have.text', '333')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '22')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '33')
					cy.get('td').eq(5).should('have.text', '555')
				})
			})

			it('should change an options property and strategy', () => {
				cy.get('#optionsTable td').contains('address').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address')
					cy.get('td').eq(1).should('have.text', 'My Strategy')
					cy.get('td').eq(2).should('have.text', '2')
					cy.get('td').eq(3).should('have.text', '3')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(5).should('have.text', '55')
				})

				cy.get('[data-cy="mergeStep.editOption"]').click()
				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.propertyNameField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'address')
				})
				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'My Strategy')
				})

				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.propertyNameField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('departmentId').parentsUntil('.v-list-item').click()

				cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('My Strategy2').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.merging[0])
							.to.deep.equal({
								algorithmRef: 'standard',
								length: {
									weight: null
								},
								propertyName: 'departmentId',
								sourceWeights: [],
								strategy: 'My Strategy2'
							})
						expect(body.steps['3'].options.mergeOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "departmentId", name: "departmentId"}
							])
					})
				cy.get('#optionsTable td').contains('departmentId').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'departmentId')
					cy.get('td').eq(1).should('have.text', 'My Strategy2')
					cy.get('td').eq(2).should('have.text', '22')
					cy.get('td').eq(3).should('have.text', '333')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '22')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '33')
					cy.get('td').eq(5).should('have.text', '555')
				})
			})
		})

		describe('Delete Options', () => {
			it('can delete options', () => {
				cy.get('#optionsTable td').contains('address').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address')
					cy.get('td').eq(1).should('have.text', 'My Strategy')
					cy.get('td').eq(2).should('have.text', '2')
					cy.get('td').eq(3).should('have.text', '3')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(5).should('have.text', '55')
				})

				cy.get('[data-cy="mergeStep.deleteOption"]').click()
				cy.get('button').contains('Cancel').click()
				cy.contains('td', 'address').should('exist')

				cy.get('[data-cy="mergeStep.deleteOption"]').click()
				cy.get('button').contains('Delete').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.merging.length).to.equal(1)
					})
				cy.contains('td', 'address').should('not.exist')
			})
		})

		describe('Add Strategy', () => {
			it('can add a Strategy', () => {
				cy.get('#strategiesTable td').contains('departmentId').should('not.exist')
				cy.get('[data-cy="merging.addStrategyBtn"]').click()
				cy.wait(500)

				cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Name is required.').should('exist')

				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.nameField"]').clear().type('My Fun Strategy')
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.maxValuesField"]').clear().type('aa')
				cy.get('.v-messages__message').contains('Max Values must be an integer.').should('exist')
				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.maxValuesField"]').clear().type('11')
				cy.get('.v-messages__message').contains('Max Values must be an integer.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.maxSourcesField"]').clear().type('aa')
				cy.get('.v-messages__message').contains('Max Sources must be an integer.').should('exist')
				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.maxSourcesField"]').clear().type('22')
				cy.get('.v-messages__message').contains('Max Sources must be an integer.').should('not.exist')

				// add a first source weight
				cy.get('[data-cy="mergeStrategyDlg.addSourceWeightBtn"]').click()
				cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Source Name is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')

				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').type('source1')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').type('33')
				cy.get('.v-messages__message').contains('Source Name is required.').should('not.exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('not.exist')

				// add a second set of source weights
				cy.get('[data-cy="mergeStrategyDlg.addSourceWeightBtn"]').click()
				cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Source Name is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')

				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').eq(1).type('source2')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').eq(1).type('333')
				cy.get('.v-messages__message').contains('Source Name is required.').should('not.exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('not.exist')

				// add a third set of source weights
				cy.get('[data-cy="mergeStrategyDlg.addSourceWeightBtn"]').click()
				cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Source Name is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')

				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').eq(2).type('source3')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').eq(2).type('3333')
				cy.get('.v-messages__message').contains('Source Name is required.').should('not.exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('not.exist')

				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').should('have.length', 3)
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').should('have.length', 3)

				// now remove the middle one
				cy.get('[data-cy="mergeStrategyDlg.removeSourceWeightBtn"]').eq(1).click()
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').should('have.length', 2)
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').should('have.length', 2)

				// length weight field
				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.lengthWeightField"]').clear().type('aa')
				cy.get('.v-messages__message').contains('Length Weight must be an integer.').should('exist')
				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.lengthWeightField"]').clear().type('44')
				cy.get('.v-messages__message').contains('Length Weight must be an integer.').should('not.exist')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.mergeStrategies[2])
							.to.deep.equal({
								algorithmRef: 'standard',
								length: {
									weight: '44'
								},
								maxSources: '22',
								maxValues: '11',
								name: 'My Fun Strategy',
								sourceWeights: [
									{ source: { name: 'source1', weight: '33' } },
									{ source: { name: 'source3', weight: '3333' } }
								]
							})
						expect(body.steps['3'].options.mergeOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "address", name: "address"}
							])
					})
				cy.get('#strategiesTable td').contains('My Fun Strategy').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'My Fun Strategy')
					cy.get('td').eq(1).should('have.text', '11')
					cy.get('td').eq(2).should('have.text', '22')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(1).should('have.text', '33')
					cy.get('td').eq(4).should('have.text', '44')
				})
			})
		})

		describe('Edit Strategy', () => {
			it('can edit a name', () => {
				// verify options
				cy.get('#optionsTable td').contains('address').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address')
					cy.get('td').eq(1).should('have.text', 'My Strategy')
					cy.get('td').eq(2).should('have.text', '2')
					cy.get('td').eq(3).should('have.text', '3')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(5).should('have.text', '55')
				})
				// verify strategies
				cy.get('#strategiesTable td').contains('My Strategy').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'My Strategy')
					cy.get('td').eq(1).should('have.text', '2')
					cy.get('td').eq(2).should('have.text', '3')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(3).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(3).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(4).should('have.text', '55')
				})

				cy.get('[data-cy="merging.editStrategyBtn"]').first().click()
				cy.wait(500)
				cy.get('[data-cy="mergeStrategyDlg.nameField"]').should('have.value', 'My Strategy')
				cy.get('[data-cy="mergeStrategyDlg.maxValuesField"]').should('have.value', '2')
				cy.get('[data-cy="mergeStrategyDlg.maxSourcesField"]').should('have.value', '3')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').eq(0).should('have.value', 'source1')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').eq(0).should('have.value', '2')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').eq(1).should('have.value', 'source2')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').eq(1).should('have.value', '3')
				cy.get('[data-cy="mergeStrategyDlg.lengthWeightField"]').should('have.value', '55')

				cy.get('[data-cy="mergeStrategyDlg.nameField"]').clear().type('My New Name')
				cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()
				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.mergeStrategies[0])
							.to.deep.equal({
								algorithmRef: 'standard',
								length: {
									weight: '55'
								},
								maxSources: '3',
								maxValues: '2',
								name: 'My New Name',
								sourceWeights: [
									{ source: { name: 'source1', weight: '2' } },
									{ source: { name: 'source2', weight: '3' } }
								]
							})
						expect(body.steps['3'].options.mergeOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "address", name: "address"}
							])
					})

				// verify option strategy name updated
				cy.get('#optionsTable td').contains('address').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address')
					cy.get('td').eq(1).should('have.text', 'My New Name')
					cy.get('td').eq(2).should('have.text', '2')
					cy.get('td').eq(3).should('have.text', '3')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(4).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(4).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(5).should('have.text', '55')
				})
				cy.get('#strategiesTable td').contains('My New Name').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'My New Name')
					cy.get('td').eq(1).should('have.text', '2')
					cy.get('td').eq(2).should('have.text', '3')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(3).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(3).find('div').eq(1).find('span').eq(1).should('have.text', '3')
					cy.get('td').eq(4).should('have.text', '55')
				})
			})

			it('can edit everything', () => {
				cy.get('#strategiesTable td').contains('My New Name').should('not.exist')
				cy.get('[data-cy="merging.editStrategyBtn"]').first().click()
				cy.wait(500)
				cy.get('[data-cy="mergeStrategyDlg.nameField"]').should('have.value', 'My Strategy')
				cy.get('[data-cy="mergeStrategyDlg.maxValuesField"]').should('have.value', '2')
				cy.get('[data-cy="mergeStrategyDlg.maxSourcesField"]').should('have.value', '3')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').eq(0).should('have.value', 'source1')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').eq(0).should('have.value', '2')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').eq(1).should('have.value', 'source2')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').eq(1).should('have.value', '3')
				cy.get('[data-cy="mergeStrategyDlg.lengthWeightField"]').should('have.value', '55')

				cy.get('[data-cy="mergeStrategyDlg.nameField"]').clear().type('My New Name')

				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.maxValuesField"]').clear().type('aa')
				cy.get('.v-messages__message').contains('Max Values must be an integer.').should('exist')
				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.maxValuesField"]').clear().type('11')
				cy.get('.v-messages__message').contains('Max Values must be an integer.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.maxSourcesField"]').clear().type('aa')
				cy.get('.v-messages__message').contains('Max Sources must be an integer.').should('exist')
				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.maxSourcesField"]').clear().type('22')
				cy.get('.v-messages__message').contains('Max Sources must be an integer.').should('not.exist')

				// add a 3rd source weight
				cy.get('[data-cy="mergeStrategyDlg.addSourceWeightBtn"]').click()
				cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Source Name is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')

				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').eq(2).type('source3')
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').eq(2).type('3333')
				cy.get('.v-messages__message').contains('Source Name is required.').should('not.exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('not.exist')

				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').should('have.length', 3)
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').should('have.length', 3)

				// now remove the middle one
				cy.get('[data-cy="mergeStrategyDlg.removeSourceWeightBtn"]').eq(1).click()
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightNameField"]').should('have.length', 2)
				cy.get('[data-cy="mergeStrategyDlg.sourceWeightWeightField"]').should('have.length', 2)


				// length weight field
				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.lengthWeightField"]').clear().type('aa')
				cy.get('.v-messages__message').contains('Length Weight must be an integer.').should('exist')
				cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.lengthWeightField"]').clear().type('44')
				cy.get('.v-messages__message').contains('Length Weight must be an integer.').should('not.exist')

				cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()
				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.mergeStrategies[0])
							.to.deep.equal({
								algorithmRef: 'standard',
								length: {
									weight: '44'
								},
								maxSources: '22',
								maxValues: '11',
								name: 'My New Name',
								sourceWeights: [
									{ source: { name: 'source1', weight: '2' } },
									{ source: { name: 'source3', weight: '3333' } }
								]
							})
						expect(body.steps['3'].options.mergeOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "address", name: "address"}
							])
					})
				cy.get('#strategiesTable td').contains('My New Name').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'My New Name')
					cy.get('td').eq(1).should('have.text', '11')
					cy.get('td').eq(2).should('have.text', '22')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(1).should('have.text', '2')
					cy.get('td').eq(3).find('div').eq(1).find('span').eq(0).should('have.text', 'source3:')
					cy.get('td').eq(3).find('div').eq(1).find('span').eq(1).should('have.text', '3333')
					cy.get('td').eq(4).should('have.text', '44')
				})
			})
		})

		describe('Delete Strategy', () => {
			it('cant delete a strategy in use', () => {
				cy.get('[data-cy="merging.deleteStrategyBtn"]').first().should('be.disabled')
				cy.get('[data-cy="mergeStep.deleteOption"]').first().click()
				cy.get('button').contains('Delete').click()
				cy.get('[data-cy="merging.deleteStrategyBtn"]').first().should('not.be.disabled')
			})

			it('can delete a strategy', () => {
				cy.get('#strategiesTable td').contains('My Strategy2').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'My Strategy2')
					cy.get('td').eq(1).should('have.text', '22')
					cy.get('td').eq(2).should('have.text', '333')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(0).should('have.text', 'source1:')
					cy.get('td').eq(3).find('div').eq(0).find('span').eq(1).should('have.text', '22')
					cy.get('td').eq(3).find('div').eq(1).find('span').eq(0).should('have.text', 'source2:')
					cy.get('td').eq(3).find('div').eq(1).find('span').eq(1).should('have.text', '33')
					cy.get('td').eq(4).should('have.text', '555')
				})

				cy.get('[data-cy="merging.deleteStrategyBtn"]').eq(1).click()
				cy.get('button').contains('Cancel').click()

				cy.get('#strategiesTable td').contains('My Strategy').should('exist')

				cy.get('[data-cy="merging.deleteStrategyBtn"]').eq(1).click()
				cy.get('button').contains('Delete').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['3'].options.mergeOptions.mergeStrategies.length).to.equal(1)
						expect(body.steps['3'].options.mergeOptions.mergeStrategies[0].name).to.equal('My Strategy')
					})
				cy.get('#strategiesTable td').contains('My Strategy2').should('not.exist')
			})
		})

		describe('Edit and Delete', () => {
			it('can edit the step', () => {
				cy.get('.v-dialog--active').should('not.exist')
				cy.get('[data-cy="flowStep.editButton"]').click()
				cy.get('.v-dialog--active [data-cy="addStepDialog.stepNameField"]').should('have.value', 'MergeTest')
				cy.get('.v-dialog--active [data-cy="addStepDialog.stepTypeField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'Merging')
				})
				cy.get('.v-dialog--active [data-cy="addStepDialog.entityTypeField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'Employee')
				})
				cy.get('.v-dialog--active [data-cy="addStepDialog.dataSourceField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'DataSource3')
				})

				cy.get('[data-cy="addStepDialog.advancedBtn"]').click()

				cy.get('[data-cy="addStepDialog.stepDescField"]').should('have.value', '')
				cy.get('.v-dialog--active [data-cy="addStepDialog.sourceDatabaseField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'Final')
				})
				cy.get('.v-dialog--active [data-cy="addStepDialog.targetDatabaseField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'Final')
				})
				cy.get('.v-dialog--active [data-cy="addStepDialog.dataFormatField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'json')
				})

				cy.get('[data-cy="addStepDialog.stepDescField"]').clear().type('Updated!')
				cy.get('.v-dialog--active [data-cy="addStepDialog.dataFormatField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('xml').parentsUntil('.v-list-item').click()
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
						expect(body.flowName).to.equal('admin')
						expect(body.stepName).to.equal('MergeTest')
					})
				cy.get('#flow-step').should('not.exist')
			})
		})
	})
})
