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
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Exact').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').contains('Match Type is required.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('departmentId').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').contains('Property to Match is required.').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('dd')
				cy.get('.v-messages__message').contains('Weight must be an integer.').should('exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('11')
				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.add[3])
							.to.deep.equal({
								propertyName: "departmentId",
								weight: "11"
							})
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"},
								{localname: "departmentId", name: "departmentId"}
							])
					})
				cy.get('td').contains('departmentId').parent().within(() => {
					cy.get('td').contains('Exact').should('exist')
					cy.get('td').contains('11').should('exist')
				})
			})

			it('can add zip options', () => {
				cy.get('td').contains('email').should('not.exist')
				cy.get('[data-cy="matching.addOptionBtn"]').click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Zip').parentsUntil('.v-list-item').click()

				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Match Type is required.').should('not.exist')
				cy.get('.v-messages__message').contains('Property to Match is required.').should('exist')
				cy.get('.v-messages__message').contains('5-vs-9 Match Weight is required.').should('exist')
				cy.get('.v-messages__message').contains('5-vs-9 Match Weight must be an integer.').should('not.exist')
				cy.get('.v-messages__message').contains('9-vs-5 Match Weight is required.').should('exist')
				cy.get('.v-messages__message').contains('9-vs-5 Match Weight must be an integer.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('email').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').contains('Property to Match is required.').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('dd')
				cy.get('.v-messages__message').contains('5-vs-9 Match Weight must be an integer.').should('exist')
				cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('11')

				cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('dd')
				cy.get('.v-messages__message').contains('9-vs-5 Match Weight must be an integer.').should('exist')
				cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('22')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.expand[1]).to.deep.equal({
							propertyName: "email",
							algorithmRef: "zip-match",
							zip: [
								{origin: 5, weight: "11"},
								{origin: 9, weight: "22"}
							]
						})
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"},
								{localname: "email", name: "email"}
							])
					})
					cy.get('td').contains('email').parent().within(() => {
						cy.get('td').contains('Zip').should('exist')
						cy.get('td span').contains('5-Matches-9 Boost:').should('exist')
						cy.get('td span').contains('11').should('exist')
						cy.get('td span').contains('9-Matches-5 Boost:').should('exist')
						cy.get('td span').contains('22').should('exist')
					})
			})

			it('can add reduce options', () => {
				cy.get('td').contains('firstName, lastName').should('not.exist')
				cy.get('[data-cy="matching.addOptionBtn"]').click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Reduce').parentsUntil('.v-list-item').click()

				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Match Type is required.').should('not.exist')
				cy.get('.v-messages__message').contains('Properties to Match is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight must be an integer.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertiesReduceField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('employeeId').parentsUntil('.v-list-item').scrollIntoView()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('firstName').parentsUntil('.v-list-item').click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('languageSkills').parentsUntil('.v-list-item').scrollIntoView()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('lastName').parentsUntil('.v-list-item').click()

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
						expect(body.steps['2'].options.matchOptions.scoring.reduce[1]).to.deep.equal({
							algorithmRef: "standard-reduction",
							weight: "33",
							allMatch: {
								property: ["firstName", "lastName"]
							}
						})
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"},
								{localname: "firstName", name: "firstName"},
								{localname: "lastName", name: "lastName"}
							])
					})
					cy.get('td').contains('firstName, lastName').parent().within(() => {
						cy.get('td').contains('Reduce').should('exist')
						cy.get('td').contains('33').should('exist')
					})
			})
		})

		describe('Edit Options', () => {
			it('can edit exact options', () => {
				cy.get('td').contains('departmentId').should('not.exist')
				cy.get('[data-cy="matchStep.editOption"]').first().click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				// cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				// cy.get('.v-menu__content:visible .v-list-item:visible').contains('Exact').parentsUntil('.v-list-item').click()
				// cy.get('.v-messages__message').contains('Match Type is required.').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('departmentId').parentsUntil('.v-list-item').click()

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.add[0])
							.to.deep.equal({
								propertyName: "departmentId",
								weight: "1"
							})
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "departmentId", name: "departmentId"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"},
							])
					})
				cy.get('td').contains('departmentId').parent().within(() => {
					cy.get('td').contains('Exact').should('exist')
					cy.get('td').contains('1').should('exist')
				})
			})

			it('can change exact to zip', () => {
				cy.get('td').contains('toolSkills').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'toolSkills')
					cy.get('td').eq(1).should('have.text', 'Exact')
					cy.get('td').eq(2).should('have.text', '1')
					cy.get('td').eq(3).should('be.empty')
				})

				cy.get('[data-cy="matchStep.editOption"]').first().click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Zip').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('dd')
				cy.get('.v-messages__message').contains('5-vs-9 Match Weight must be an integer.').should('exist')
				cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('11')

				cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('dd')
				cy.get('.v-messages__message').contains('9-vs-5 Match Weight must be an integer.').should('exist')
				cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('22')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.expand[0]).to.deep.equal({
							propertyName: "toolSkills",
							algorithmRef: "zip-match",
							zip: [
								{origin: 5, weight: "11"},
								{origin: 9, weight: "22"}
							]
						})
						expect(body.steps['2'].options.matchOptions.scoring.expand.length).to.equal(2)
						expect(body.steps['2'].options.matchOptions.scoring.add.length).to.equal(2)
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"},
							])
					})
				cy.get('td').contains('toolSkills').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'toolSkills')
					cy.get('td').eq(1).should('have.text', 'Zip')
					cy.get('td').eq(2).should('be.empty')
					cy.get('td span').contains('5-Matches-9 Boost:').should('exist')
					cy.get('td span').contains('11').should('exist')
					cy.get('td span').contains('9-Matches-5 Boost:').should('exist')
					cy.get('td span').contains('22').should('exist')
				})
			})

			it('can change exact to reduce', () => {
				cy.get('td').contains('toolSkills').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'toolSkills')
					cy.get('td').eq(1).should('have.text', 'Exact')
					cy.get('td').eq(2).should('have.text', '1')
					cy.get('td').eq(3).should('be.empty')
				})

				cy.get('[data-cy="matchStep.editOption"]').first().click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Reduce').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertiesReduceField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('employeeId').parentsUntil('.v-list-item').scrollIntoView()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('firstName').parentsUntil('.v-list-item').click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('languageSkills').parentsUntil('.v-list-item').scrollIntoView()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('lastName').parentsUntil('.v-list-item').click()

				cy.get('.menuable__content__active').invoke('css', 'display', 'none')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.reduce[0]).to.deep.equal({
							algorithmRef: "standard-reduction",
							weight: "1",
							allMatch: {
								property: ["firstName", "lastName"]
							}
						})
						expect(body.steps['2'].options.matchOptions.scoring.expand.length).to.equal(1)
						expect(body.steps['2'].options.matchOptions.scoring.add.length).to.equal(2)
						expect(body.steps['2'].options.matchOptions.scoring.reduce.length).to.equal(2)
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "firstName", name: "firstName"},
								{localname: "lastName", name: "lastName"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"},
							])
					})
				cy.get('td').contains('firstName, lastName').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'firstName, lastName')
					cy.get('td').eq(1).should('have.text', 'Reduce')
					cy.get('td').eq(2).should('have.text', '1')
					cy.get('td').eq(3).should('be.empty')
				})
			})

			it('can edit zip options', () => {
				cy.get('td').contains('country').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'country')
					cy.get('td').eq(1).should('have.text', 'Zip')
					cy.get('td').eq(2).should('be.empty')
					cy.get('td span').contains('5-Matches-9 Boost:').should('exist')
					cy.get('td span').contains('2').should('exist')
					cy.get('td span').contains('9-Matches-5 Boost:').should('exist')
					cy.get('td span').contains('3').should('exist')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(3).click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('11')
				cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('22')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.expand[0]).to.deep.equal({
							propertyName: "country",
							algorithmRef: "zip-match",
							zip: [
								{origin: 5, weight: "11"},
								{origin: 9, weight: "22"}
							]
						})
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"}
							])
					})
					cy.get('td').contains('country').parent().within(() => {
						cy.get('td').eq(0).should('have.text', 'country')
						cy.get('td').eq(1).should('have.text', 'Zip')
						cy.get('td').eq(2).should('be.empty')
						cy.get('td span').contains('5-Matches-9 Boost:').should('exist')
						cy.get('td span').contains('11').should('exist')
						cy.get('td span').contains('9-Matches-5 Boost:').should('exist')
						cy.get('td span').contains('22').should('exist')
					})
			})

			it('can change zip to exact', () => {
				cy.get('td').contains('country').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'country')
					cy.get('td').eq(1).should('have.text', 'Zip')
					cy.get('td').eq(2).should('be.empty')
					cy.get('td span').contains('5-Matches-9 Boost:').should('exist')
					cy.get('td span').contains('2').should('exist')
					cy.get('td span').contains('9-Matches-5 Boost:').should('exist')
					cy.get('td span').contains('3').should('exist')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(3).click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Exact').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('33')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.add[3])
							.to.deep.equal({
								propertyName: "country",
								weight: "33"
							})
						expect(body.steps['2'].options.matchOptions.scoring.expand.length).to.equal(0)
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"}
							])
					})
					cy.get('td').contains('country').parent().within(() => {
						cy.get('td').eq(0).should('have.text', 'country')
						cy.get('td').eq(1).should('have.text', 'Exact')
						cy.get('td').eq(2).should('have.text', '33')
						cy.get('td').eq(3).should('be.empty')
					})
			})

			it('can change zip to reduce', () => {
				cy.get('td').contains('country').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'country')
					cy.get('td').eq(1).should('have.text', 'Zip')
					cy.get('td').eq(2).should('be.empty')
					cy.get('td span').contains('5-Matches-9 Boost:').should('exist')
					cy.get('td span').contains('2').should('exist')
					cy.get('td span').contains('9-Matches-5 Boost:').should('exist')
					cy.get('td span').contains('3').should('exist')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(3).click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Reduce').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertiesReduceField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('employeeId').parentsUntil('.v-list-item').scrollIntoView()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('firstName').parentsUntil('.v-list-item').click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('languageSkills').parentsUntil('.v-list-item').scrollIntoView()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('lastName').parentsUntil('.v-list-item').click()

				cy.get('.menuable__content__active').invoke('css', 'display', 'none')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('33')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.reduce[0])
							.to.deep.equal({
								algorithmRef: "standard-reduction",
								weight: "33",
								allMatch: {
									property: ["firstName", "lastName"]
								}
							})
						expect(body.steps['2'].options.matchOptions.scoring.add.length).to.equal(3)
						expect(body.steps['2'].options.matchOptions.scoring.reduce.length).to.equal(2)
						expect(body.steps['2'].options.matchOptions.scoring.expand.length).to.equal(0)
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "firstName", name: "firstName"},
								{localname: "lastName", name: "lastName"},
								{localname: "city", name: "city"}
							])
					})
					cy.get('td').contains('firstName, lastName').parent().within(() => {
						cy.get('td').eq(0).should('have.text', 'firstName, lastName')
						cy.get('td').eq(1).should('have.text', 'Reduce')
						cy.get('td').eq(2).should('have.text', '33')
						cy.get('td').eq(3).should('be.empty')
					})
			})

			it('can edit reduce options', () => {
				cy.get('td').contains('address, city').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address, city')
					cy.get('td').eq(1).should('have.text', 'Reduce')
					cy.get('td').eq(2).should('have.text', '4')
					cy.get('td').eq(3).should('be.empty')
				})
				cy.get('[data-cy="matchStep.editOption"]').eq(4).click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchOptionDlg.weightField"]').clear().type('334')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.reduce[0]).to.deep.equal({
							algorithmRef: "standard-reduction",
							weight: "334",
							allMatch: {
								property: ["address", "city"]
							}
						})
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "city", name: "city"}
							])
					})
					cy.get('td').contains('address, city').parent().within(() => {
						cy.get('td').eq(0).should('have.text', 'address, city')
						cy.get('td').eq(1).should('have.text', 'Reduce')
						cy.get('td').eq(2).should('have.text', '334')
						cy.get('td').eq(3).should('be.empty')
					})

			})

			it('can change reduce to exact', () => {
				cy.get('td').contains('address, city').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address, city')
					cy.get('td').eq(1).should('have.text', 'Reduce')
					cy.get('td').eq(2).should('have.text', '4')
					cy.get('td').eq(3).should('be.empty')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(4).click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Exact').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('departmentId').parentsUntil('.v-list-item').click()

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.add[3])
							.to.deep.equal({
								propertyName: "departmentId",
								weight: "4"
							})
						expect(body.steps['2'].options.matchOptions.scoring.add.length).to.equal(4)
						expect(body.steps['2'].options.matchOptions.scoring.expand.length).to.equal(1)
						expect(body.steps['2'].options.matchOptions.scoring.reduce.length).to.equal(0)
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "departmentId", name: "departmentId"}
							])
					})
					cy.get('td').contains('departmentId').parent().within(() => {
						cy.get('td').eq(0).should('have.text', 'departmentId')
						cy.get('td').eq(1).should('have.text', 'Exact')
						cy.get('td').eq(2).should('have.text', '4')
						cy.get('td').eq(3).should('be.empty')
					})
			})

			it('can change reduce to zip', () => {
				cy.get('td').contains('address, city').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'address, city')
					cy.get('td').eq(1).should('have.text', 'Reduce')
					cy.get('td').eq(2).should('have.text', '4')
					cy.get('td').eq(3).should('be.empty')
				})

				cy.get('[data-cy="matchStep.editOption"]').eq(4).click()

				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.matchTypeField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Zip').parentsUntil('.v-list-item').click()
				cy.get('.v-messages__message').should('not.exist')

				cy.get('.v-dialog--active [data-cy="matchOptionDlg.propertyNameField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('departmentId').parentsUntil('.v-list-item').click()

				cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('dd')
				cy.get('.v-messages__message').contains('5-vs-9 Match Weight must be an integer.').should('exist')
				cy.get('[data-cy="matchOptionDlg.zip5match9Field"]').clear().type('11')

				cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('dd')
				cy.get('.v-messages__message').contains('9-vs-5 Match Weight must be an integer.').should('exist')
				cy.get('[data-cy="matchOptionDlg.zip9match5Field"]').clear().type('22')

				cy.get('.v-messages__message').should('not.exist')
				cy.get('[data-cy="matchOptionDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.scoring.expand[1])
							.to.deep.equal({
								propertyName: "departmentId",
								algorithmRef: "zip-match",
								zip: [
									{origin: 5, weight: "11"},
									{origin: 9, weight: "22"}
								]
							})
						expect(body.steps['2'].options.matchOptions.scoring.add.length).to.equal(3)
						expect(body.steps['2'].options.matchOptions.scoring.expand.length).to.equal(2)
						expect(body.steps['2'].options.matchOptions.scoring.reduce.length).to.equal(0)
						expect(body.steps['2'].options.matchOptions.propertyDefs.properties)
							.to.deep.equal([
								{localname: "toolSkills", name: "toolSkills"},
								{localname: "address", name: "address"},
								{localname: "employeeId", name: "employeeId"},
								{localname: "country", name: "country"},
								{localname: "departmentId", name: "departmentId"}
							])
					})
					cy.get('td').contains('departmentId').parent().within(() => {
						cy.get('td').eq(0).should('have.text', 'departmentId')
						cy.get('td').eq(1).should('have.text', 'Zip')
						cy.get('td').eq(2).should('be.empty')
						cy.get('td span').contains('5-Matches-9 Boost:').should('exist')
						cy.get('td span').contains('11').should('exist')
						cy.get('td span').contains('9-Matches-5 Boost:').should('exist')
						cy.get('td span').contains('22').should('exist')
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
						expect(body.steps['2'].options.matchOptions.scoring.add.length).to.eq(2)
					})
				cy.contains('td', 'employeeId').should('not.exist')
			})
		})

		describe('Add Threshold', () => {
			it('can add a Notify threshold', () => {
				cy.get('[data-cy="matching.addThresholdBtn"]').click()
				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Name is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight Threshold is required.').should('exist')
				cy.get('.v-messages__message').contains('Action is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight Threshold must be an integer.').should('not.exist')


				cy.get('[data-cy="matchThresholdDlg.nameField"]').type('My Notify Thresh')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').type('dd')
				cy.get('.v-messages__message').contains('Weight Threshold must be an integer.').should('exist')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').clear().type('11')

				cy.get('.v-dialog--active [data-cy="matchThresholdDlg.actionField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Notify').parentsUntil('.v-list-item').click()

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.thresholds.threshold[2])
						.to.deep.eq({
							above: '11',
							label: 'My Notify Thresh',
							action: "notify"
						})
					})
				cy.get('td').contains('My Notify Thresh').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'My Notify Thresh')
					cy.get('td').eq(1).should('have.text', '11')
					cy.get('td').eq(2).should('have.text', 'notify')
				})
			})

			it('can add a Merge threshold', () => {
				cy.get('[data-cy="matching.addThresholdBtn"]').click()
				cy.wait(500)

				cy.get('.v-messages__message').should('not.exist')

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.get('.v-messages__message').contains('Name is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight Threshold is required.').should('exist')
				cy.get('.v-messages__message').contains('Action is required.').should('exist')
				cy.get('.v-messages__message').contains('Weight Threshold must be an integer.').should('not.exist')


				cy.get('[data-cy="matchThresholdDlg.nameField"]').type('My Merge Thresh')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').type('dd')
				cy.get('.v-messages__message').contains('Weight Threshold must be an integer.').should('exist')
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').clear().type('11')

				cy.get('.v-dialog--active [data-cy="matchThresholdDlg.actionField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Merge').parentsUntil('.v-list-item').click()

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.thresholds.threshold[2])
						.to.deep.eq({
							above: '11',
							label: 'My Merge Thresh',
							action: "merge"
						})
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
				cy.wait(500)
				cy.get('[data-cy="matchThresholdDlg.aboveField"]').clear().type('333')
				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.thresholds.threshold[0])
						.to.deep.eq({
							above: '333',
							label: 'merge',
							action: "merge"
						})
					})

				cy.get('td').contains('merge').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'merge')
					cy.get('td').eq(1).should('have.text', '333')
					cy.get('td').eq(2).should('have.text', 'merge')
				})
			})

			it('can change a merge to a notify', () => {
				cy.get('[data-cy="matching.editThresholdBtn"]').first().click()
				cy.wait(500)
				cy.get('.v-dialog--active [data-cy="matchThresholdDlg.actionField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Notify').parentsUntil('.v-list-item').click()

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.thresholds.threshold[0])
						.to.deep.eq({
							above: '32',
							label: 'merge',
							action: "notify"
						})
					})

				cy.get('td').contains('merge').parent().within(() => {
					cy.get('td').eq(0).should('have.text', 'merge')
					cy.get('td').eq(1).should('have.text', '32')
					cy.get('td').eq(2).should('have.text', 'notify')
				})
			})

			it('can change a notify to a merge', () => {
				cy.get('[data-cy="matching.editThresholdBtn"]').last().click()
				cy.wait(500)
				cy.get('.v-dialog--active [data-cy="matchThresholdDlg.actionField"]').parent().click()
				cy.get('.v-menu__content:visible .v-list-item:visible').contains('Merge').parentsUntil('.v-list-item').click()

				cy.get('[data-cy="matchThresholdDlg.saveBtn"]').click()

				cy.wait('@saveFlow')
					.its('request.body')
					.should(body => {
						expect(body.steps['2'].options.matchOptions.thresholds.threshold[1])
						.to.deep.eq({
							above: '1',
							label: 'notify',
							action: "merge"
						})
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
						expect(body.steps['2'].options.matchOptions.thresholds.threshold.length).to.eq(1)
						expect(body.steps['2'].options.matchOptions.thresholds.threshold[0].label).to.eq('notify')
					})

				cy.get('td').contains('merge').should('not.exist')
			})
		})

		describe('Edit and Delete', () => {
			it('can edit the step', () => {
				cy.get('.v-dialog--active').should('not.exist')
				cy.get('[data-cy="flowStep.editButton"]').click()
				cy.get('.v-dialog--active [data-cy="addStepDialog.stepNameField"]').should('have.value', 'MatchTest')
				cy.get('.v-dialog--active [data-cy="addStepDialog.stepTypeField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'Matching')
				})
				cy.get('.v-dialog--active [data-cy="addStepDialog.entityTypeField"]').parent().within(() => {
					cy.get('.v-select__selection').should('have.text', 'Employee')
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
						expect(body.flowName).to.equal('21232f297a57a5a743894a0e4a801fc3')
						expect(body.stepName).to.equal('MatchTest')
					})
				cy.get('#flow-step').should('not.exist')
			})
		})
	})
})
