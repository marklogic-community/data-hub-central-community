export default function draggable(source, target, positionOrOptions) {
	cy.wrap(source)
		.trigger('pointerdown', { which: 1, button: 0, pointerType: 'mouse'})
    .trigger('mousedown', { which: 1, button: 0 })
    .trigger('dragstart')

  cy.wrap(target)
    .trigger('dragover', positionOrOptions)
    .trigger('drop', positionOrOptions)
    .trigger('mouseup', { which: 1, button: 0 })
}


// drag and drop a source element to a target selector, anchoring the drop to the given
// position of the selector. positions are the same as the cypress trigger command
export function dropCommand($source, selector, position) {
  cy.get(selector).then($target => {
    const source = $source[0]
    const target = $target[0]
    draggable(source, target, position)
  })
}

// moves an item to a given index in the same list
export function moveCommand($source, listSelector, itemsSelector, targetIndex) {
  cy.get(listSelector).then($list => {
    const list = $list[0]
    const items = [].slice.call(list.querySelectorAll(itemsSelector))
    const item = $source[0]
    const currentIndex = items.findIndex(i => i === item)
    // if the target index is equal to the current index, do nothing
    if (targetIndex === currentIndex) return
    if (targetIndex === 0) {
      // if the target index is 0, move to the top of the drag area
      draggable(item, list, 'center')
    } else if (targetIndex === items.length - 1) {
      // if the target index is the last index, move to the bottom of the drag area
      draggable(item, list, 'bottom')
    } else if (targetIndex < currentIndex) {
      // if the target index is less than the current index, choose target - 1
      draggable(item, items[targetIndex - 1])
    } else if (targetIndex > currentIndex) {
      // if the target index is more than the current index, choose target + 1
      draggable(item, items[targetIndex + 1])
    }
  })
}
