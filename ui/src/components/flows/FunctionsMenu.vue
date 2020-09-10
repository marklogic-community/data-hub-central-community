<template>
	<v-dialog
		v-model="open"
		width="700"
	>
		<template v-slot:activator="{ on }">
			<v-btn
				right
				icon
				data-cy="function.button"
				v-on="on">
				<v-icon>functions</v-icon>
			</v-btn>
		</template>

		<v-card>
			<v-card-title>Functions</v-card-title>
			<v-card-text>
				<v-row class="fullheight">
					<v-flex md6 class="listwrapper">
						<v-text-field
							autofocus
							data-cy="functions.filter"
							class="funcFilter"
							v-model="funcSearch"
							label="Search Functions"
							hide-details
							solo
							clearable
							@click:clear="funcSearch = ''"
							clear-icon="mdi-close-circle-outline"
						></v-text-field>
						<v-list class="functions" elevation="1">
							<v-list-item-group v-model="selectedFuncIndex">
								<div v-for="category in categories" :key="category">
									<v-subheader>{{category}}</v-subheader>
									<v-list-item v-for="(func, index) in functionsByCategory(category)" :key="index" @click="selectedFunc = func">
										<v-list-item-title>{{func.name}}</v-list-item-title>
									</v-list-item>
								</div>
							</v-list-item-group>
						</v-list>
					</v-flex>
					<v-flex md6 class="sigwrapper">
						<v-container v-if="selectedFunc">
							<h2><v-icon>functions</v-icon>{{selectedFunc.name}}</h2>
							<p class="funcsig">{{selectedFunc.signature}}</p>
							<p v-if="selectedFunc.category === 'xpath'">
								<v-icon>menu_book</v-icon> <a data-cy="functions.docsLink" target="new" :href="`https://docs.marklogic.com/fn:${selectedFunc.name}`">Online Documentation</a>
							</p>
						</v-container>
						<div class="text-center">
							<v-btn
								v-if="selectedFunc"
								data-cy="functions.insertBtn"
								@click="insertFunction">Insert Function</v-btn>
						</div>
					</v-flex>
				</v-row>
			</v-card-text>
		</v-card>
	</v-dialog>
</template>

<script>
import _ from 'lodash'

export default {
	props: {
		functions: { type: Array }
	},
	data() {
		return {
			open: null,
			selectedFuncIndex: null,
			selectedFunc: null,
			funcSearch: ''
		}
	},
	computed: {
		categories() {
			return _.uniq(this.functions.map(f => f.category))
		}
	},
	methods: {
		functionsByCategory(category) {
			return this.functions
				.filter(f => f.category === category)
				.filter(f => f.name.toLowerCase().includes((this.funcSearch || '').toLowerCase()))
				.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
		},
		onSelected(func) {
			this.selectedFunc = func
		},
		insertFunction() {
			this.$emit('selected', this.selectedFunc)
			this.open = false
		}
	}
}
</script>

<style lang="less" scoped>
/deep/ .v-dialog {
	height: 90%;

	.v-card {
		display: flex;
		height: 100%;
		flex-direction: column;
		flex: 0 0 auto;
	}
	.v-card__text {
		overflow-y: auto;
		height: 100%;
	}

	.v-subheader {
		text-transform: uppercase;
	}

	.v-list.functions {
		height: 100%;
		overflow-y: auto;
	}

	.listwrapper {
		display: flex;
		flex-direction: column;
		flex: 1 0 auto;
		height: 100%;
	}

	.sigwrapper {
		flex: 1 1 auto;
    display: flex;
		flex-direction: column;

		.container {
			flex: 1 1 auto;
		}
	}
	.funcFilter {
		margin-bottom: 10px;
	}

	.fullheight {
		height: 100%;
	}

	.funcsig {
		margin-top: 10px;
	}
}
</style>
