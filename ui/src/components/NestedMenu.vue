<template>
  <v-menu v-model="menuOpen" :class="`menu.${name}`" :offset-x='isOffsetX' :offset-y='isOffsetY' :open-on-hover='isOpenOnHover' :transition='transition'>
    <template v-slot:activator="{ on }">
      <v-list-item v-if='isSubMenu' class='d-flex justify-space-between' :data-cy="`menuBtn.${name}`" v-on="on">
        {{ name }}<v-icon>navigate_next</v-icon>
      </v-list-item>
      <v-btn :data-cy="`menuBtn.${name}`" v-else :color='color' v-on="on" text tile>{{ name }}</v-btn>
    </template>
    <v-list>
      <template v-for="(item, index) in menuItems">
        <v-divider v-if='item.isDivider' :key='index' />
        <nested-menu v-else-if='item.enabled && item.items' :key='index' :name='item.label' :menu-items='item.items' @selected='emitClickEvent'
          :is-open-on-hover=false :is-offset-x=true :is-offset-y=false :is-sub-menu=true
        />
				<template v-else>
					<v-list-item v-if="item.enabled" :key='index' @click.stop='emitClickEvent(item)'>
						<v-list-item-title>{{ item.label }}</v-list-item-title>
					</v-list-item>
					<v-tooltip v-else :key='index' bottom>
						<template v-slot:activator="{ on }">
							<v-list-item :data-cy="`item.${item.label}`" @click.stop='emitClickEvent(item)' v-on="on">
								<v-list-item-title>{{ item.label }}</v-list-item-title>
								<v-icon color='red'>priority_high</v-icon>
							</v-list-item>
						</template>
						<span>This index is not enabled in MarkLogic Server</span>
					</v-tooltip>
				</template>
      </template>
    </v-list>
  </v-menu>
</template>

<script>
import NestedMenu from '@/components/NestedMenu'

export default {
	name: 'NestedMenu',
	components: {
		NestedMenu
	},
	data: () => {
		return {
			menuOpen: false
		}
	},
  props: {
    name: String,
    icon: String,
    menuItems: Array,
    color: { type: String, default: 'secondary' },
    isOffsetX: { type: Boolean, default: false },
    isOffsetY: { type: Boolean, default: true },
    isOpenOnHover: { type: Boolean, default: false },
    isSubMenu: { type: Boolean, default: false },
    transition: { type: String, default: 'scale-transition' }
  },
  methods: {
    emitClickEvent (item) {
			if (item.enabled) {
				this.menuOpen = false
				this.$emit('selected', item)
			}
		}
  }
}
</script>
