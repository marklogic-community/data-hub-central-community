<template>
  <v-menu v-model="menuOpen" :class="`menu.${name}`" :offset-x='isOffsetX' :offset-y='isOffsetY' :open-on-hover='isOpenOnHover' :transition='transition'>
    <template v-slot:activator="{ on: menu }">
      <v-list-item v-if='isSubMenu' class='d-flex justify-space-between' :data-cy="`menuBtn.${name}`" v-on="{ ...menu }">
        {{ name }}<v-icon>navigate_next</v-icon>
      </v-list-item>
			<template v-else>
				<div class="btn-wrapper">
					<label class="menu-label v-label v-label--active theme--light">Sort</label>
					<v-tooltip top>
						<template v-slot:activator="{ on: tooltip }">
							<v-btn :data-cy="`menuBtn.${name}`" :color='color' v-on="{ ...tooltip, ...menu }" text tile>{{ name | truncate(25, '') }}</v-btn>
						</template>
						<span>{{name}}</span>
					</v-tooltip>
				</div>
			</template>
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

<style lang="less" scoped>
.menu-label {
	left: 15px;
	right: auto;
	position: absolute;
	top: -8px;
	font-size: 14px;
}

.btn-wrapper {
	position: relative;
}
</style>
