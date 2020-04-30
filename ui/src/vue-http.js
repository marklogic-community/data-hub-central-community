import Axios from 'axios';

function plugin(Vue) {
  if (plugin.installed) {
    return;
  }

  plugin.installed = true;

	const accessToken = localStorage.getItem('access_token');

	if (accessToken) {
		Axios.defaults.headers.common['Authorization'] =  accessToken;
	}

  Object.defineProperties(Vue.prototype, {
    $http: {
      get() {
				return Axios;
      }
    }
  });
}

export default plugin;
