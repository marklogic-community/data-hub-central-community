describe('Integrate Tab', () => {

    beforeEach(() => {
        cy.server()
        cy.route('/api/auth/status', {
            "appName": null,
            "authenticated": true,
            "username": "admin",
            "disallowUpdates": false,
            "appUsersOnly": false
        })
        cy.route({
            method: 'PUT',
            url: '/api/models/',
            status: 204,
            response: {}
        })
        cy.route('GET', '/api/models/', [])
        cy.route('/api/auth/profile', {
            "username": "admin",
            "fullname": null,
            "emails": null
        })
        cy.route('/api/models/current', 'fixture:model.json')
        cy.route('GET', '/api/entities', 'fixture:entities.json')
        cy.route('GET', '/api/flows', 'fixture:flowsEnvision.json')
        cy.route('GET', '/api/flows/21232f297a57a5a743894a0e4a801fc3', 'fixture:flow-envision.json')
        cy.route('GET', '/api/jobs?flowName=21232f297a57a5a743894a0e4a801fc3', 'fixture:jobs.json')
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
                cy.getMenuOption('departmentId').click()
                cy.get('.v-messages__message').contains('Property to Merge is required.').should('not.exist')

                cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().click()
                cy.getMenuOption('My Strategy').click()
                cy.get('.v-messages__message').contains('Strategy is required.').should('not.exist')

                cy.get('.v-messages__message').should('not.exist')
                cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

                cy.wait('@saveFlow')
                    .its('request.body')
                    .should(body => {
                        //cy.log(JSON.stringify(body));
                        expect(JSON.stringify(body.steps['3'].mergeRules[1]))
                            .to.deep.equal(JSON.stringify({
                                "entityPropertyPath": "departmentId",
                                "mergeType": "strategy",
                                "length": {
                                    "weight": null
                                },
                                "sourceWeights": [],
                                "mergeStrategyName": "My Strategy"
                            }))
                    })
                cy.get('#optionsTable td').contains('departmentId').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'departmentId')
                    cy.get('td').eq(1).should('have.text', 'My Strategy')
                    cy.get('td').eq(2).should('have.text', '2')
                    cy.get('td').eq(3).should('have.text', '3')
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '3')
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
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '3')
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
                cy.getMenuOption('departmentId').click()
                cy.get('.v-messages__message').should('not.exist')
                cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

                cy.wait('@saveFlow')
                    .its('request.body')
                    .should(body => {
                        expect(JSON.stringify(body.steps['3'].mergeRules[0]))
                            .to.deep.equal(JSON.stringify({
                                "entityPropertyPath": "departmentId",
                                "mergeStrategyName": "My Strategy",
                                "maxValues": "2",
                                "priorityOrder": {
                                    "lengthWeight": 55,
                                    "sources": [{
                                        "sourceName": "source1",
                                        "weight": "2"
                                    }, {
                                        "sourceName": "source2",
                                        "weight": "3"
                                    }]
                                },
                                "strategyName": "My Strategy",
                                "maxSources": "3",
                                "raw": {
                                    "entityPropertyPath": "address",
                                    "mergeStrategyName": "My Strategy",
                                    "maxValues": 1,
                                    "priorityOrder": {
                                        "lengthWeight": "10",
                                        "sources": []
                                    }
                                },
                                "mergeType": "strategy",
                                "length": {
                                    "weight": null
                                },
                                "sourceWeights": []
                            }))
                    })
                cy.get('#optionsTable td').contains('departmentId').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'departmentId')
                    cy.get('td').eq(1).should('have.text', 'My Strategy')
                    cy.get('td').eq(2).should('have.text', '2')
                    cy.get('td').eq(3).should('have.text', '3')
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '3')
                    cy.get('td').eq(5).should('have.text', '55')
                })
            })

            it('should change an options strategy', () => {
                cy.get('#optionsTable td').contains('address').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'address')
                    cy.get('td').eq(1).should('have.text', 'My Strategy')
                    cy.get('td').eq(2).should('have.text', '2')
                    cy.get('td').eq(3).should('have.text', '3')
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '3')
                    cy.get('td').eq(5).should('have.text', '55')
                })

                cy.get('[data-cy="mergeStep.editOption"]').click()

                cy.getActiveDialog()
                cy.get('.v-dialog--active [data-cy="mergeOptionDlg.propertyNameField"]').parent().within(() => {
                    cy.get('.v-select__selection').should('have.text', 'address')
                })
                cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().within(() => {
                    cy.get('.v-select__selection').should('have.text', 'My Strategy')
                })

                cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().click()
                cy.getMenuOption('My Strategy2').click()
                cy.get('.v-messages__message').should('not.exist')

                cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

                cy.wait('@saveFlow')
                    .its('request.body')
                    .should(body => {
                        expect(JSON.stringify(body.steps['3'].mergeRules[0]))
                            .to.deep.equal(JSON.stringify({
                                "entityPropertyPath": "address",
                                "mergeStrategyName": "My Strategy2",
                                "maxValues": "2",
                                "priorityOrder": {
                                    "lengthWeight": 55,
                                    "sources": [{
                                        "sourceName": "source1",
                                        "weight": "2"
                                    }, {
                                        "sourceName": "source2",
                                        "weight": "3"
                                    }]
                                },
                                "strategyName": "My Strategy",
                                "maxSources": "3",
                                "raw": {
                                    "entityPropertyPath": "address",
                                    "mergeStrategyName": "My Strategy",
                                    "maxValues": 1,
                                    "priorityOrder": {
                                        "lengthWeight": "10",
                                        "sources": []
                                    }
                                },
                                "mergeType": "strategy",
                                "length": {
                                    "weight": null
                                },
                                "sourceWeights": []
                            }))
                    })
                cy.get('#optionsTable td').contains('address').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'address')
                    cy.get('td').eq(1).should('have.text', 'My Strategy2')
                    cy.get('td').eq(2).should('have.text', '22')
                    cy.get('td').eq(3).should('have.text', '333')
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '22')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '33')
                    cy.get('td').eq(5).should('have.text', '555')
                })
            })

            it('should change an options property and strategy', () => {
                cy.get('#optionsTable td').contains('address').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'address')
                    cy.get('td').eq(1).should('have.text', 'My Strategy')
                    cy.get('td').eq(2).should('have.text', '2')
                    cy.get('td').eq(3).should('have.text', '3')
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '3')
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
                cy.getMenuOption('departmentId').click()

                cy.get('.v-dialog--active [data-cy="mergeOptionDlg.strategyField"]').parent().click()
                cy.getMenuOption('My Strategy2').click()
                cy.get('.v-messages__message').should('not.exist')

                cy.get('[data-cy="mergeOptionDlg.saveBtn"]').click()

                cy.wait('@saveFlow')
                    .its('request.body')
                    .should(body => {
                        expect(JSON.stringify(body.steps['3'].mergeRules[0]))
                            .to.deep.equal(JSON.stringify({
                                "entityPropertyPath": "departmentId",
                                "mergeStrategyName": "My Strategy2",
                                "maxValues": "2",
                                "priorityOrder": {
                                    "lengthWeight": 55,
                                    "sources": [{
                                        "sourceName": "source1",
                                        "weight": "2"
                                    }, {
                                        "sourceName": "source2",
                                        "weight": "3"
                                    }]
                                },
                                "strategyName": "My Strategy",
                                "maxSources": "3",
                                "raw": {
                                    "entityPropertyPath": "address",
                                    "mergeStrategyName": "My Strategy",
                                    "maxValues": 1,
                                    "priorityOrder": {
                                        "lengthWeight": "10",
                                        "sources": []
                                    }
                                },
                                "mergeType": "strategy",
                                "length": {
                                    "weight": null
                                },
                                "sourceWeights": []
                            }))
                    })
                cy.get('#optionsTable td').contains('departmentId').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'departmentId')
                    cy.get('td').eq(1).should('have.text', 'My Strategy2')
                    cy.get('td').eq(2).should('have.text', '22')
                    cy.get('td').eq(3).should('have.text', '333')
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '22')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '33')
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
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '3')
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
                        expect(body.steps['3'].mergeRules.length).to.equal(1)
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
                        expect(JSON.stringify(body.steps['3'].mergeStrategies[2]))
                            .to.deep.equal(JSON.stringify({
                                "maxSources": "22",
                                "maxValues": "11",
                                "strategyName": "My Fun Strategy",
                                "priorityOrder": {
                                    "sources": [{
                                        "sourceName": "source1",
                                        "weight": "33"
                                    }, {
                                        "sourceName": "source3",
                                        "weight": "3333"
                                    }],
                                    "lengthWeight": "44"
                                }
                            }))
                    })
                cy.get('#strategiesTable td').contains('My Fun Strategy').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'My Fun Strategy')
                    cy.get('td').eq(1).should('have.text', '11')
                    cy.get('td').eq(2).should('have.text', '22')
                    cy.get('td').eq(3).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(3).find('span').eq(1).should('have.text', '33')
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
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '3')
                    cy.get('td').eq(5).should('have.text', '55')
                })
                // verify strategies
                cy.get('#strategiesTable td').contains('My Strategy').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'My Strategy')
                    cy.get('td').eq(1).should('have.text', '2')
                    cy.get('td').eq(2).should('have.text', '3')
                    cy.get('td').eq(3).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(3).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(3).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(3).find('span').eq(3).should('have.text', '3')
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
                        expect(JSON.stringify(body.steps['3'].mergeStrategies[0]))
                            .to.deep.equal(JSON.stringify({
                                "strategyName": "My New Name",
                                "maxValues": "2",
                                "maxSources": "3",
                                "priorityOrder": {
                                    "sources": [{
                                        "sourceName": "source1",
                                        "weight": "2"
                                    }, {
                                        "sourceName": "source2",
                                        "weight": "3"
                                    }],
                                    "lengthWeight": 55
                                }
                            }))
                    })

                // verify option strategy name updated
                cy.get('#optionsTable td').contains('address').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'address')
                // TODO: Uncomment once the defect is fixed
                    /*cy.get('td').eq(1).should('have.text', 'My New Name')
                    cy.get('td').eq(2).should('have.text', '2')
                    cy.get('td').eq(3).should('have.text', '3')
                    cy.get('td').eq(4).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(4).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(4).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(4).find('span').eq(3).should('have.text', '3')
                    cy.get('td').eq(5).should('have.text', '55')*/
                })
                cy.get('#strategiesTable td').contains('My New Name').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'My New Name')
                    cy.get('td').eq(1).should('have.text', '2')
                    cy.get('td').eq(2).should('have.text', '3')
                    cy.get('td').eq(3).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(3).find('span').eq(1).should('have.text', '2')
                    cy.get('td').eq(3).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(3).find('span').eq(3).should('have.text', '3')
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

                // length weight field
                cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.lengthWeightField"]').clear().type('aa')
                cy.get('.v-messages__message').contains('Length Weight must be an integer.').should('exist')
                cy.get('.v-dialog--active [data-cy="mergeStrategyDlg.lengthWeightField"]').clear().type('44')
                cy.get('.v-messages__message').contains('Length Weight must be an integer.').should('not.exist')

                cy.get('[data-cy="mergeStrategyDlg.saveBtn"]').click()
                cy.wait('@saveFlow')
                    .its('request.body')
                    .should(body => {
                        expect(JSON.stringify(body.steps['3'].mergeStrategies[0]))
                            .to.deep.equal(JSON.stringify({
                                "strategyName": "My New Name",
                                "maxValues": "11",
                                "maxSources": "22",
                                "priorityOrder": {
                                    "sources": [{
                                        "sourceName": "source1",
                                        "weight": "2"
                                    }, {
                                        "sourceName": "source2",
                                        "weight": "3"
                                    }],
                                    "lengthWeight": "44"
                                }
                            }))
                    })

                // add a 3rd source weight
                cy.get('[data-cy="merging.editStrategyBtn"]').first().click()
                cy.wait(500)
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
                cy.get('[data-cy="mergeStrategyDlg.cancelBtn"]').click()

                cy.get('#strategiesTable td').contains('My New Name').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'My New Name')
                    cy.get('td').eq(1).should('have.text', '11')
                    cy.get('td').eq(2).should('have.text', '22')
                    cy.get('td').eq(3).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(3).find('span').eq(1).should('have.text', '2')
                // TODO: Uncomment once the defect is fixed
                   /*cy.get('td').eq(3).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(3).find('span').eq(3).should('have.text', '3')*/
                    cy.get('td').eq(4).should('have.text', '44')
                })
            })
        })

        describe('Delete Strategy', () => {
        	// TODO: Uncomment once the defect is fixed
            /*it('cant delete a strategy in use', () => {
            	cy.get('[data-cy="merging.deleteStrategyBtn"]').first().should('be.disabled')
            	cy.get('[data-cy="mergeStep.deleteOption"]').first().click()
            	cy.get('button').contains('Delete').click()
            	cy.get('[data-cy="merging.deleteStrategyBtn"]').first().should('not.be.disabled')
            })*/

            it('can delete a strategy', () => {
                cy.get('#strategiesTable td').contains('My Strategy2').parent().within(() => {
                    cy.get('td').eq(0).should('have.text', 'My Strategy2')
                    cy.get('td').eq(1).should('have.text', '22')
                    cy.get('td').eq(2).should('have.text', '333')
                    cy.get('td').eq(3).find('span').eq(0).should('have.text', 'source1:')
                    cy.get('td').eq(3).find('span').eq(1).should('have.text', '22')
                    cy.get('td').eq(3).find('span').eq(2).should('have.text', 'source2:')
                    cy.get('td').eq(3).find('span').eq(3).should('have.text', '33')
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
                        expect(body.steps['3'].mergeStrategies.length).to.equal(1)
                        expect(JSON.stringify(body.steps['3'].mergeStrategies[0]))
                            .to.deep.equal(JSON.stringify({
                                "strategyName": "My Strategy",
                                "maxValues": "2",
                                "maxSources": "3",
                                "priorityOrder": {
                                    "lengthWeight": 55,
                                    "sources": [{
                                        "sourceName": "source1",
                                        "weight": "2"
                                    }, {
                                        "sourceName": "source2",
                                        "weight": "3"
                                    }]
                                }
                            }))
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
                    //cy.get('.v-select__selection').should('have.text', 'Employee')
                })
                cy.get('.v-dialog--active [data-cy="addStepDialog.dataSourceField"]').parent().within(() => {
                    cy.get('.v-select__selection').should('have.text', 'DataSource3')
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
                        expect(body.stepName).to.equal('MergeTest')
                    })
                cy.get('#flow-step').should('not.exist')
            })
        })
    })
})