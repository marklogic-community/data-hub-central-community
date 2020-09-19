import Vue from 'vue';
import Router from 'vue-router';
import { sync } from 'vuex-router-sync';

import $store from './store';

Vue.use(Router);

let entryUrl = null

const isTesting = process.env.NODE_ENV === 'test'
const isHosted = process.env.VUE_APP_IS_HOSTED === 'true'

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
	if (
    $store.state.auth.authenticated ||
    !(to.meta.requiresLogin || to.meta.requiresUpdates)
  ) {
		if (to.name !== 'root.login' && entryUrl) {
			const url = entryUrl;
			entryUrl = null;
			next(url); // goto stored url
		}
		else {
    next();
		}
  } else {
		// entryUrl = to.path
    next({
      replace: true,
      name: 'root.login',
      params: { state: to.name, params: to.params }
    });
  }
};

const routes = [
    {
      path: '/',
      name: 'root.landing',
		redirect: isHosted ? '/upload' : '/model',
			meta: {}
		},
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
			import(/* webpackChunkName: "explorerpage" */ './views/ExplorerPage.vue'),
      meta: {
        label: 'Explore',
        navArea: 'header',
        requiresLogin: true,
        checkLogin
      }
		},
		{
      path: '/explore/compare',  //url path
      name: 'root.explorer.compare', //use to navigate to page
      // lazy-loading of page
      component: () =>
			import(/* webpackChunkName: "mergedcompare" */ './views/MergedCompare.vue'),
      meta: {
        label: 'Compare',
        // navArea: 'header',
        requiresLogin: true,
        checkLogin
      }
    },
    {
      path: '/notifications',  //url path
      name: 'root.notifications', //use to navigate to page
      // lazy-loading of page
      component: () =>
			import(/* webpackChunkName: "masteringnotifications" */ './views/MasteringNotifications.vue'),
      meta: {
        label: 'Notifications',
        requiresLogin: true,
        checkLogin
      }
		},
    {
      path: '/notifications/compare',  //url path
      name: 'root.notifications.compare', //use to navigate to page
      // lazy-loading of page
      component: () =>
			import(/* webpackChunkName: "masteringnotificationcompare" */ './views/MasteringNotificationCompare.vue'),
      meta: {
        label: 'Compare',
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
			import(/* webpackChunkName: "adminpage" */ './views/AdminPage.vue'),
      meta: {
        requiresLogin: true,
        checkLogin
      }
		},
		{
      path: '/export',  //url path
      name: 'root.export', //use to navigate to page
      // lazy-loading of page
      component: () =>
			import(/* webpackChunkName: "exportpage" */ './views/ExportPage.vue'),
      meta: {
        label: 'Export',
        navArea: 'header',
        requiresLogin: true,
        checkLogin
      }
    },
    {
      path: '/detail',
      name: 'root.details',
      // lazy-loading of page
      component: () => import(/* webpackChunkName: "detail" */ './views/DetailPage.vue'),
      meta: {
        label: 'View',
        navArea: 'document',
				requiresLogin: true,
				checkLogin
      }
	},
	{
		path: '*',
		component: () => import(/* webpackChunkName: "detail" */ './views/NotFoundPage.vue'),
		meta: {}
    }
  ]

if (isHosted || isTesting) {
	routes.splice(1, 0, {
		path: '/upload',
		name: 'root.upload',
		// lazy-loading of page
		component: () =>
			import(/* webpackChunkName: "upload" */ './views/UploadPage.vue'),
		props: {
			type: 'all'
		},
		meta: {
			label: 'Upload',
			navArea: 'header',
			requiresUpdates: true,
			checkLogin
		}
	})

	routes.splice(3, 0, {
		path: '/integrate/:stepName?',	//url path
		name: 'root.integrate', //use to navigate to page
		// lazy-loading of page
		component: () =>
			import(/* webpackChunkName: "integratepage" */ './views/IntegratePage.vue'),
		meta: {
			label: 'Integrate',
			navArea: 'header',
			requiresLogin: true,
			checkLogin
		}
	})

	routes.push({
		path: '/signup',
		name: 'root.signup',
		// lazy-loading of page
		component: () =>
			import(/* webpackChunkName: "login" */ './views/SignupPage.vue'),
		meta: {
			label: 'Signup',
			navArea: 'usermenu'
		}
	})
	routes.push({
		path: '/forgotPassword',
		name: 'root.forgotPassword',
		// lazy-loading of page
		component: () =>
			import(/* webpackChunkName: "login" */ './views/ForgotPasswordPage.vue'),
		meta: {}
	})
	routes.push({
		path: '/updatePassword',
		name: 'root.updatePassword',
		// lazy-loading of page
		component: () =>
			import(/* webpackChunkName: "login" */ './views/UpdatePasswordPage.vue'),
			meta: {}
	})
	routes.push({
		path: '/registrationComplete',
		name: 'root.registrationComplete',
		// lazy-loading of page
		component: () =>
			import(/* webpackChunkName: "login" */ './views/RegistrationCompletePage.vue'),
			meta: {}
	})
}
if (!isHosted || isTesting) {
	routes.push({
		path: '/know',	//url path
		name: 'root.know', //use to navigate to page
		// lazy-loading of page
		component: () =>
			import(/* webpackChunkName: "knowpage" */ './views/KnowPage.vue'),
		meta: {
			label: 'Know',
			navArea: 'header',
			requiresLogin: true,
			checkLogin
		}
	})
}

const $router = new Router({
	mode: 'history',
	base: process.env.BASE_URL,
	routes: routes
});

// Keep the router in sync with vuex store
sync($store, $router);

// Protect all protected routes, redirecting to login if needed
$router.beforeEach(checkLogin);

export default $router;
