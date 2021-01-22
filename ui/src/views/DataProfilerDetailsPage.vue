<template>
	<v-container fluid class="DataProfilerDetailsPage">
		<v-layout row>
			<v-flex md12>
				<router-link :to="{name: 'root.profiles'}">&lt; All Reports</router-link>
			</v-flex>
		</v-layout>
		<v-layout row class="fullHeight">
			<v-flex md12>
				<template v-if="report">
					<v-layout row>
						<v-flex md3>
							<v-card>
								<v-card-text>
									<highcharts class="hc" :options="dataFormatOptions" ref="chart"></highcharts>
								</v-card-text>
							</v-card>
						</v-flex>
						<v-flex md9>
							<v-layout row>
								<v-flex md12>
									<v-card>
										<v-card-title>Data Summary</v-card-title>
										<v-card-text>
											<v-layout row class="info-wrapper">
												<v-flex md6>
													<div class="info-label">Data Source</div>
													<div class="info-value">{{report.name}}</div>
												</v-flex>
												<v-flex md6>
													<div class="info-label">Documents</div>
													<div class="info-value">200</div>
												</v-flex>
											</v-layout>
										</v-card-text>
									</v-card>
								</v-flex>
							</v-layout>
						</v-flex>
					</v-layout>

					<v-layout row>
						<v-flex md12>
							<v-data-table
								:headers="profileHeaders"
								:items="profileItems"
								:items-per-page="10"
								class="elevation-1"
							>
								<template v-slot:body="{ items, headers }">
									<tbody>
										<tr
											v-for="item in items"
											:key="item.name"
										>
											<td v-for="h in headers" :key="h.name">
												<span v-html="item[h.value]"></span>
											</td>
										</tr>
									</tbody>
								</template>
							</v-data-table>
						</v-flex>
					</v-layout>
				</template>
			</v-flex>
		</v-layout>
	</v-container>
</template>

<script>
import profilerApi from '@/api/ProfilerApi'

import Highcharts from 'highcharts'
import exportingInit from 'highcharts/modules/exporting'

exportingInit(Highcharts)

export default {
	components: {
	},
	computed: {
		uri() {
			return decodeURIComponent(this.$route.query.uri)
		},
		dataFormats() {
			if (this.report) {
				return Object.keys(this.report.docTypes).map(key => ({
					type: key,
					percent: (this.report.docTypes[key] / this.report.total) * 100
				}))
			}
			return []
		},
		profileHeaders() {
			return [
				{ text: 'Property', value: 'name' },
				{ text: 'Type', value: 'valueType' },
				{ text: 'Unique Values', value: 'uniqueValues' },
				{ text: 'Null Values', value: 'nullValues' },
				{ text: 'Missing Values', value: 'missingValues' },
				{ text: 'Total Values', value: 'totalValues' },
			]
		},
		profileItems() {
			if (this.report) {
				return Object.keys(this.report.properties).map(key => {
					const property = this.report.properties[key]
					const profile = this.report.profileProperties[key]
					let valueType = property.type
					if (property.valueTypes) {
						const types = Object.keys(property.valueTypes)
						if (types.length === 1) {
							valueType = types[0]
						}
						else {
							valueType = Object.keys(property.valueTypes).map(k => ({
								name: k,
								count: property.valueTypes[k]
							}))
								.sort((a,b) => a.count < b.count)
								.map(x => `${x.name} ${(x.count/this.report.total) * 100}%`)
								.join('<br/>')
						}
					}
					return {
						name: key,//.replace(/[^/]+\//g, '<span class="offset">&nbsp;</span>'),
						...property,
						valueType: valueType,//property.valueTypes || property.type,
						uniqueValues: property.values ? Object.keys(property.values).length : '',
						nullValues: profile.nullCount,//property.values ? property.values.null || 0 : '',
						missingValues: profile.documentMissingCount,//[],
						totalValues: this.report.total
					}
				})
			}
			return []
		},
		dataFormatOptions() {
			return {
				chart: {
					type: 'pie'
				},
				title: {
					text: 'Data Formats'
				},
				plotOptions: {
					pie: {
						allowPointSelect: true,
						cursor: 'pointer',
						dataLabels: {
							enabled: true,
							format: '<b>{point.name}</b>: {point.percentage:.1f} %'
						}
					}
				},
				tooltip: {
					headerFormat: '<span style="font-size:10px">{point.key}</span><table>',
					pointFormat: '<td style="padding:0"><b>{point.percentage:.1f}%</b></td></tr>',
					footerFormat: '</table>',
					shared: true,
					useHTML: true
				},
				series: [{
					data: Object.keys(this.report.docTypes).map(key => ({
						name: key,
						y: (this.report.docTypes[key] / this.report.total) * 100
					}))
				}]
			}
		},
		completenessOptions() {
			return {
				chart: {
					type: 'column',
					scrollablePlotArea: {
						minWidth: 8000
					}
				},
				title: {
					text: 'Data Completeness'
				},
				xAxis: {
					type: 'category',
					min: 0,
				},
				yAxis: {
					min: 0,
					max: 100,
					title: {
						text: 'Percent Complete'
					}
				},
				tooltip: {
					headerFormat: '<span style="font-size:10px">{point.key}</span><table>',
					pointFormat: '<td style="padding:0"><b>{point.tt}</b></td></tr>',
					footerFormat: '</table>',
					shared: true,
					useHTML: true
				},
				plotOptions: {
					column: {
						pointPadding: 0.2,
						pointWidth: 40,
						borderWidth: 0
					},
				},
				series: [{
					name: 'Properties',
					data: Object.keys(this.report.properties).map(key => {
						const field = this.report.properties[key]
						let percent = Math.min(100, Object.values(field.values || {}).reduce((total, current) => {
							return total + current
						}, 0) / this.report.total * 100)

						if (percent == 0) {
							return {
								name: key,
								y: 100,
								tt: 'Missing',
								color: '#fafafa',
								borderWidth: 1,
								borderColor: '#ddd'
							}
						}
						return {name: key, y: percent, tt: `${percent}%`}
					})
				}]
			}
		}
	},
	data() {
		return {
			report: null
		}
	},
	methods: {
		getReport() {
			profilerApi.getReport(this.uri).then(resp => {
				this.report = resp
			})
		}
	},
	mounted() {
		this.getReport()
	},
	watch: {
		'$route.query': {
			handler(params) {
				if (params) {
					this.getReport()
				}
			},
			deep: true
		}
	}
}
</script>

<style lang="less" scoped>
.fullHeight {
	height: 100%;
	position: relative;
}

.DataProfilerDetailsPage {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 0px;

	.flex {
		padding: 12px 20px;
	}
}

/deep/ span.offset {
	width: 10px;
	display: inline-block;
}

.info-wrapper {
	.flex:not(:last-child) {
		border-right: 1px solid #eee;
	}

	.info-label {
		font-size: 16px;
	}

	.info-value {
		margin-top: 5px;
		font-size: 30px;
		font-weight: bold;
	}
}
</style>
