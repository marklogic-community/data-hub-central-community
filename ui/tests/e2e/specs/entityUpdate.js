describe('End to end test to create and update model', () => {
	beforeEach(() => {
		cy.server()
		cy.route('/api/auth/status', {"appName":null,"authenticated":true,"username":"admin","disallowUpdates":false,"appUsersOnly":false,"needsInstall":false})
		cy.route({
			method: 'PUT',
			url: '/api/models/',
			status: 204,
			response: {}
		})
		cy.route('GET', '/api/models/', [])
		cy.route('/api/auth/profile', {"username":"admin","fullname":null,"emails":null})
		return cy.readFile('tests/e2e/data/model.json')
			.then(file => {
				cy.route('/api/models/model.json', file)
				return cy.readFile('tests/e2e/data/searchResults.json')
			})
			.then(file => {
				cy.route('POST', '/api/explore/entities', file)
			})
	})

	//create new 'Test Model'
	it('can create a new model', () => {
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{}}])
		cy.get('[data-cy="cardMenu.createModelButton"]').click()
		cy.get('[data-cy="createModelVue.createModelNameField"]').type('Test Model')
		cy.get('[data-cy="createModelVue.createSubmitButton"]').click()
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('have.text', 'Test Model')
	})

	it('can delete a model', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.route('GET', '/api/models/', [])
		cy.get('[data-cy="cardMenu.deleteModelButton"]').click()
		cy.get('button').contains('Delete').click()
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('not.contain', 'Test Model')
	})

	// rename model 
	it('can rename a model', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.route('GET', '/api/models/', [])
		cy.get('[data-cy="cardMenu.renameModelButton"]').click()
		cy.get('[data-cy="renameModelVue.renameModelNameField"]').type('A new Model Name')
		cy.get('[data-cy="renameModelVue.renameSubmitButton"]').click()
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('have.text', 'A new Model Name')	
	})

	//add Customer entity
	it('can add a new entity', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.get('[data-cy="modelerPageVue.addNodeButton"]').click({force:true})
		cy.get('[data-cy="addEntity.entityNameField"]').type('Poet')
		cy.get('[data-cy="addEntity.createEntityButton"]').click()
		//see if the entity appears
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('contain', 'Test Model')
		cy.get('[data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('[data-cy="editProperty.propName"]').type('id')
		cy.get('[data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'id')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'String')
	})

	it('can add a new String property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('[data-cy="editProperty.propName"]').type('firstName')
		cy.get('[data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'firstName')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'String')
	})

	it('can add a new Array property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('[data-cy="editProperty.propName"]').type('arrayProp')
		cy.get('[data-cy="editProperty.dataType"]').parentsUntil('.v-select__slot').click()
		cy.get('.v-menu__content:visible .v-list-item').contains('Array').parentsUntil('.v-list-item').click()
		cy.get('[data-cy="editProperty.arrayDataType"]').parentsUntil('.v-select__slot').click()
		cy.get('.v-menu__content:visible').last().find('.v-list-item').contains('Boolean').parentsUntil('.v-list-item').click()
		cy.get('[data-cy="editProperty.advancedBtn"]').click()
		cy.get('[data-cy="prop.isPii"]').should('not.be.checked')
		cy.get('[data-cy="prop.isPrimaryKey"]').should('not.be.checked')
		cy.get('[data-cy="prop.isRequired"]').should('not.be.checked')
		cy.get('[data-cy="prop.isElementRangeIndex"]').should('not.be.checked')
		cy.get('[data-cy="prop.isRangeIndex"]').should('not.be.checked')
		cy.get('[data-cy="prop.isWordLexicon"]').should('not.be.checked')

		cy.get('[data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'arrayProp')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'Boolean[]')
	})

	it('can delete a property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "String" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'address')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'String')

		cy.get('[data-cy="entityPickList.deletePropertyBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('not.contain', 'address')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('not.contain', 'String')
		cy.contains('No properties')
	})

	it('can edit a property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "String"},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "String" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="entityPickList.entityPropertyName"]').first().should('have.text', 'address')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').first().should('have.text', 'String')

		cy.get('[data-cy="entityPickList.editPropertyBtn"]').first().click()
		cy.get('[data-cy="editProperty.propName"]').clear().type('arrayProp')
		cy.get('[data-cy="editProperty.dataType"]').parentsUntil('.v-select__slot').click()
		cy.get('.v-menu__content:visible .v-list-item').contains('Array').parentsUntil('.v-list-item').click()
		cy.get('[data-cy="editProperty.arrayDataType"]').parentsUntil('.v-select__slot').click()
		cy.get('.v-menu__content:visible').last().find('.v-list-item').contains('Boolean').parentsUntil('.v-list-item').click()
		cy.get('[data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').first().should('have.text', 'arrayProp')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').first().should('have.text', 'Boolean[]')
	})

	// TODO: FIX THIS
	it('can not edit a property to an existing one', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "String"},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "String" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="entityPickList.entityPropertyName"]').first().should('have.text', 'address')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').first().should('have.text', 'String')

		cy.get('[data-cy="entityPickList.editPropertyBtn"]').first().click()
		cy.get('[data-cy="editProperty.propName"]').clear().type('id')
		cy.get('[data-cy="editProperty.createBtn"]').click()
		cy.get('.v-messages__message').should('have.text', 'Property already exists')
	})

	it('can show advanced property features', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "String", "isPrimaryKey": true, "isPii": true},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "String" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()
		cy.get('[data-cy="entityPickList.editPropertyBtn"]').last().click()
		cy.get('[data-cy="editProperty.advancedBtn"]').click()
		cy.get('[data-cy="prop.isPii"]').should('be.checked')
		cy.get('[data-cy="prop.isPrimaryKey"]').should('be.checked')
		cy.get('[data-cy="prop.isRequired"]').should('not.be.checked')
		cy.get('[data-cy="prop.isElementRangeIndex"]').should('not.be.checked')
		cy.get('[data-cy="prop.isRangeIndex"]').should('not.be.checked')
		cy.get('[data-cy="prop.isWordLexicon"]').should('not.be.checked')
	})

	// TODO: FIX THIS
	it('can only have one primary key', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "String", "isPrimaryKey": true, "isPii": true},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "String" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()
		cy.get('[data-cy="entityPickList.editPropertyBtn"]').first().click()
		cy.get('[data-cy="editProperty.advancedBtn"]').click()
		cy.get('[data-cy="prop.isPrimaryKey"]').parentsUntil('.v-input__slot').click()
		cy.get('[data-cy="editProperty.createBtn"]').click()
	})
})
