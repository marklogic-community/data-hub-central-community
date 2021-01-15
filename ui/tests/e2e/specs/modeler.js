describe('End to end test to create and update model', () => {
	beforeEach(() => {
		cy.server()
		cy.route('/api/auth/status', {"appName":null,"authenticated":true,"username":"admin","disallowUpdates":false,"appUsersOnly":false})
		cy.route({
			method: 'PUT',
			url: '/api/models/',
			status: 204,
			response: {}
		}).as('saveModel')
		cy.route({
			method: 'POST',
			url: '/api/models/rename',
			status: 204,
			response: {}
		}).as('renameModel')
		cy.route('GET', '/api/models/', [])
		cy.route('GET', '/api/entities', 'fixture:entities.json')
		cy.route('/api/auth/profile', {"username":"admin","fullname":null,"emails":null})
		cy.route('/api/models/current', 'fixture:model.json')
		cy.route('POST', '/api/explore/entities', 'fixture:searchResults.json')
		cy.route('POST', '/api/mastering/notifications', 'fixture:notificationsPage1.json').as('getNotifications')
	})

	it('makes you create a new model', () => {
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.route('GET', '/api/models/', [])
		cy.wait('@getNotifications')

		cy.get('h2').contains('Please Create or Load a Model to begin').should('exist')
		cy.get('[data-cy="modeler.entityFilter"]').should('not.exist')
		cy.get('[data-cy="modeler.addEntity"]').should('be.disabled')
		cy.get('[data-cy="modeler.addRelationship"]').should('be.disabled')
		cy.get('[data-cy="cardMenu.renameModelButton"').should('be.disabled')
		cy.get('[data-cy="cardMenu.saveImageButton"').should('be.disabled')
		cy.get('[data-cy="cardMenu.deleteModelButton"').should('be.disabled')

		cy.get('[data-cy="cardMenu.createModelButton"]').click({force: true})
		cy.get('.v-menu__content.menuable__content__active').should('be.visible')
		cy.wait(1000)
		cy.get('[data-cy="createModelVue.createModelNameField"]').type('Test Model')
		cy.get('[data-cy="createModelVue.createSubmitButton"]').click()
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('have.text', 'Test Model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body).to.deep.equal({"name":"Test Model","edges":{},"nodes":{}})
			})

		cy.get('[data-cy="modeler.entityFilter"]').should('exist')
		cy.get('[data-cy="modeler.addEntity"]').should('not.be.disabled')
		cy.get('[data-cy="modeler.addRelationship"]').should('not.be.disabled')
		cy.get('[data-cy="cardMenu.renameModelButton"').should('not.be.disabled')
		cy.get('[data-cy="cardMenu.saveImageButton"').should('not.be.disabled')
		cy.get('[data-cy="cardMenu.deleteModelButton"').should('not.be.disabled')
	})

	it('can create a new model', () => {
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{}}])
		cy.wait('@getNotifications')
		cy.get('[data-cy="cardMenu.createModelButton"]').click({force: true})
		cy.get('.v-menu__content.menuable__content__active').should('be.visible')
		cy.wait(1000)
		cy.get('[data-cy="createModelVue.createModelNameField"]').type('Test Model')
		cy.get('[data-cy="createModelVue.createSubmitButton"]').click()
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('have.text', 'Test Model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body).to.deep.equal({"name":"Test Model","edges":{},"nodes":{}})
			})
	})

	it('can delete a model', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body).to.deep.equal({"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}})
			})
		cy.route('GET', '/api/models/', [])
		cy.route('POST', '/api/models/delete', {}).as('deleteModel')
		cy.get('[data-cy="cardMenu.deleteModelButton"]').click()
		cy.wait(1000)
		cy.get('.menuable__content__active button').contains('Delete').click()
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('not.exist')
		cy.wait('@deleteModel')
			.its('request.body')
			.should(body => {
				expect(body).to.deep.equal({"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}})
			})
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body).to.be.null
			})
	})

	// rename model
	it('can rename a model', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.route('GET', '/api/models/', [])
		cy.wait('@getNotifications')
		cy.get('[data-cy="cardMenu.renameModelButton"]').click({force: true})
		cy.get('.v-menu__content.menuable__content__active').should('be.visible')
		cy.wait(1000)
		cy.get('[data-cy="renameModelVue.renameModelNameField"]').type('A new Model Name')
		cy.get('[data-cy="renameModelVue.renameSubmitButton"]').click()
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('have.text', 'A new Model Name')
		cy.wait('@renameModel')
			.its('request.body')
			.should(body => {
				expect(body.model).to.deep.equal({"name":"A new Model Name","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}})
				expect(body.originalname).to.equal('Test Model')
				expect(body.newname).to.equal('A new Model Name')
			})
	})

	// add Customer entity
	it('cannot add a new entity with invalid name', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}]).as('getModels')
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.wait('@getModels')
			.its('response.body')
				.should((body) => {
					expect(body.length).to.equal(1)
					expect(body[0].name).to.equal('Test Model')
				})
		cy.wait('@getModels')
			.its('response.body')
				.should((body) => {
					expect(body.length).to.equal(1)
					expect(body[0].name).to.equal('Test Model')
				})
		cy.get('[data-cy="modelerPageVue.addNodeButton"]').click({force:true})
		cy.get('.v-messages__message').should('not.exist')
		// cy.get('.v-messages__message').should('not.contain', 'Entity Name cannot contain spaces. Only letters, numbers, and underscore')
		cy.get('[data-cy="addEntity.entityNameField"]').clear()
		cy.get('[data-cy="addEntity.createEntityButton"]').click()
		cy.get('.v-messages__message').should('contain', 'Entity name is required')

		cy.get('[data-cy="addEntity.entityNameField"]').clear().type('A Poet')
		cy.get('[data-cy="addEntity.createEntityButton"]').click()
		cy.get('.v-messages__message').should('contain', 'Entity Name cannot contain spaces. Only letters, numbers, and underscore')

		cy.get('[data-cy="addEntity.entityNameField"]').clear().type('Poet')
		cy.get('[data-cy="addEntity.createEntityButton"]').click()
		cy.wait(1000)
		cy.get('.v-messages__message').should('contain', 'Entity already exists')
	})

	it('cannot add a new entity with invalid iri', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.v-messages__message').should('not.exist')
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.get('[data-cy="modelerPageVue.addNodeButton"]').click({force:true})
		cy.get('[data-cy="addEntity.entityNameField"]').type('Poet')
		cy.get('[data-cy="addEntity.advancedBtn"]').click()
		cy.get('[data-cy="addEntity.iriField"]').clear().type('http://blah')
		cy.get('[data-cy="addEntity.createEntityButton"]').click()

		cy.get('.v-messages__message').should('contain', 'A valid IRI is required, e.g. http://marklogic.envision.com/')
	})

	it('can add a new entity', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.wait('@saveModel')
		.its('request.body')
		.should(body => {
			expect(body).to.deep.equal({"name":"Test Model","edges":{},"nodes":{}})
		})

		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.get('[data-cy="modelerPageVue.addNodeButton"]').click({force:true})
		cy.get('[data-cy="addEntity.entityNameField"]').type('Poet')
		cy.get('[data-cy="addEntity.createEntityButton"]').click()

		//see if the entity appears
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('contain', 'Test Model')
		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('id')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'id')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'string')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes).to.deep.equal({ "poet": { "version": "0.0.1", "baseUri": "http://marklogic.envision.com/", "id": "poet", "label": "Poet", "entityName": "Poet", "type": "entity", "properties": [] }})
			})
	})

	it('can delete an entity', () => {
		cy.route('GET', '/api/models/', 'fixture:models.json')
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')

		// delete category
		cy.get('[data-cy=nodeList]').should('contain', 'address')
		cy.get('[data-cy=nodeList]').should('contain', 'category')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')
		cy.get('[data-cy=nodeList]').should('contain', 'product')
		cy.get('[data-cy=nodeList]').contains("category").click()

		cy.get('[data-cy="modeler.deleteSelected"]').click()
		cy.get('.menuable__content__active button').contains('Cancel').click()
		cy.get('[data-cy=nodeList]').should('contain', 'address')
		cy.get('[data-cy=nodeList]').should('contain', 'category')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')
		cy.get('[data-cy=nodeList]').should('contain', 'product')

		cy.get('[data-cy="modeler.deleteSelected"]').click()
		cy.get('.menuable__content__active button').contains('Delete').click()
		cy.get('[data-cy=nodeList]').should('contain', 'address')
		cy.get('[data-cy=nodeList]').should('not.contain', 'category')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')
		cy.get('[data-cy=nodeList]').should('contain', 'product')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				console.log(JSON.stringify(body.edges))
				expect(body.edges['product-hascategory-category']).to.be.undefined
				expect(body.nodes.category).to.be.undefined
			})

		// delete product
		cy.get('[data-cy=nodeList]').should('contain', 'address')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')
		cy.get('[data-cy=nodeList]').should('contain', 'product')
		cy.get('[data-cy=nodeList]').contains("product").click()

		cy.get('[data-cy="modeler.deleteSelected"]').click()
		cy.get('.menuable__content__active button').contains('Cancel').click()
		cy.get('[data-cy=nodeList]').should('contain', 'address')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')
		cy.get('[data-cy=nodeList]').should('contain', 'product')

		cy.get('[data-cy="modeler.deleteSelected"]').click()
		cy.get('.menuable__content__active button').contains('Delete').click()
		cy.get('[data-cy=nodeList]').should('contain', 'address')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')
		cy.get('[data-cy=nodeList]').should('not.contain', 'product')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				console.log(JSON.stringify(body.edges))
				expect(body.edges['order-has-product']).to.be.undefined
				expect(body.nodes.product).to.be.undefined
				const address = body.nodes.customer.properties.find(p => p.name === 'homeAddress')
				expect(address).to.exist
				expect(body.nodes.address).to.exist
			})

		// delete address
		cy.get('[data-cy=nodeList]').should('contain', 'address')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')
		cy.get('[data-cy=nodeList]').contains("address").click()

		cy.get('[data-cy="modeler.deleteSelected"]').click()
		cy.get('.menuable__content__active button').contains('Cancel').click()
		cy.get('[data-cy=nodeList]').should('contain', 'address')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')

		cy.get('[data-cy="modeler.deleteSelected"]').click()
		cy.get('.menuable__content__active button').contains('Delete').click()
		cy.get('[data-cy=nodeList]').should('not.contain', 'address')
		cy.get('[data-cy=nodeList]').should('contain', 'city')
		cy.get('[data-cy=nodeList]').should('contain', 'customer')
		cy.get('[data-cy=nodeList]').should('contain', 'order')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				console.log(JSON.stringify(body.nodes.customer))
				const address = body.nodes.customer.properties.find(p => p.name === 'homeAddress')
				expect(address).to.be.undefined
				expect(body.nodes.address).to.be.undefined
			})
	})

	it('can delete an edge', () => {
		cy.route('GET', '/api/models/', [ { "name": "Test Model", "nodes": { "poet": { "id": "poet", "x": -156.3861003861004, "y": -130.42857142857144, "label": "Poet", "entityName": "Poet", "type": "entity", "properties": [] }, "philosopher": { "id": "philosopher", "x": -156.3861003861004, "y": 100.42857142857144, "label": "Philosopher", "entityName": "Philosopher", "type": "entity", "properties": [] } }, "edges": { "poet-is-philosopher": { "id": "poet-is-philosopher", "from": "poet", "label": "is", "to": "philosopher", "cardinality": "1:Many", "keyFrom": "orderId", "keyTo": "orderId", "smooth": { "roundness": 0.5 } } } } ])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.edges['poet-is-philosopher']).to.deep.equal({
					"id": "poet-is-philosopher",
					"from": "poet",
					"label": "is",
					"to": "philosopher",
					"cardinality": "1:Many",
					"keyFrom": "orderId",
					"keyTo": "orderId",
					"smooth": {
						"roundness": 0.5
					}
				})
			})
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').should('contain', 'poet')
		cy.get('[data-cy=nodeList]').should('contain', 'philosopher')
		cy.get('[data-cy=edgeList]').should('contain', 'poet-is-philosopher')
		cy.get('[data-cy=edgeList]').contains('poet-is-philosopher').click()

		cy.get('[data-cy="modeler.deleteSelected"]').click()
		cy.get('.menuable__content__active button').contains('Cancel').click()
		cy.get('[data-cy=nodeList]').should('contain', 'poet')
		cy.get('[data-cy=nodeList]').should('contain', 'philosopher')
		cy.get('[data-cy=edgeList]').should('contain', 'poet-is-philosopher')

		cy.get('[data-cy="modeler.deleteSelected"]').click()
		cy.get('.menuable__content__active button').contains('Delete').click()
		cy.get('[data-cy=nodeList]').should('contain', 'poet')
		cy.get('[data-cy=nodeList]').should('contain', 'philosopher')
		cy.get('[data-cy=edgeList]').should('not.exist')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.edges).to.deep.equal({})
			})
	})

	it('can create and edit 2 connected entities', () => {
		cy.route('GET', '/api/models/', [ { "name": "Test Model", "edges": {}, "nodes": {} } ])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body).to.deep.equal({ "name": "Test Model", "edges": {}, "nodes": {} })
			})

		// create the poet entity
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.get('[data-cy="modelerPageVue.addNodeButton"]').click({force:true})
		cy.get('[data-cy="addEntity.entityNameField"]').type('Poet')
		cy.get('[data-cy="addEntity.createEntityButton"]').click()

		//see if the entity appears
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('contain', 'Test Model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes).to.deep.equal({ "poet": { "version": "0.0.1", "baseUri": "http://marklogic.envision.com/", "id": "poet", "label": "Poet", "entityName": "Poet", "type": "entity", "properties": [] }})
			})

		// create the philosopher entity
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}, "philosopher": { "id": "philosopher", "x": -156.3861003861004, "y": 100.42857142857144, "label": "Philosopher", "entityName": "Philosopher", "type": "entity", "properties": [] }}}])
		cy.get('[data-cy="modelerPageVue.addNodeButton"]').click({force:true})
		cy.get('[data-cy="addEntity.entityNameField"]').type('Philosopher')
		cy.get('[data-cy="addEntity.createEntityButton"]').click()

		// //see if the entity appears
		cy.get('[data-cy="createModelVue.currentModelLabel"]').should('contain', 'Test Model')

		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				delete body.img
				expect(body).to.deep.equal({"name":"Test Model","edges":{},"nodes":{"poet":{"baseUri": "http://marklogic.envision.com/", "id":"poet","label":"Poet","entityName":"Poet","type":"entity","properties":[],"version":"0.0.1"}, "philosopher": { "baseUri": "http://marklogic.envision.com/", "id": "philosopher", "label": "Philosopher", "entityName": "Philosopher", "type": "entity", "properties": [], "version": "0.0.1" }}})
			})

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"isArray": false, "isElementRangeIndex": false, "isPii": false, "isPrimaryKey": false, "isRangeIndex": false, "isRequired": false, "isWordLexicon": false, "name": "id", "type": "string", "_propId": "6d743ab9-ca56-44b0-83a7-8684b531d60e"}]}, "philosopher": { "id": "philosopher", "x": -156.3861003861004, "y": 100.42857142857144, "label": "Philosopher", "entityName": "Philosopher", "type": "entity", "properties": [] }}}])

		// add id property
		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('id')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get(".v-expansion-panel--active").find('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'id')
		cy.get(".v-expansion-panel--active").find('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'string')

		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties.map(p => p.name)).to.deep.equal(['id'])
				expect(body.nodes.philosopher.properties.map(p => p.name)).to.deep.equal([])
			})

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('firstName')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'idfirstName')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'stringstring')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties.map(p => p.name)).to.deep.equal(['id', 'firstName'])
				expect(body.nodes.philosopher.properties.map(p => p.name)).to.deep.equal([])
			})
		// cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"isArray": false, "isElementRangeIndex": false, "isPii": false, "isPrimaryKey": false, "isRangeIndex": false, "isRequired": false, "isWordLexicon": false, "name": "id", "type": "string", "_propId": "6d743ab9-ca56-44b0-83a7-8684b531d60e"},{"isArray": false, "isElementRangeIndex": false, "isPii": false, "isPrimaryKey": false, "isRangeIndex": false, "isRequired": false, "isWordLexicon": false, "name": "firstName", "type": "string", "_propId": "6d743ab9-ca56-44b0-83a7-8684b531d6ff"}]}, "philosopher": { "id": "philosopher", "x": -156.3861003861004, "y": 100.42857142857144, "label": "Philosopher", "entityName": "Philosopher", "type": "entity", "properties": [] }}}])

		cy.get('[data-cy=nodeList]').contains("philosopher").click()
		cy.get('[data-cy="panel-philosopher"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('id')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'ididfirstName')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'stringstringstring')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties.map(p => p.name)).to.deep.equal(['id', 'firstName'])
				expect(body.nodes.philosopher.properties.map(p => p.name)).to.deep.equal(['id'])
			})
	})

	it('can add a new String property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([])
			})

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('firstName')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'firstName')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'string')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties[0].name).to.equal('firstName')
				expect(body.nodes.poet.properties[0].type).to.equal('string')
				expect(body.nodes.poet.properties[0].isArray).to.equal(false)
			})
	})

	it('can add a new Array property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([])
			})

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('arrayProp')
		cy.get('.v-dialog--active [data-cy="editProperty.dataType"]').click()
		cy.get('.v-list-item__title:visible').contains('array').parentsUntil('.v-list-item').click()
		cy.get('.v-dialog--active [data-cy="editProperty.arrayDataType"]').click()
		cy.get('.v-list-item:visible').contains('boolean').parentsUntil('.v-list-item').click()
		cy.get('.v-dialog--active [data-cy="editProperty.advancedBtn"]').click()
		cy.get('.v-dialog--active [data-cy="prop.isPii"]').should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isRequired"]').should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isElementRangeIndex"]').should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isRangeIndex"]').should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isWordLexicon"]').should('not.be.checked')

		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'arrayProp')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'boolean[]')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties[0].name).to.equal('arrayProp')
				expect(body.nodes.poet.properties[0].type).to.equal('boolean')
				expect(body.nodes.poet.properties[0].isArray).to.equal(true)
			})
	})

	it('can drag to reorder properties', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","label":"Poet","entityName":"Poet","type":"entity","properties":[{ "_propId": "abc123", "name": "a", "type": "string" }, { "_propId": "def456", "name": "b", "type": "string" }, { "_propId": "ghi789", "name": "c", "type": "string" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body).to.deep.equal({"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","label":"Poet","entityName":"Poet","type":"entity","properties":[{ "_propId": "abc123", "name": "a", "type": "string" }, { "_propId": "def456", "name": "b", "type": "string" }, { "_propId": "ghi789", "name": "c", "type": "string" }]}}})
			})

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.wait(1000)

		cy.get('[data-cy="entityPickList.propertyRow"]').eq(0).within(() => {
			cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'a')
		})
		cy.get('[data-cy="entityPickList.propertyRow"]').eq(1).within(() => {
			cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'b')
		})
		cy.get('[data-cy="entityPickList.propertyRow"]').eq(2).within(() => {
			cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'c')
		})

		cy.get('[data-cy="entityPickList.propertyRow"][name="c"]')
			.move('div.properties', '[data-cy="entityPickList.propertyRow"]', 0)

		cy.get('[data-cy="entityPickList.propertyRow"]').eq(0).within(() => {
			cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'a')
		})
		cy.get('[data-cy="entityPickList.propertyRow"]').eq(1).within(() => {
			cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'c')
		})
		cy.get('[data-cy="entityPickList.propertyRow"]').eq(2).within(() => {
			cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'b')
		})
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([{ "_propId": "abc123", "name": "a", "type": "string" }, { "_propId": "ghi789", "name": "c", "type": "string" }, { "_propId": "def456", "name": "b", "type": "string" }])
			})
	})

	it('can delete a property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{ "_propId": "abc123", "name": "address", "type": "string" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([
					{
						"_propId": "abc123",
						"name": "address",
						"type": "string"
					}
				])
			})

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.entityPropertyName"]').should('have.text', 'address')
		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.entityPropertyType"]').should('have.text', 'string')
		cy.get('[data-cy="panel-poet"] [data-cy="deleteDataConfirm.deleteButton"]').click()
		cy.get('.menuable__content__active button').contains('Cancel').click()
		cy.get('[data-cy="deleteDataConfirm.deleteButton"]').click()
		cy.get('.menuable__content__active button').contains('Delete').click()
		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.entityPropertyName"]').should('not.exist')
		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.entityPropertyType"]').should('not.exist')
		cy.contains('No properties')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([])
			})
	})

	it('can edit a property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "string"},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "string" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.wait('@saveModel')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.entityPropertyName"]').last().should('have.text', 'address')
		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.entityPropertyType"]').last().should('have.text', 'string')

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.editPropertyBtn"]').last().click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').should('be.visible')
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').clear().type('arrayProp')
		cy.get('.v-dialog--active [data-cy="editProperty.dataType"]').click()
		cy.contains('.menuable__content__active .v-list-item', 'array').click()
		cy.get('.v-dialog--active [data-cy="editProperty.arrayDataType"]').click()
		cy.get('.v-menu__content:visible .v-list-item:visible').contains('boolean').parentsUntil('.v-list-item').click()
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').last().should('have.text', 'arrayProp')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').last().should('have.text', 'boolean[]')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([
					{
						"_propId": "abc123",
						"name": "id",
						"type": "string"
					},
					{
						"_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7",
						"name": "arrayProp",
						"type": "boolean",
						"isArray": true,
						"isStructured": false
					}
				])
			})
	})

	it('can rename a property', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"QuestionnaireAnswers":{"id":"questionnaireanswers","x":-156.3861003861004,"y":-130.42857142857144,"label":"QuestionnaireAnswers","entityName":"QuestionnaireAnswers","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "string"},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "string" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.wait('@saveModel')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("questionnaireanswers").click()

		cy.get('[data-cy="panel-questionnaireanswers"] [data-cy="entityPickList.entityPropertyName"]').last().should('have.text', 'address')
		cy.get('[data-cy="panel-questionnaireanswers"] [data-cy="entityPickList.entityPropertyType"]').last().should('have.text', 'string')

		cy.get('[data-cy="panel-questionnaireanswers"] [data-cy="entityPickList.editPropertyBtn"]').last().click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').should('be.visible')
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').clear().type('newName')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').last().should('have.text', 'newName')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').last().should('have.text', 'string')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.questionnaireanswers.properties).to.deep.equal([
					{
						"_propId": "abc123",
						"name": "id",
						"type": "string"
					},
					{
						"_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7",
						"name": "newName",
						"type": "string",
						"isStructured": false,
						"isArray": false
					}
				])
			})
	})

	it('can add a property after a nested property', () => {
		cy.route('GET', '/api/models/', [ { "name": "Test Model", "edges": {}, "nodes": { "poet": { "id": "poet", "x": -156.3861003861004, "y": -130.42857142857144, "label": "Poet", "entityName": "Poet", "type": "entity", "properties": [] }, "philosopher": { "id": "philosopher", "x": -156.3861003861004, "y": 100.42857142857144, "label": "Philosopher", "entityName": "Philosopher", "type": "entity", "properties": [] } } } ])
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([])
			})

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('arrayProp')
		cy.get('.v-dialog--active [data-cy="editProperty.dataType"]').click()
		cy.get('.v-list-item__title:visible').contains('array').parentsUntil('.v-list-item').click()
		cy.get('.v-dialog--active [data-cy="editProperty.arrayDataType"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.arrayDataType"]').type('{backspace}')
		cy.get('.v-dialog--active [data-cy="editProperty.arrayDataType"]').type('phi')
		cy.get('.v-list-item__title:visible').contains('Philosopher').parentsUntil('.v-list-item').click()

		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'arrayProp')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'Philosopher[]')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties[0].name).to.equal('arrayProp')
				expect(body.nodes.poet.properties[0].type).to.equal('Philosopher')
				expect(body.nodes.poet.properties[0].isArray).to.equal(true)
				expect(body.nodes.poet.properties[0].isStructured).to.equal(true)
			})

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('firstName')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('[data-cy="entityPickList.entityPropertyName"]').should('have.text', 'arrayPropfirstName')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').should('have.text', 'Philosopher[]string')
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties[1].name).to.equal('firstName')
				expect(body.nodes.poet.properties[1].type).to.equal('string')
				expect(body.nodes.poet.properties[1].isArray).to.equal(false)
				expect(body.nodes.poet.properties[1].isStructured).to.equal(false)
			})
	})

	it('can not edit a property to an existing one', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "string"},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "string" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.entityPropertyName"]').last().should('have.text', 'address')
		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.entityPropertyType"]').last().should('have.text', 'string')

		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.editPropertyBtn"]').last().click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').clear().type('id')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()
		cy.get('.v-messages__message').should('have.text', 'Property already exists')
	})

	it('can show advanced property features', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "string", "isPrimaryKey": true, "isPii": true},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "string" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()
		cy.get('[data-cy="panel-poet"] [data-cy="entityPickList.editPropertyBtn"]').first().click()
		cy.get('.v-dialog--active [data-cy="editProperty.advancedBtn"]').click()
		cy.get('.v-dialog--active [data-cy="prop.isPii"]').should('be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').should('be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isRequired"]').should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isElementRangeIndex"]').should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isRangeIndex"]').should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isWordLexicon"]').should('not.be.checked')
	})

	it('can only have one primary key', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "string", "isPrimaryKey": true, "isPii": true},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "string" }]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body).to.deep.equal({"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[{"_propId": "abc123", "name": "id", "type": "string", "isPrimaryKey": true, "isPii": true},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "string" }]}}})
			})

		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()
		// Starting point has 'id' as the primary key. 'address' is the last property in the display (assumed alpha
		// order). Clicking the pk button for the 'address' property should make it the pk and the pk attribute should
		// be removed from the 'id' prop

		cy.log("id (the last attribute in the list) should have pk selected")
		cy.get('[data-cy="entityPickList.editPropertyBtn"]').first().click()
		cy.get('.v-dialog--active [data-cy="editProperty.advancedBtn"]').click()
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').should('be.checked')
		cy.get('.v-dialog--active [data-cy="editProperty.cancelBtn"]').click()

		cy.log("make address the PK then check that the id attr does not have pk selected")
		cy.get('[data-cy="entityPickList.editPropertyBtn"]').last().click()
		cy.get('.v-dialog--active [data-cy="editProperty.advancedBtn"]').last().click()
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').last().should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').last().parentsUntil('.v-input__slot').first().click()
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').last().should('be.checked')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').last().click()
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([{"_propId": "abc123", "name": "id", "type": "string", "isPrimaryKey": false, "isPii": true},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "string", "isArray": false, "isPrimaryKey": true, "isStructured": false }])
			})

		// check id attr
		cy.get('[data-cy="entityPickList.editPropertyBtn"]').first().click()
		cy.get('.v-dialog--active [data-cy="editProperty.advancedBtn"]').first().click()
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').first().should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="editProperty.cancelBtn"]').first().click()

		cy.log("check we can unselect the PK, so nothing has a PK")
		cy.get('[data-cy="entityPickList.editPropertyBtn"]').last().click()
		cy.get('.v-dialog--active [data-cy="editProperty.advancedBtn"]').last().click()
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').last().should('be.checked')
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').last().parentsUntil('.v-input__slot').first().click()
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').last().should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').last().click()
		cy.wait('@saveModel')
			.its('request.body')
			.should(body => {
				expect(body.nodes.poet.properties).to.deep.equal([{"_propId": "abc123", "name": "id", "type": "string", "isPrimaryKey": false, "isPii": true},{ "_propId": "9c6144b2-4d75-4e6c-bd7e-7319b48039c7", "name": "address", "type": "string", "isArray": false, "isPrimaryKey": false, "isStructured": false }])
			})

		// check id attr
		cy.get('[data-cy="entityPickList.editPropertyBtn"]').first().click()
		cy.get('.v-dialog--active [data-cy="editProperty.advancedBtn"]').first().click()
		cy.get('.v-dialog--active [data-cy="prop.isPrimaryKey"]').first().should('not.be.checked')
		cy.get('.v-dialog--active [data-cy="editProperty.cancelBtn"]').first().click()
	})

	it('can not edit Info.iri with invalid stuff', () => {
		cy.route('GET', '/api/models/', [{"name":"Test Model","edges":{},"nodes":{"poet":{"id":"poet","x":-156.3861003861004,"y":-130.42857142857144,"label":"Poet","entityName":"Poet","type":"entity","properties":[]}}}])
		cy.visit('/')
		cy.url().should('include', '/model')

		cy.get('.v-messages__message').should('not.exist')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("poet").click()
		cy.get('.v-tab').contains('Info').click()
		cy.get('[data-cy="infoPane.baseUri"]').clear().type('http://blah')
		cy.get('.v-messages__message').should('contain', 'A valid IRI is required, e.g. http://marklogic.envision.com/')
		cy.get('[data-cy="infoPane.baseUri"]').clear().type('http://blah.com/')
		cy.get('.v-messages__message').should('not.exist')
	})

	it('can model nested entities', () => {
		cy.route('GET', '/api/models/', 'fixture:models.json')
		cy.visit('/')
		cy.url().should('include', '/model')
		cy.get('.hideUnlessTesting').invoke('css', 'visibility', 'visible')
		cy.get('[data-cy=nodeList]').contains("customer").click()

		cy.get('[data-cy="panel-customer"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('address1')
		cy.get('.v-dialog--active [data-cy="editProperty.dataType"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.dataType"]').type('{backspace}')
		cy.get('.v-dialog--active [data-cy="editProperty.dataType"]').type('addr')
		cy.get('.v-list-item__title:visible').contains('Address').parentsUntil('.v-list-item').click()
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()

		cy.get('.fa.fa-angle-right').should('be.visible')
		cy.get('[data-cy="entityPickList.entityPropertyName"]').contains('address1').should('be.visible')
		cy.get('[data-cy="entityPickList.entityPropertyType"]').contains('Address').should('be.visible')
		cy.get('[name="address1"]').should('have.css', 'max-height').and('eq', 'none')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('line1').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('line2').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('city').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('state').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('zip').should('exist')

		cy.get('[data-cy="entityPickList.entityPropertyType"]').contains('Address').click()
		cy.get('[name="address1"]').should('have.css', 'max-height').and('eq', 'none')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('line1').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('line2').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('city').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('state').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('zip').should('exist')

		cy.get('[data-cy="entityPickList.entityPropertyType"]').contains('Address').click()
		cy.get('[name="address1"]').should('have.css', 'max-height').and('eq', 'none')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('line1').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('line2').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('city').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('state').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('zip').should('exist')

		cy.get('[data-cy=nodeList]').contains("address").click()
		cy.get('[data-cy="panel-address"] [data-cy="entityPickList.addPropertyBtn"]').click()
		cy.get('.v-dialog--active [data-cy="editProperty.propName"]').type('stuff')
		cy.get('.v-dialog--active [data-cy="editProperty.createBtn"]').click()

		cy.get('[data-cy=nodeList]').contains("customer").click()
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('line1').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('line2').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('city').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('state').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('zip').should('exist')
		cy.get('[name="address1"] [data-cy="entityPickList.entityPropertyName"]').contains('stuff').should('exist')
	})
})
