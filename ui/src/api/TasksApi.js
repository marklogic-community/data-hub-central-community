var api = '/api/tasks/';

// TODO: consider refactoring out to utils library with identical
// function in grove-vue-visjs-graph
function buildUrl(path, params) {
  var url = new URL(api + path, window.location.href);
  if (params) {
    Object.keys(params).forEach(key => {
      if (Array.isArray(params[key])) {
        params[key].map(param => url.searchParams.append(key, param));
      } else {
        url.searchParams.append(key, params[key]);
      }
    });
  }
  return url;
}

// // copied from Angular.js
// function isObject(value) {
//   // http://jsperf.com/isobject4
//   return value !== null && typeof value === 'object';
// }

export default {
  name: 'TasksApi',
  toModel(params) {
    return fetch(buildUrl('to-model', params), {
      method: 'GET',
      credentials: 'same-origin'
    }).then(
      response => {
        return response.json().then(text => {
          return { isError: false, response: json };
        });
      },
      error => {
        return { isError: true, error: error };
      }
    );
  },
  fromModel(data, params) {
    return fetch(buildUrl('from-model', params), {
      method: 'PUT',
      headers: {
        'content-type': 'application/json'
      },
      body: JSON.stringify(data),
      credentials: 'same-origin'
    }).then(
      response => {
        return response.text().then(text => {
          return { isError: false, response: text };
        });
      },
      error => {
        return { isError: true, error: error };
      }
    );
  }
};
