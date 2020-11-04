/* eslint-env node */
/* eslint-disable no-console */

function bool(val, def) {
	if (val !== undefined || val !== null) {
		return '' + val === 'true';
	} else {
		return def;
	}
}

const appHost = 'localhost';
const appPort = process.env.VUE_APP_MIDDLETIER_PORT || '9003';
const appFrontendUsesHttps = bool(
	process.env.VUE_APP_ENABLE_HTTPS_IN_FRONTEND,
	false
);
const appMiddletierUsesHttps = bool(
	process.env.VUE_APP_HTTPS_ENABLED_IN_MIDDLETIER,
	false
);

const middletierUrl =
	(appMiddletierUsesHttps ? 'https' : 'http') + '://' + appHost + ':' + appPort;

module.exports = {
	lintOnSave: false,
	configureWebpack: {
		devtool: 'eval-source-maps'
	},
	chainWebpack: config => {
		config.plugin('html')
		.tap(args => {
			args[0].tracking = (process.env.GTAG_ID && process.env.VUE_APP_IS_HOSTED === 'true') ? `<!-- Global site tag (gtag.js) - Google Analytics -->
			<script async src="https://www.googletagmanager.com/gtag/js?id=G-TH5YWDQ8KL"></script>
			<script>
				window.dataLayer = window.dataLayer || [];
				function gtag(){dataLayer.push(arguments);}
				gtag('js', new Date());

				gtag('config', '${process.env.GTAG_ID}');
			</script>` : ''
			return args
		})
	},
	devServer: {
		port: process.env.VUE_APP_DEV_PORT || 8081,
		https: appFrontendUsesHttps,
		proxy: {
			'/websocket': {
				target: middletierUrl,
				ws: true,
				secure: false,
				bypass: function(req) {
					if (req.url.startsWith('/websocket')) {
						console.log(
							'Proxying ' + req.method + ' ' + req.url + ' to ' + middletierUrl
						);
					} else {
						return req.url;
					}
				}
			},
			'/api': {
				target: middletierUrl,
				secure: false,
				bypass: function(req) {
					if (req.url.startsWith('/api')) {
						console.log(
							'Proxying ' + req.method + ' ' + req.url + ' to ' + middletierUrl
						);
					} else {
						return req.url;
					}
				}
			},
			// for legacy proxying support
			'/v1': {
				target: middletierUrl,
				secure: false,
				bypass: function(req) {
					if (req.url.startsWith('/v1')) {
						console.log(
							'Proxying ' + req.method + ' ' + req.url + ' to ' + middletierUrl
						);
					} else {
						return req.url;
					}
				}
			}
		}
	}
};
