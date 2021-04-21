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
		cy.route('GET', '/api/flows/', 'fixture:flowsEnvision.json')
		cy.route('GET', '/api/flows/21232f297a57a5a743894a0e4a801fc3', 'fixture:flow-envision.json')
		cy.route('GET', '/api/jobs?flowName=21232f297a57a5a743894a0e4a801fc3', 'fixture:jobs.json')
		cy.route('GET', '/api/flows/newStepInfo', 'fixture:newStepInfo.json')
		cy.route('POST', '/api/mastering/notifications', 'fixture:notificationsPage1.json')
		cy.route('GET', '/api/flows/mappings/functions', 'fixture:functions.json')
		cy.route('GET', '/api/flows/mappings/MappingTest', 'fixture:maptest-mapping.json')
		cy.route('GET', '/v1/resources/mlCollections*', 'fixture:sample-doc-uris.json')
		cy.route('POST', '/api/flows/mappings', {})
		cy.route('POST', /\/api\/flows\/mappings\/validate.*/, 'fixture:validate-sample-doc.json')
		cy.route('POST', '/api/flows/mappings/sampleDoc', 'fixture:mapping-sample-doc.json')
		cy.route('POST', '/api/flows/mappings/preview', 'fixture:mapping-preview.json')
		cy.route('POST', '/api/flows/steps', {}).as('updateStep')
		cy.route('POST', '/api/flows/steps/delete', {}).as('deleteStep')
		cy.route('PUT', '/api/flows/21232f297a57a5a743894a0e4a801fc3', {}).as('saveFlow')
	})

	describe('Matching Step', () => {
		beforeEach(() => {
			cy.visit('/integrate')
			cy.get('.step-wrapper').contains('MatchTest').click()
		})

		describe('Add Options', () => {
			it('can add exact options', () => {
				cy.get('td').contains('departmentId').should('not.exist')
				cy.get('[data-cy="matching.addOptionBtn"]').click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Match Type is required.').should('exist')
				cy.get('.v-messages__message').contains('Property to Match is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight must be an integer.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Exact').click()
				cy.get('.v-messages__message').contains('Match Type is required.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.getMenuOption('departmentId').click()
				cy.get('.v-messages__message').contains('Property to Match is required.').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('dd')
				cy.get('.v-messages__message').contains('Weight must be an integer.').should('exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('11')
				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]},{"name":"departmentId - Exact","weight":"11","matchRules":[{"entityPropertyPath":"departmentId","matchType":"exact","options":{}}]}]))
					})
				cy.get('td').contains('departmentId').parent().within(() => {
					cy.get('td').contains('exact').should('exist')
					cy.get('td').contains('11').should('exist')
				})
			})

			it('can add zip options', () => {
				cy.get('td').contains('email').should('not.exist')
				cy.get('[data-cy="matching.addOptionBtn"]').click()


				cy.getActiveDialog().should('exist')
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Zip').click()

				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Match Type is required.').should('not.exist')
				cy.get('.v-messages__message').contains('Property to Match is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')
				cy.get('.v-messages__message').contains('Match Weight must be an integer.').should('not.exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')
				cy.get('.v-messages__message').contains('Match Weight must be an integer.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.getMenuOption('email').click()
				cy.get('.v-messages__message').contains('Property to Match is required.').should('not.exist')

				//cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('dd')
				//cy.get('.v-messages__message').contains('5-vs-9 Match Weight must be an integer.').should('exist')
				//cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('11')

				//cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('dd')
				//cy.get('.v-messages__message').contains('9-vs-5 Match Weight must be an integer.').should('exist')
				//cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('22')
				
				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('11')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]},{"name":"email - Zip","weight":"11","matchRules":[{"entityPropertyPath":"email","matchType":"zip","options":{}}]}]))
					})
					cy.get('td').contains('email').parent().within(() => {
						cy.get('td').contains('zip').should('exist')
						cy.get('td').contains('11').should('exist')
					})
			})

			it('can add reduce options', () => {
				cy.get('td').contains('firstName, lastName').should('not.exist')
				cy.get('[data-cy="matching.addOptionBtn"]').click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Reduce').click()

				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Match Type is required.').should('not.exist')
				cy.get('.v-messages__message').contains('Properties to Match is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight must be an integer.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertiesReduceField"]').parent().click()
				cy.getMenuOption('employeeId').scrollIntoView()
				cy.getMenuOption('firstName').click()
				cy.getMenuOption('languageSkills').scrollIntoView()
				cy.getMenuOption('lastName').click()

				// cy.get('.v-menu__content:visible .v-select-list:visible').type('{enter}')
				cy.get('.menuable__content__active').invoke('css', 'display', 'none')
				cy.get('.v-messages__message').contains('Properties to Match is required.').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('dd')
				cy.get('.v-messages__message').contains('Weight must be an integer.').should('exist')
				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('33')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]},{"name":"Reduce: firstName - Exact, lastName - Exact","weight":"33","matchRules":[{"entityPropertyPath":"firstName","matchType":"exact","options":{}},{"entityPropertyPath":"lastName","matchType":"exact","options":{}}]}]))
					})
					cy.get('td').contains('firstName').parent().within(() => {
						cy.get('td').contains('Reduce').should('exist')
						cy.get('td').contains('33').should('exist')
					})
			})
		})

		describe('Edit Options', () => {
			it('can edit exact options', () => {
				cy.get('td').contains('departmentId').should('not.exist')
				cy.get('[data-cy="matchStep.editOption"]').first().click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				// cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				// cy.getMenuOption('Exact').click()
				// cy.get('.v-messages__message').contains('Match Type is required.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.getMenuOption('departmentId').click()

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"departmentId - Exact","weight":1,"matchRules":[{"entityPropertyPath":"departmentId","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]}]))
					})
				cy.get('td').contains('departmentId').parent().within(() => {
					cy.get('td').contains('exact').should('exist')
					cy.get('td').contains('1').should('exist')
				})
			})

			it('can change exact to zip', () => {
				cy.get('td').contains('toolSkills').parent().within(() => {
					cy.get('td').eq(1).should('have.text', '1')
				})

				cy.get('[data-cy="matchStep.editOption"]').first().click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Zip').click()
				cy.get('.v-messages__message').should('not.exist')

				//cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('dd')
				//cy.get('.v-messages__message').contains('5-vs-9 Match Weight must be an integer.').should('exist')
				//cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('11')

				//cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('dd')
				//cy.get('.v-messages__message').contains('9-vs-5 Match Weight must be an integer.').should('exist')
				//cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('22')
				
				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('11')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Zip","weight":"11","matchRules":[{"entityPropertyPath":"toolSkills","matchType":"zip","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]}]))
					})
				cy.get('td').contains('toolSkills').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'toolSkills - zip')
					cy.get('td').eq(1).should('have.text', '11')
				})
			})

			it('can change exact to reduce', () => {
				cy.get('td').contains('toolSkills').parent().within(() => {
					cy.get('td').eq(1).should('have.text', '1')
				})

				cy.get('[data-cy="matchStep.editOption"]').first().click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Reduce').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertiesReduceField"]').parent().click()
				cy.getMenuOption('employeeId').scrollIntoView()
				cy.getMenuOption('firstName').click()
				cy.getMenuOption('languageSkills').scrollIntoView()
				cy.getMenuOption('lastName').click()

				cy.get('.menuable__content__active').invoke('css', 'display', 'none')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"Reduce: firstName - Exact, lastName - Exact","weight":1,"matchRules":[{"entityPropertyPath":"firstName","matchType":"exact","options":{}},{"entityPropertyPath":"lastName","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]}]))
					})
					cy.get('td').contains('firstName').parent().within(() => {
						cy.get('td').contains('Reduce').should('exist')
						cy.get('td').contains('1').should('exist')
					})
			})

			it('can edit zip options', () => {
				cy.get('td').contains('country').parent().within(() => {
					cy.get('td').eq(1).should('have.text', '9')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(3).click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				//cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('11')
				//cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('22')
				
				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('11')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":"11","matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]}]))
					})
					cy.get('td').contains('country').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'country - zip')
					cy.get('td').eq(1).should('have.text', '11')
				})
			})

			it('can change zip to exact', () => {
				cy.get('td').contains('country').parent().within(() => {
					cy.get('td').eq(1).should('have.text', '9')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(3).click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Exact').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('33')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Exact","weight":"33","matchRules":[{"entityPropertyPath":"country","matchType":"exact","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]}]))
					})
					cy.get('td').contains('country').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'country - exact')
					cy.get('td').eq(1).should('have.text', '33')
				})
			})

			it('can change zip to reduce', () => {
				cy.get('td').contains('country').parent().within(() => {
					cy.get('td').eq(1).should('have.text', '9')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(3).click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Reduce').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertiesReduceField"]').parent().click()
				cy.getMenuOption('employeeId').scrollIntoView()
				cy.getMenuOption('firstName').click()
				cy.getMenuOption('languageSkills').scrollIntoView()
				cy.getMenuOption('lastName').click()

				cy.get('.menuable__content__active').invoke('css', 'display', 'none')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('33')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"Reduce: firstName - Exact, lastName - Exact","weight":"33","matchRules":[{"entityPropertyPath":"firstName","matchType":"exact","options":{}},{"entityPropertyPath":"lastName","matchType":"exact","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]}]))
					})
					cy.get('td').contains('firstName').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'Reduce: firstName - exact, lastName - exact')
					cy.get('td').eq(1).should('have.text', '33')
				})
			})

			it('can edit reduce options', () => {
				cy.get('td').contains('city').parent().within(() => {
					cy.get('td').eq(1).should('have.text', '5')
				})
				cy.get('[data-cy="matchStep.editOption"]').eq(4).click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('334')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":"334","matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]}]))
					})
					cy.get('td').contains('city').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'Reduce: city - exact')
					cy.get('td').eq(1).should('have.text', '334')
				})
			})

			it('can change reduce to exact', () => {
				cy.get('td').contains('city').parent().within(() => {
					cy.get('td').eq(1).should('have.text', '5')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(4).click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Exact').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.getMenuOption('departmentId').click()

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"departmentId - Exact","weight":5,"matchRules":[{"entityPropertyPath":"departmentId","matchType":"exact","options":{}}]}]))
					})
					cy.get('td').contains('departmentId').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'departmentId - exact')
					cy.get('td').eq(1).should('have.text', '5')
				})
			})

			it('can change reduce to zip', () => {
				cy.get('td').contains('city').parent().within(() => {
					cy.get('td').eq(1).should('have.text', '5')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(4).click()

				// cy.getActiveDialog().should('exist')
				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.getMenuOption('Zip').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.getMenuOption('departmentId').click()

				//cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('dd')
				//cy.get('.v-messages__message').contains('5-vs-9 Match Weight must be an integer.').should('exist')
				//cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('11')

				//cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('dd')
				//cy.get('.v-messages__message').contains('9-vs-5 Match Weight must be an integer.').should('exist')
				//cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('22')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"employeeId - Exact","weight":80,"matchRules":[{"entityPropertyPath":"employeeId","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"departmentId - Zip","weight":5,"matchRules":[{"entityPropertyPath":"departmentId","matchType":"zip","options":{}}]}]))
					})
					cy.get('td').contains('departmentId').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'departmentId - zip')
					cy.get('td').eq(1).should('have.text', '5')
				})
			})
		})

		describe('Delete Options', () => {
			it('can delete options', () => {
				cy.contains('td', 'employeeId').should('exist')
				cy.get('[data-cy="matchStep.deleteOption"]').eq(2).click()
				cy.get('button').contains('Cancel').click()
				cy.contains('td', 'employeeId').should('exist')

				cy.get('[data-cy="matchStep.deleteOption"]').eq(2).click()
				cy.get('button').contains('Cancel').click()
				cy.contains('td', 'employeeId').should('exist')

				cy.get('[data-cy="matchStep.deleteOption"]').eq(2).click()
				cy.get('button').contains('Delete').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].matchRulesets)).to.deep.equal(JSON.stringify([{"name":"toolSkills - Exact","weight":1,"matchRules":[{"entityPropertyPath":"toolSkills","matchType":"exact","options":{}}]},{"name":"address - Exact","weight":12,"matchRules":[{"entityPropertyPath":"address","matchType":"exact","options":{}}]},{"name":"country - Zip","weight":9,"matchRules":[{"entityPropertyPath":"country","matchType":"zip","options":{}}]},{"name":"Reduce: city - Exact","weight":5,"matchRules":[{"entityPropertyPath":"city","matchType":"exact","options":{}}]}]))
					})
				cy.contains('td', 'employeeId').should('not.exist')
			})
		})

		describe('Add Threshold', () => {
			it('can add a Notify threshold', () => {
				cy.get('[data-cy="matching.addThresholdBtn"]').click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Name is required.').should('exist')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').clear()
				cy.get('.v-messages__message').contains('Weight Threshold is required.').should('exist')
				cy.get('.v-messages__message').contains('Action is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight Threshold must be an integer.').should('not.exist')


				cy.get('[data-cy="matchThresholdDlg.nameField"]').type('My Notify Thresh')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').type('dd')
				cy.get('.v-messages__message').contains('Weight Threshold must be an integer.').should('exist')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').clear().type('11')

				cy.get('.v-dialog--active [data-cy="matchThresholdDlg.actionField"]').parent().click()
				cy.getMenuOption('Notify').click()

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].thresholds)).to.deep.equal(JSON.stringify([{"thresholdName":"merge","action":"merge","score":32},{"thresholdName":"notify","action":"notify","score":1},{"score":"11","action":"notify","thresholdName":"My Notify Thresh"}]))
					})
				cy.get('td').contains('My Notify Thresh').parent().within(() => {
				cy.get('td').eq(0).should('have.text', 'My Notify Thresh')
					cy.get('td').eq(1).should('have.text', '11')
					cy.get('td').eq(2).should('have.text', 'notify')
				})
			})

			it('can add a Merge threshold', () => {
				cy.get('[data-cy="matching.addThresholdBtn"]').click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Name is required.').should('exist')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').clear()
				cy.get('.v-messages__message').contains('Weight Threshold is required.').should('exist')
				cy.get('.v-messages__message').contains('Action is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight Threshold must be an integer.').should('not.exist')


				cy.get('[data-cy="matchThresholdDlg.nameField"]').type('My Merge Thresh')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').type('dd')
				cy.get('.v-messages__message').contains('Weight Threshold must be an integer.').should('exist')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').clear().type('11')

				cy.get('.v-dialog--active [data-cy="matchThresholdDlg.actionField"]').parent().click()
				cy.getMenuOption('Merge').click()

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].thresholds)).to.deep.equal(JSON.stringify([{"thresholdName":"merge","action":"merge","score":32},{"thresholdName":"notify","action":"notify","score":1},{"score":"11","action":"merge","thresholdName":"My Merge Thresh"}]))
					})
				cy.get('td').contains('My Merge Thresh').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'My Merge Thresh')
					cy.get('td').eq(1).should('have.text', '11')
					cy.get('td').eq(2).should('have.text', 'merge')
				})
			})
		})

		describe('Edit Threshold', () => {
			it('can edit a weight', () => {
				cy.get('[data-cy="matching.editThresholdBtn"]').first().click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').clear().type('333')
				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].thresholds)).to.deep.equal(JSON.stringify([{"thresholdName":"merge","score":"333","action":"merge"},{"thresholdName":"notify","action":"notify","score":1}]))
					})

				cy.get('td').contains('merge').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'merge')
					cy.get('td').eq(1).should('have.text', '333')
					cy.get('td').eq(2).should('have.text', 'merge')
				})
			})

			it('can change a merge to a notify', () => {
				cy.get('[data-cy="matching.editThresholdBtn"]').first().click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)
				cy.get('.v-dialog--active [data-cy="matchThresholdDlg.actionField"]').parent().click()
				cy.getMenuOption('Notify').click()

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].thresholds)).to.deep.equal(JSON.stringify([{"thresholdName":"merge","score":32,"action":"notify"},{"thresholdName":"notify","action":"notify","score":1}]))
					})

				cy.get('td').contains('merge').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'merge')
					cy.get('td').eq(1).should('have.text', '32')
					cy.get('td').eq(2).should('have.text', 'notify')
				})
			})

			it('can change a notify to a merge', () => {
				cy.get('[data-cy="matching.editThresholdBtn"]').last().click()

				cy.getActiveDialog().should('exist')
				// cy.wait(500)
				cy.get('.v-dialog--active [data-cy="matchThresholdDlg.actionField"]').parent().click()
				cy.getMenuOption('Merge').click()

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].thresholds)).to.deep.equal(JSON.stringify([{"thresholdName":"merge","action":"merge","score":32},{"thresholdName":"notify","score":1,"action":"merge"}]))
					})

				cy.get('td').contains('notify').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'notify')
					cy.get('td').eq(1).should('have.text', '1')
					cy.get('td').eq(2).should('have.text', 'merge')
				})
			})
		})

		describe('Delete Threshold', () => {
			it('can delete a threshold', () => {
				cy.get('td').contains('merge').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'merge')
					cy.get('td').eq(1).should('have.text', '32')
					cy.get('td').eq(2).should('have.text', 'merge')
				})

				cy.get('[data-cy="matching.deleteThresholdBtn"]').first().click()
				cy.get('button').contains('Cancel').click()

				cy.get('td').contains('merge').should('exist')

				cy.get('[data-cy="matching.deleteThresholdBtn"]').first().click()
				cy.get('button').contains('Delete').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(JSON.stringify(body.steps['2'].thresholds)).to.deep.equal(JSON.stringify([{"thresholdName":"notify","action":"notify","score":1}]))
					})

				cy.get('td').contains('merge').should('not.exist')
			})
		})

		describe('Edit and Delete', () => {
			it('can edit the step', () => {
				cy.get('.v-dialog--active').should('not.exist')
				cy.get('[data-cy="flowStep.editButton"]').click()

				cy.getActiveDialog().should('exist')
				cy.get('.v-dialog--active [data-cy="addStepDialog.stepNameField"]').should('have.value', 'MatchTest')
				cy.get('.v-dialog--active [data-cy="addStepDialog.stepTypeField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'Matching')
				})
				cy.get('.v-dialog--active [data-cy="addStepDialog.entityTypeField"]').parent().within(() => {
					//cy.get('.v-select__selection').should('have.text', 'Employee')
				})
				cy.get('.v-dialog--active [data-cy="addStepDialog.dataSourceField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'DataSource2')
				})

				cy.get('[data-cy="addStepDialog.advancedBtn"]').click()

				cy.get('[data-cy="addStepDialog.stepDescField"]').should('have.value', '')
				// cy.get('.v-dialog--active [data-cy="addStepDialog.sourceDatabaseField"]').parent().within(() => {
				// 	cy.get('.v-select__selection').should('have.text', 'Final')
				// })
				// cy.get('.v-dialog--active [data-cy="addStepDialog.targetDatabaseField"]').parent().within(() => {
				// 	cy.get('.v-select__selection').should('have.text', 'Final')
				// })
				cy.get('.v-dialog--active [data-cy="addStepDialog.dataFormatField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'json')
				})

				cy.get('[data-cy="addStepDialog.stepDescField"]').clear().type('Updated!')
				cy.get('.v-dialog--active [data-cy="addStepDialog.dataFormatField"]').parent().click()
				cy.getMenuOption('xml').click()
				cy.get('[data-cy="addStepDialog.saveBtn"]').click()
				cy.wait('@updateStep')
					.its('request.body')
					.should((body) => {
						expect(body.step.description).to.equal('Updated!')
						expect(body.step.outputFormat).to.equal('xml')
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
						expect(body.stepName).to.equal('MatchTest')
					})
				cy.get('#flow-step').should('not.exist')
			})
		})
	})
})
