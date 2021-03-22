import Vue from 'vue'
import Vuetify from 'vuetify/lib'
import 'vuetify/src/styles/main.sass'

Vue.use(Vuetify)

// colours taken from https://branding.marklogic.com/d/Gr3vJKwiERnN/marklogic-style-guide#/basics/colors
export default new Vuetify({
  theme: {
    themes: {
      light: {
        primary: '#394494',
        secondary: '#424242',
        accent: '#6068b2',
        error: '#B32424',
        info: '#5B69AF',
        success: '#389E0D',
        warning: '#CE8406'
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
