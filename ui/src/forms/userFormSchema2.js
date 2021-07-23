import VueFormGenerator from 'vue-form-generator'

export default {
  groups: [
    {
			styleClasses: "group-one-class",
      legend: 'Source Connection',
      fields: [
        {
          type: 'input',
          inputType: 'text',
          label: 'Connection',
          model: 'connectionString',
          required: false,
          validator: ['string']
        },
				{
          type: 'input',
          inputType: 'text',
          label: 'Username',
          model: 'username',
          required: false,
          validator: ['string']
        },
        {
          type: 'input',
          inputType: 'password',
          label: 'Password',
          model: 'password',
          required: false,
          validator: ['string']
        },
        {
          type: 'input',
          inputType: 'number',
          label: 'Threads',
          model: 'numThreads',
          required: false,
          validator: ['number']
        },
        {
          type: 'input',
          inputType: 'number',
          label: 'Batch Size',
          model: 'batchSize',
          required: false,
          validator: ['number']
        }
      ]
    }
	]
}
