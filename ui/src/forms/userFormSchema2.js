import VueFormGenerator from 'vue-form-generator'

export default {
  groups: [
    {
      legend: 'Source Connection',
      fields: [
        {
          type: 'input',
          inputType: 'text',
          label: 'Connection',
          model: 'srcConnection',
          required: true,
          validator: ['string', 'required']
        },
				{
          type: 'input',
          inputType: 'text',
          label: 'Username',
          model: 'srcUser',
          required: true,
          validator: ['string', 'required']
        },
        {
          type: 'input',
          inputType: 'password',
          label: 'Password',
          model: 'srcPassword',
          required: true,
          validator: ['string', 'required']
        }
      ]
    }
	]
}
