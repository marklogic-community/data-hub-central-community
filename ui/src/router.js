import Vue from 'vue';
import Router from 'vue-router';
import { sync } from 'vuex-router-sync';

import $store from './store';

Vue.use(Router);

const checkLogin = (to, from, next) => {
  if (!$store.state.initialized) {
    $store.dispatch('init').then(function() {
      redirectBasedOnAuth(to, from, next);
    });
  } else {
    redirectBasedOnAuth(to, from, next);
  }
};

const redirectBasedOnAuth = (to, from, next) => {
	if (to.name !== 'root.install' && $store.state.auth.authenticated && $store.state.auth.needsInstall) {
		next({
			replace: true,
			name: 'root.install',
			params: { state: to.name, params: to.params }
		})
	}
	else if (to.name == 'root.install' && $store.state.auth.authenticated && !$store.state.auth.needsInstall) {
		next({
			replace: true,
			name: 'root.landing'
		})
	}
	else if (
    $store.state.auth.authenticated ||
    !(to.meta.requiresLogin || to.meta.requiresUpdates)
  ) {
    next();
  } else {
    next({
      replace: true,
      name: 'root.login',
      params: { state: to.name, params: to.params }
    });
  }
};

const $router = new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/',
      name: 'root.landing',
			redirect: '/model',
			meta: {}
		},
		// {
    //   path: '/home',
    //   name: 'root.home',
    //   // lazy-loading of page
    //   component: () =>
    //     import(/* webpackChunkName: "landing" */ './views/LandingPage.vue'),
    //   meta: {
    //     label: 'Home',
    //     navArea: 'header'
    //   }
    // },
    // {
    //   path: '/upload/all',
    //   name: 'root.upload',
    //   // lazy-loading of page
    //   component: () =>
    //     import(/* webpackChunkName: "upload" */ './views/UploadPage.vue'),
    //   props: {
    //     type: 'all'
    //   },
    //   meta: {
    //     label: 'Upload',
    //     navArea: 'header',
    //     requiresUpdates: true,
    //     checkLogin
    //   }
    // },
    {
      path: '/model',  //url path
      name: 'root.modeler', //use to navigate to page
      // lazy-loading of page
      component: () =>
        import(/* webpackChunkName: "modeler" */ './views/ModelerPage.vue'),
      meta: {
        label: 'Connect',
        navArea: 'header',
        requiresLogin: true,
        checkLogin
      }
    },
    {
      path: '/explore',  //url path
      name: 'root.explorer', //use to navigate to page
      // lazy-loading of page
      component: () =>
        import(/* webpackChunkName: "modeler" */ './views/ExplorerPage.vue'),
      meta: {
        label: 'Explore',
        navArea: 'header',
        requiresLogin: true,
        checkLogin
      }
    },
    {
      path: '/know',  //url path
      name: 'root.know', //use to navigate to page
      // lazy-loading of page
      component: () =>
        import(/* webpackChunkName: "modeler" */ './views/KnowPage.vue'),
      meta: {
        label: 'Know',
        navArea: 'header',
        requiresLogin: true,
        checkLogin
      }
    },
    {
      path: '/deploy',  //url path
      name: 'root.deploy', //use to navigate to page
      // lazy-loading of page
      component: () =>
        import(/* webpackChunkName: "modeler" */ './views/DeployPage.vue'),
      meta: {
        label: 'Deploy',
        navArea: 'header',
        requiresLogin: true,
        checkLogin
      }
    },
    /*{
      path: '/map',  //url path
      name: 'root.mapper', //use to navigate to page
      // lazy-loading of page
      component: () =>
        import(/* webpackChunkName: "mapper" * /
        './views/MapPage.vue'),
      meta: {
        label: 'Map',
        navArea: 'header'
      }
    },*/
    // {
    //   path: '/search/all',
    //   name: 'root.search',
    //   // lazy-loading of page
    //   component: () =>
    //     import(/* webpackChunkName: "search" */ './views/SearchPage.vue'),
    //   props: {
    //     type: 'all'
    //   },
    //   meta: {
    //     label: 'Search',
    //     navArea: 'header',
    //     requiresLogin: true,
    //     checkLogin
    //   }
    // },
    // {
    //   path: '/create/all',
    //   name: 'root.create',
    //   // lazy-loading of page
    //   component: () =>
    //     import(/* webpackChunkName: "create" */ './views/CreatePage.vue'),
    //   props: {
    //     type: 'all'
    //   },
    //   meta: {
    //     label: 'Create',
    //     navArea: 'header',
    //     requiresUpdates: true
    //   }
    // },
    {
      path: '/install',
      name: 'root.install',
      // lazy-loading of page
      component: () =>
        import(/* webpackChunkName: "login" */ './views/InstallPage.vue'),
      meta: {
        requiresLogin: true,
        checkLogin
      }
		},
		{
      path: '/login',
      name: 'root.login',
      // lazy-loading of page
      component: () =>
        import(/* webpackChunkName: "login" */ './views/LoginPage.vue'),
      meta: {
        label: 'Login',
        navArea: 'usermenu'
      }
    },
    {
      path: '/admin',  //url path
      name: 'root.admin', //use to navigate to page
      // lazy-loading of page
      component: () =>
        import(/* webpackChunkName: "modeler" */ './views/AdminPage.vue'),
      meta: {
        requiresLogin: true,
        checkLogin
      }
    },
    // {
    //   path: '/profile',
    //   name: 'root.profile',
    //   // lazy-loading of page
    //   component: () =>
    //     import(/* webpackChunkName: "profile" */ './views/ProfilePage.vue'),
    //   meta: {
    //     label: 'Profile',
    //     navArea: 'usermenu',
    //     requiresLogin: true
    //   }
    // },
    // {
    //   path: '/edit/all/:id',
    //   name: 'root.edit',
    //   // lazy-loading of page
    //   component: () =>
    //     import(/* webpackChunkName: "create" */ './views/CreatePage.vue'),
    //   props($route) {
    //     return {
    //       type: 'all',
    //       id: $route.params.id
    //     };
    //   },
    //   meta: {
    //     label: 'Edit',
    //     navArea: 'document',
    //     requiresUpdates: true
    //   }
    // },
    // {
    //   path: '/detail/all/:id',
    //   name: 'root.view',
    //   // lazy-loading of page
    //   component: () =>
    //     import(/* webpackChunkName: "detail" */ './views/DetailPage.vue'),
    //   props($route) {
    //     return {
    //       type: 'all',
    //       id: $route.params.id
    //     };
    //   },
    //   meta: {
    //     label: 'View',
    //     navArea: 'document',
    //     requiresLogin: true
    //   }
    // }
  ]
});

// Keep the router in sync with vuex store
sync($store, $router);

// Protect all protected routes, redirecting to login if needed
$router.beforeEach(checkLogin);

export default $router;
