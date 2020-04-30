# Vue-MarkLogic-Node

This project provides a skeleton for a Vue.js front-end stack, designed to run against a grove-node middle-tier, backed by MarkLogic.

This project is still Work In Progress.

## Quick Start

This template has not been integrated into grove-cli yet, so you'll have to scrape things together yourself. Next to this project, you'll need:

- a copy/clone of latest grove-node
- an instance of some MarkLogic REST api

You could potentially use grove-cli to gather most, hookup your copy of this project to that middle-tier, and launch the Vue-frontend instead of the React-frontend.

Make note at which host and port your middle-tier is running. The Vue front-end by default runs with a hot-reload feature, that runs at its own port (typically 8080, or higher if occupied), and needs to know to where backend calls need to be proxied:

- edit appHost and appPort constants in vue.config.js (should point to the middle-tier)

After that you can pull in dependencies, and launch the front-end:

- npm install
- npm run serve

A browser should open automatically (typically at localhost:8080).

## Using HTTPS

There are two places to think about HTTPS:

- When serving the files of this UI application to a client (a browser), and
- When this UI application makes network calls to a middle-tier or other backend.

As the sections below make clear, in most production-like situations, nothing needs to change in this application when moving from HTTP to HTTPS or vice-versa.

### Using HTTPS when serving the UI application

You will most often want to use HTTPS in a production-like environment. Typically, in such an environment, this UI will have been transpiled and minified into a set of static files (possible to achieve by running `npm run build`). A file server (which could be a Grove middle-tier, but could also be Apache, Nginx, etc, which serves static assets and proxies back to a middle-tier) will then serve those files to clients. The file server should be configured to use HTTPS - and nothing special has to be done in this UI application.

Sometimes, however, you will want to use HTTPS in development, when you are making use of the Webpack development server bundled with this Vue.js CLI based app. This is easy to setup: Simply set the `VUE_APP_ENABLE_HTTPS_IN_FRONTEND` environment variable to true.

You can do this in .env.development (shared with your team) or .env.development.local (only for your local machine):

```
VUE_APP_ENABLE_HTTPS_IN_FRONTEND=true
```

### Using HTTPS when making network calls

As in the last section, in a production situation, nothing special needs to be done. All network calls should be relative URLs, inheriting the protocol (https), host and port from which the UI application files themselves were served.

In development, when your middle-tier or other backend requires HTTPS, simply set the `VUE_APP_HTTPS_ENABLED_IN_MIDDLETIER` environment variable to true.

You can do this in .env.development (shared with your team) or .env.development.local (only for your local machine):

```
VUE_APP_HTTPS_ENABLED_IN_MIDDLETIER=true
```

## NPM command overview

### Install all dependencies
```
npm install
```

### Install prod dependencies only
```
npm install --only=prod
```

### Compiles and hot-reloads for development
```
npm run serve
```

### Compiles and minifies for production
```
npm run build
```

### Lints and fixes files
```
npm run lint
```

### Run your unit tests
```
npm run test:unit
```

### Run your end-to-end tests
```
npm run test:e2e
```

