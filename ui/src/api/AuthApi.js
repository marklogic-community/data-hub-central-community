import axios from 'axios';

export default {
  name: 'AuthApi',

  signup(email, name, password) {
    return axios.post('/api/auth/signup', {
			email, name, password
		})
		.then(response => {
        if (response.status === 200) {
					axios.defaults.headers.common['Authorization'] = response.headers.authorization;
					localStorage.setItem('access_token', response.headers.authorization);
          return response.data;
        } else {
          return { isError: true, error: response.data };
        }
			}
		)
		.catch(error => {
			console.error('error:', error);
			return { isError: true, error: error };
		});
	},

	userExists(email) {
		return axios.get(`/api/auth/userExists?email=${encodeURIComponent(email)}`)
			.then(response => {
				if (response.status === 200) {
					return response.data;
				} else {
					return { isError: true, error: response.data };
				}
			})
			.catch(error => {
				return { isError: true, error: error };
			})
	},

	resetPassword(email) {
		return axios.get(`/api/auth/resetPassword?email=${encodeURIComponent(email)}`)
			.then(response => {
				if (response.status === 200) {
					return response.data;
				} else {
					return { isError: true, error: response.data };
				}
			})
			.catch(error => {
				return { isError: true, error: error };
			})
	},

	validateResetToken(token) {
		return axios.get(`/api/auth/validateResetToken?token=${encodeURIComponent(token)}`)
			.then(response => {
				if (response.status === 200) {
					return response.data;
				} else {
					return { isError: true, error: response.data };
				}
			})
			.catch(error => {
				return { isError: true, error: error };
			})
	},
	setPassword(token, password) {
		return axios.post(`/api/auth/updatePassword`, {
			token, password
		})
		.then(response => {
			if (response.status === 200) {
				return response.data;
			} else {
				return { isError: true, error: response.data };
			}
		})
		.catch(error => {
			return { isError: true, error: error };
		});
	},

  login(user, pass) {
    return axios.post('/api/auth/login', {
			username: '' + user,
			password: '' + pass
		})
		.then(response => {
        if (response.status === 200) {
					axios.defaults.headers.common['Authorization'] = response.headers.authorization;
					localStorage.setItem('access_token', response.headers.authorization);
          return response.data;
        } else {
          return { isError: true, error: response.data };
        }
			}
		)
		.catch(error => {
			console.error('error:', error);
			return { isError: true, error: error };
		});
  },

  logout() {
		return axios.post('/api/auth/logout')
		.then(response => {
			if (response.status === 204) {
				return {};
			} else {
				return { isError: true, error: response.data };
			}
		})
		.catch(error => {
			console.error('error:', error);
			return { isError: true, error: error };
		});
  },

  status() {
		return axios.get('/api/auth/status')
		.then(response => {
			if (response.status === 200) {
				return response.data;
			} else {
				return { isError: true, error: response.data };
			}
		})
		.catch(error => {
			return { isError: true, error: error };
		});
	},

	async install() {
		return axios.post('/api/auth/install')
		.then(response => {
			if (response.status === 200) {
				return response.data;
			} else {
				return { isError: true, error: response.data };
			}
		})
		.catch(error => {
			console.error('error:', error);
			return { isError: true, error: error };
		});
	},
  profile(profile) {
    if (profile) {
      // update
      return axios.post('/api/auth/profile', profile)
      .then(response => {
				if (response.status === 201 || response.status === 204) {
					return {};
				} else {
					return { isError: true, error: response.data };
				}
			})
			.catch(error => {
				console.error('error:', error);
				return { isError: true, error: error };
			});
    } else {
      // get
			return axios.get('/api/auth/profile')
			.then(response => {
				if (response.status === 200) {
					return response.data;
				} else if (response.status === 204 || response.status === 404) {
					return {};
				} else {
					return { isError: true, error: response.data };
				}
			})
			.catch(error => {
				console.error('error:', error);
				return { isError: true, error: error };
			});
    }
  }
};
