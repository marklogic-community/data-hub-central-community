<template>
	<div>
		<template v-if="localProperties.length > 0">
			<div class="prop-grid">
				<div class="grid-head id-col"></div>
				<div class="grid-head primary--text">Name</div>
				<div class="grid-head primary--text">Type</div>
				<div class="grid-head primary--text">Action</div>
			</div>
			<draggable v-bind="dragOptions" v-model="localProperties" tag="div" class="properties">
				<property-row
					v-for="prop in localProperties"
					:key="prop.name"
					:prop="prop"
					:entity="entity"
					:entities="entities"
					@editProperty="showEdit"
					@deleteProperty="deleteProperty"/>
			</draggable>
		</template>
		<p class="grey--text darken-4" v-else>No properties</p>
		<v-btn
			color="primary"
			fab
			dark
			small
			data-cy="entityPickList.addPropertyBtn"
			@click="showEdit(null)"
		>
			<v-icon dark>add</v-icon>
		</v-btn>
		<edit-property-dialog ref="editPropertyDlg"></edit-property-dialog>
	</div>

</template>

<script>
import draggable from 'vuedraggable'
import EditPropertyDialog from '@/components/EditPropertyDialog.vue'
import { mapState } from 'vuex'
import PropertyRow from './PropertyRow.vue'

export default {
	name: 'EditProperties',
	components: {
		draggable,
		EditPropertyDialog,
		PropertyRow
	},
	props: {
		properties: { type: Array },
		entity: { type: Object }
	},
	computed: {
		...mapState({
			model: state => state.model.model
		}),
		entities() {
			return this.model.nodes
		},
		localProperties: {
			get() {
				return JSON.parse(JSON.stringify(this.properties))
			},
			set(props) {
				this.propertiesUpdated(props)
			}
		}
	},
	data() {
		return {
			dragOptions: {
				animation: 200,
			}
		}
	},
	methods: {
		showEdit(oldProp) {
			this.$refs.editPropertyDlg.open({
				prop: oldProp ? Object.assign({}, oldProp) : null,
				entityName: this.entity.entityName,
				existingProperties: this.properties
			})
			.then((newProp) => {
				this.$emit('save', {properties: this.localProperties, oldProp, newProp})
			})
			.catch(() => {})
		},
		deleteProperty({entity, propName}){
			this.$emit ("deleteProperties", {entity, propName})
		},
		propertiesUpdated(props) {
			this.$emit('updated', props)
		}
	}
}
</script>

<style lang="less" scoped>
.id-col {
	padding: 0px;
	span {
		font-weight: bold;
		font-size: 8px;
		border: 1px solid darkblue;
    padding: 4px;
    border-radius: 10px;
    background-color: lightblue;
		margin-right: 2px;
	}
}
.small-btn {
	width: 30px;
	height: 30px;
}
.fa-pencil {
	margin-right: 10px;
}

.v-data-table > .v-data-table__wrapper > table > tbody > tr:not(:last-child) > td.prop-wrapper:last-child {
	border: 1px solid red;
}
.v-data-table > .v-data-table__wrapper > table > tbody > tr > td.prop-wrapper {
	padding: 0px;
	border: 1px solid red;
}

.sub-props {
	background-color: #ddd;
}

/deep/ .prop-grid {
	display: grid;
	grid-template-columns: 0.5fr 1fr 1fr 0.5fr;
	grid-template-rows: auto;
	grid-column-gap: 0px;
	grid-row-gap: 0px;
	color: black;

	&.grid-row {
		&:hover {
			background-color: #eee;
		}

		&.expanded {
			border: 1px solid rgb(68, 73, 156);
			background-color: rgb(68, 73, 156);
			color: white;
			border-bottom: 0px;

			i {
				color: white;
			}
		}
	}
}
</style>
