import Vue from 'vue'
import Vuetify from 'vuetify/lib'
import 'vuetify/src/styles/main.sass'

Vue.use(Vuetify)

// colours taken from https://branding.marklogic.com/d/Gr3vJKwiERnN/marklogic-style-guide#/basics/colors
export default new Vuetify({
  theme: {
    themes: {
      light: {
        primary: '#44499c',
        secondary: '#424242',
        accent: '#6068b2',
        error: '#cb333b',
        info: '#47ffde',
        success: '#3cdbc0',
        warning: '#f4364c'
      }
    }
  },
  iconfont: 'md',
})
/* pre TOPG137
		dark: false,
    primary: '#ee44aa',
    secondary: '#424242',
    accent: '#82B1FF',
    error: '#FF5252',
    info: '#2196F3',
    success: '#4CAF50',
    warning: '#FFC107'
    */