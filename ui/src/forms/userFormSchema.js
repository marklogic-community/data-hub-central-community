import VueFormGenerator from 'vue-form-generator'

export default {
  groups: [
    {
      legend: 'Personal Info',
      fields: [
        {
          type: 'input',
          inputType: 'text',
          label: 'First Name',
          model: 'first_name',
          required: true,
          validator: ['string', 'required']
        },
        {
          type: 'input',
          inputType: 'text',
          label: 'Last Name',
          model: 'last_name',
          required: true,
          validator: ['string', 'required']
        },
        {
          type: 'select',
          label: 'Gender',
          model: 'gender',
          values: [
            {id: 'male', name: 'Male'},
            {id: 'female', name: 'Female'}
          ],
          selectOptions: {
            noneSelectedText: 'Choose One'
          },
          required: true,
          validator: ['string', 'required']
        },
        {
          type: 'input',
          inputType: 'number',
          label: 'Age',
          model: 'age',
          required: true,
          hint: 'Age is required & must be a between 18 and 35.',
          validator: ['number', 'required'],
          min: 18,
          max: 35
        },
        {
          type: 'input',
          inputType: 'text',
          label: 'City',
          model: 'city',
          required: true,
          validator: ['string', 'required']
        }
      ]
    },
    {
      legend: 'Contact Details',
      fields: [
        {
          type: 'input',
          inputType: 'email',
          label: 'Email',
          model: 'email',
          required: true,
          validator: VueFormGenerator.validators.email
        },
        {
          type: 'tel-input',
          label: 'Phone Number',
          model: 'phone_number'
        }
      ]
    },
    {
      legend: 'Profile',
      fields: [
        {
          type: 'textArea',
          inputType: 'textArea',
          rows: 4,
          label: 'About',
          model: 'about',
          required: true,
          validator: ['string', 'required']
        },
        {
          type: 'checklist',
          label: 'Skills',
          model: 'skills',
          values: ['Javascript', 'VueJS', 'CSS3', 'HTML5'],
          multiSelect: true,
          multi: true,
          required: true,
          validator: ['array', 'required'],
          selectOptions: {
            noneSelectedText: 'Choose One'
          }
        }
      ]
    },
    {
      legend: 'Login Details',
      fields: [
        {
          type: 'input',
          inputType: 'text',
          label: 'Username',
          model: 'username',
          required: true,
          validator: ['string', 'required']
        },
        {
          type: 'input',
          inputType: 'password',
          label: 'Password',
          model: 'password',
          required: true,
          validator: ['strongPassword', 'required']
        }
      ]
    }
  ]
}
