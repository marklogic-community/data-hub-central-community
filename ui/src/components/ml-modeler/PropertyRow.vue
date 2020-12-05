<template>
	<div data-cy="entityPickList.propertyRow" :name="prop.name" v-if="prop.isStructured">
		<div class="prop-grid grid-row" :class="expanded ? 'expanded': ''" @click="expandProperty">
			<div class="id-col" data-cy="entityPickList.entityPropertyId">
				<i class="fa fa-angle-right" :class="expanded ? 'expanded' : ''"></i>
			</div>
			<div data-cy="entityPickList.entityPropertyName">{{prop.name}}</div>
			<div data-cy="entityPickList.entityPropertyType">{{prop.type}}{{prop.isArray ? '[]' : ''}}</div>
			<div class="action" v-if="!readOnly">
				<v-tooltip bottom>
					<template v-slot:activator="{ on }">
						<v-btn
							data-cy="entityPickList.editPropertyBtn"
							right
							icon
							small
							@click.stop="editProperty"
							v-on="on"
						>
							<v-icon>edit</v-icon>
						</v-btn>
					</template>
					<span>Edit {{prop.name}}</span>
				</v-tooltip>
				<delete-data-confirm
					data-cy="entityPickList.deletePropertyBtn"
					tooltip="Delete property"
					:message="`Do you really want to delete ${prop.name}?`"
					:collection="prop.name"
					@deleted="deleteProperty"/>
			</div>
		</div>
		<div class="structured" :class="expanded ? 'expanded': ''">
			<div>
				<property-row
					v-for="p in entities[prop.type.toLowerCase()].properties"
					:key="p.name"
					:entity="entity"
					:prop="p"
					:entities="entities"
					:readOnly="true"
					/>
			</div>
		</div>
	</div>
	<div data-cy="entityPickList.propertyRow" :name="prop.name" v-else>
		<div class="prop-grid grid-row">
			<div class="id-col" data-cy="entityPickList.entityPropertyId">
				<span v-if="entity.idField === prop.name" title="ID field">id</span>
				<span v-if="prop.isPii" title="PII">pii</span>
				<span v-if="prop.isPrimaryKey" title="Primary Key">key</span>
				<span v-if="prop.isRequired" title="Required">req</span>
				<span v-if="prop.isElementRangeIndex || prop.isWordLexicon || prop.isRangeIndex" title="Advanced options">...</span>
			</div>
			<div data-cy="entityPickList.entityPropertyName">{{prop.name}}</div>
			<div data-cy="entityPickList.entityPropertyType">{{prop.type}}{{prop.isArray ? '[]' : ''}}</div>
			<div class="action" v-if="!readOnly">
				<v-tooltip bottom>
					<template v-slot:activator="{ on }">
						<v-btn
							data-cy="entityPickList.editPropertyBtn"
							right
							icon
							small
							@click.stop="editProperty"
							v-on="on"
						>
							<v-icon>edit</v-icon>
						</v-btn>
					</template>
					<span>Edit {{prop.name}}</span>
				</v-tooltip>
				<delete-data-confirm
					data-cy="entityPickList.deletePropertyBtn"
					:small="true"
					tooltip="Delete property"
					:message="`Do you really want to delete ${prop.name}?`"
					:collection="prop.name"
					@deleted="deleteProperty"/>
			</div>
		</div>
	</div>
</template>

<script>
import DeleteDataConfirm from '@/components/DeleteDataConfirm'

export default {
	name: "PropertyRow",
	components: {
		DeleteDataConfirm
	},
	props: {
		entity: { type: Object },
		prop: { type: Object },
		entities: { type: Object },
		readOnly: { type: Boolean, default: false }
	},
	data() {
		return {
			expanded: false,
		}
	},
	methods: {
		expandProperty() {
			if (this.prop.isStructured) {
				this.expanded = !this.expanded
			}
		},
		editProperty() {
			this.$emit('editProperty', this.prop)
		},
		deleteProperty() {
			this.$emit('deleteProperty', {entity: this.entity, propName: this.prop.name})
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

.structured {
	border: none;
	max-height: 0;
	overflow: hidden;
	transition: 0.25s ease;
	border: 1px solid transparent;

	&.expanded {
		padding: 2px;
		border: 1px solid rgb(68, 73, 156);
		max-height: inherit;
		transition: 0.25s ease;
	}
}

.fa-angle-right {
	transition: 0.25s ease;
	margin-left: 0.5rem;
	&.expanded {
		transform: rotate(90deg);
	}
}

.grid-row div {
	padding: 5px;
}

/deep/ .v-btn--icon {
	width: 20px;
	height: 20px;

	i.v-icon {
		width: 18px;
		height: 18px;
		font-size: 18px;
	}
}
</style>
