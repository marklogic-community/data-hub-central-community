
Cypress.Commands.add('getMenuOption', (selector) => {
	return cy.contains('.v-menu__content.menuable__content__active:visible .v-list-item:visible', selector)
})

Cypress.Commands.add('getActiveDialog', () => {
	cy.get('.v-dialog--active').invoke('css', 'opacity', '1')
	cy.get('.v-dialog--active:visible').should('be.visible')
	return cy.get('.v-dialog--active:visible')
})
