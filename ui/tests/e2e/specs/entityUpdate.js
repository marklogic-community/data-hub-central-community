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
		createModel('Test Model')
	})
	//add Customer entity
	it('can add a new entity', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		addEntity('Test Model', 'Poet','id')
	})
})

function addEntity(modelName, entityName, propertyName){
	//hidden button for testing
	cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}},"img":"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAoCAYAAACM/rhtAAAAV0lEQVRYR+3SsQ0AMAgEMdh/aYZwQ3HpT0LO7zx/+/y+6UD9oQQTVAHt22CCKqB9G0xQBbRvgwmqgPZtMEEV0L4NJqgC2rfBBFVA+zaYoApo3wYTVAHtDxz6ACmSkz/0AAAAAElFTkSuQmCC"}])
	cy.get('[data-cy="modelerPageVue.addNodeButton"]').click({force:true})
	cy.get('[data-cy="addEntity.entityNameField"]').type(entityName)
	cy.get('[data-cy="addEntity.createEntityButton"]').click()
	//see if the entity appears
	cy.get('[data-cy="createModelVue.currentModelLabel"]').should(($div)=> {
		expect($div.text().trim()).equal(modelName)
	})
	cy.get('[data-cy="entityPickList.addPropertyBtn"]').click()
	cy.get('[data-cy="addProperty.propName"]').type(propertyName)
	cy.get('[data-cy="addProperty.createBtn"]').click()
	cy.get('[data-cy="entityPickList.entityPropertyName"]').should(($td)=> {
		expect($td.text().trim()).equal(propertyName)
	})
}

function createModel(modelName){
	cy.get('[data-cy="cardMenu.createModelButton"]').click()
	cy.get('[data-cy="createModelVue.createModelNameField"]').type(modelName)
	cy.get('[data-cy="createModelVue.createSubmitButton"]').click()
	//validate that the current model is 'Test model'
	cy.get('[data-cy="createModelVue.currentModelLabel"]').should(($div)=> {
		expect($div.text().trim()).equal(modelName)
	})
}
