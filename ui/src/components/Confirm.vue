<template>
    <v-card>
        <v-card-text v-show="!!message" class="pa-4">{{ message }}</v-card-text>
        <v-card-actions class="pt-0">
        <v-spacer></v-spacer>
        <v-btn color="secondary" text @click.native="cancel">Cancel</v-btn>
		<v-btn :disabled="disabled" color="primary" text @click.native="confirm">{{ confirmText || 'Confirm' }}</v-btn>
        </v-card-actions>
    </v-card>
</template>

<script>
export default {
    props: {
		disabled: { type: Boolean, default: false },
        message: {type: String},
        confirmText: {type: String}
	},
    methods: {
        confirm() {
            this.$emit('confirm')
        },
        cancel() {
            this.$emit('cancel')
        }
    }
}
</script>