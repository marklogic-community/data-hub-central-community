<template>
	<v-card>
		<v-card-text>
			<div v-for="(result, $index) in coloredResults" :key="$index" :class="['result', (currentNode === result) ? 'active': '']" @click="selectResult(result)">
				<h3>{{ resultLabel(result) }}</h3>
				<v-chip v-if="result.entityName" :color="result.bgColor" :style="{border: '2px dashed', borderColor: result.borderColor}">{{result.entityName}}</v-chip>
				<div class="matches">
					<div class="match" v-for="(match, $index) in result.matches" :key="$index">
						<em v-for="(text, $index) in match['match-text']" :key="$index">
							<span :class="text.highlight !== undefined ? 'highlight' : ''">{{
								text.highlight !== undefined ? text.highlight : text
							}}</span>
						</em>
					</div>
				</div>
			</div>
		</v-card-text>
	</v-card>
</template>

<script>
export default {
  name: 'ml-results',
  props: {
		currentNode: { type: Object },
		colors: { type: Object },
    results: {
      type: Array,
      default: () => {
        return [];
      }
    }
	},
	computed: {
		coloredResults() {
			return this.results.map(r => {
				if (r.entityName) {
					const e = r.entityName.toLowerCase()
					const color = this.colors[e] || {}
					return {
						...r,
						borderColor: color.border,
						bgColor: color.background
					}
				}
				else {
					return r
				}
			})
		}
	},
  methods: {
    resultLabel(result) {
      return result.label || result.uri.split('/').pop();
		},
		selectResult(result) {
			this.$emit('select', result)
		}
  }
};
</script>
<style lang="less" scoped>
.result {
	padding: 1em 0.5em;
	cursor: pointer;
	border: 1px solid transparent;
	border-top: none;
	border-bottom: 1px solid #ccc;

	&:first-child {
		border-top: 1px solid transparent;
		&:hover {
			border-top: 1px solid #ccc;
		}
	}

	&:last-child {
		border-bottom: none;
	}

	&:hover {
		background-color: #f8f8f8;
		border: 1px solid #ccc;
		border-top: none;
	}

	&.active {
		border: 1px solid #ffdddd;
		background-color: #ffefef;
		margin-top: -1px;
	}

}

h3 {
	display: inline-block;
}

.highlight {
	background-color: yellow;
}
.v-chip {
	float: right;
}

.v-card {
	overflow-y: scroll;
}
</style>
