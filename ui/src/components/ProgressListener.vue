<template>
	<div>
		<v-snackbar v-if="!hidden" :value="progresses.length > 0" multi-line absolute bottom right light :timeout="-1">
			<div class="minimize-icon" @click="hidden = !hidden"><v-icon>keyboard_arrow_down</v-icon></div>
			<transition-group appear tag="div" class="status" v-for="(p, idx) in progresses" :key="idx">
				<div :key="p.key">
					<div class="lbl">{{p.message}}
						<v-icon v-if="p.error" class="errored" small>error</v-icon>
						<v-icon v-else-if="p.percentComplete >= 100" class="finished" small>done</v-icon>
					</div>
					<div v-if="p.error" class="err">{{p.error}}</div>
					<v-progress-linear v-else :color="p.error ? 'red': 'primary'" :indeterminate="p.percentComplete == null" :value="p.percentComplete"></v-progress-linear>
				</div>
			</transition-group>
		</v-snackbar>
		<div v-else-if="taskCount > 0" class="minimized" @click="hidden = !hidden">
			<v-progress-linear color="white" :value="totalPc"></v-progress-linear>
			<div class="min-text">{{taskCount}} Processes running...</div>
		</div>
	</div>
</template>

<script>

export default {
	data() {
		return {
			hidden: true,
			allProgresses: {}
		}
	},
	computed: {
		taskCount() {
			return this.progresses.length
		},
		totalPc() {
			const ps = this.progresses.filter(p => p.percentComplete != null)
			return ps.reduce((total, current) => {
				return total + current.percentComplete
			}, 0) / ps.length
		},
		progresses() {
			return Object.keys(this.allProgresses).map(key => {
				const status = this.allProgresses[key]
				const finished = !status.error && status.percentComplete >= 100
				if (finished) {
					setTimeout(() => {
						this.$delete(this.allProgresses, key)
					}, 2000)
				}
				else if (status.error) {
					setTimeout(() => {
						this.$delete(this.allProgresses, key)
					}, 4000)
				}
				return status
			})
		}
	},
	mounted() {
		this.$ws.subscribe('/topic/status', tick => {
			const msg = tick.body
			if (!this.allProgresses[msg.key]) {
				this.hidden = false
			}
			this.$set(this.allProgresses, msg.key, msg)
		})
	},
};
</script>
<style lang="less" scoped>
.status:not(:last-child) {
	margin-bottom: 1em;
}
.lbl {
	margin-bottom: 0.5em;
	font-weight: bold;

	.v-icon {
		margin-left: 0.5em;

		&.finished {
			color: green;
		}
		&.errored {
			color: red;
		}
	}
}

.minimize-icon {
	float: right;
	cursor: pointer;
}
.err {
	color: red;
	font-style: italic;
}
.minimized {
	cursor: pointer;
	color: white;
	position: absolute;
	bottom: 15px;
	right: 15px;

	display: flex;
	flex: 1;
	align-items: center;

	.v-progress-linear {
		width: 75px;
	}

	.min-text {
		flex: 1 1 auto;
		margin-left: 1rem;
	}
}
</style>
