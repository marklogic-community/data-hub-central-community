<template>
  <div>
    <section class="content-header">
      <h1>Successful Deployment! <small>Grove Search App</small></h1>
    </section>

    <section class="content landing">
      <p>You can go check out the sample application once you've logged in! If you loaded the documents from the <em>data</em> directory via the instructions in the readme, you should also be able to <router-link :to="{ name: 'root.search' }">search</router-link> over the included dictionary. The application allows you to <router-link :to="{ name: 'root.create' }">create</router-link> various people that you can then <router-link :to="{ name: 'root.search' }">search</router-link>. If you fill out all the fields, you'll be able to use the built-in <em>faceting</em> and <em>typeahead</em> features!</p>
      <p>If you need any help making the application your own refer to the links below:</p>
      <div class="row">
        <div class="col-md-6">
          <div class="panel panel-primary">
            <div class="panel-heading">
              <h3 class="panel-title">Vue UI Template</h3>
            </div>
            <div class="panel-body" style="min-height: 90px;">
              <p>
                This UI Template for Grove is built on top of a base template generated with Vue CLI. You can find elaborate documentation about it here:
                <a href="https://cli.vuejs.org" target="_blank" rel="noopener">vue-cli documentation</a>.
              </p>
              <!-- TODO: add links to github wiki pages with guides, design, recipes, etc etc.. -->
            </div>
          </div>

          <div class="panel panel-primary">
            <div class="panel-heading">
              <h3 class="panel-title">Recommended Guides and Sites</h3>
            </div>
            <div class="panel-body">
              <ul>
                <li><a href="https://docs.marklogic.com/">MarkLogic Documentation</a></li>
                <li><a href="https://github.com/marklogic-community/ml-gradle/wiki">Deploying into MarkLogic Using ML-Gradle</a></li>
                <li><a href="https://docs.marklogic.com/guide/rest-dev">MarkLogic REST Application Developer's Guide</a></li>
                <li><a href="https://github.com/marklogic">MarkLogic Github of Supported Tools</a></li>
                <li><a href="https://github.com/marklogic-community">MarkLogic Github of Community Tools</a></li>
              </ul>
            </div>
          </div>

          <div class="panel panel-primary">
            <div class="panel-heading">
              <h3 class="panel-title">Installed Vue plugins</h3>
            </div>
            <div class="panel-body">
              <ul>
                <li><a href="https://router.vuejs.org" target="_blank" rel="noopener">vue-router (page routing)</a></li>
                <li><a href="https://vuex.vuejs.org" target="_blank" rel="noopener">vuex (state manager)</a></li>
                <li><a href="https://github.com/vuejs/vue-devtools#vue-devtools" target="_blank" rel="noopener">vue-devtools (allow browser debugging)</a></li>
                <li><a href="https://vue-loader.vuejs.org" target="_blank" rel="noopener">vue-loader (vue loader for webpack)</a></li>
                <li><a href="https://github.com/vuejs/awesome-vue" target="_blank" rel="noopener">awesome-vue (get more vue stuff here)</a></li>
              </ul>
            </div>
          </div>

          <div class="panel panel-primary">
            <div class="panel-heading">
              <h3 class="panel-title">Installed CLI plugins</h3>
            </div>
            <div class="panel-body">
              <ul>
                <li><a href="https://github.com/vuejs/vue-cli/tree/dev/packages/%40vue/cli-plugin-babel" target="_blank" rel="noopener">babel (transpiler)</a></li>
                <li><a href="https://github.com/vuejs/vue-cli/tree/dev/packages/%40vue/cli-plugin-pwa" target="_blank" rel="noopener">pwa (progressive web apps)</a></li>
                <li><a href="https://github.com/vuejs/vue-cli/tree/dev/packages/%40vue/cli-plugin-eslint" target="_blank" rel="noopener">eslint (code style checker)</a></li>
                <li><a href="https://github.com/vuejs/vue-cli/tree/dev/packages/%40vue/cli-plugin-unit-jest" target="_blank" rel="noopener">unit-jest (unit tester)</a></li>
                <li><a href="https://github.com/vuejs/vue-cli/tree/dev/packages/%40vue/cli-plugin-e2e-nightwatch" target="_blank" rel="noopener">e2e-nightwatch (selenium based e2e tester)</a></li>
              </ul>
            </div>
          </div>
        </div>

        <div class="col-md-6">
          <div class="panel panel-primary">
            <div class="panel-heading">
              <h3 class="panel-title">Stack Technology</h3>
            </div>
            <div class="panel-body">
              <b>Backend</b>
              <ul>
                <li><a href="http://www.marklogic.com">Database - MarkLogic</a></li>
                <li><a href="https://github.com/marklogic-community/ml-gradle">Deployment - ML-Gradle</a></li>
              </ul>
              <b>Middle-tier</b>
              <ul>
                <li><a href="http://nodejs.org/">NodeJS</a></li>
                <li><a href="https://expressjs.com/">ExpressJS</a></li>
              </ul>
              <b>Frontend</b>
              <ul>
                <li><a href="https://vuejs.org/v2/guide/">UI Framework - Vue.js</a></li>
                <li><a href="http://getbootstrap.com/">Styling - Bootstrap</a></li>
                <li><a href="http://lesscss.org/">Styling - LESS</a></li>
              </ul>
            </div>
          </div>

          <div class="panel panel-primary">
            <div class="panel-heading">
              <h3 class="panel-title">IDE Suggestions</h3>
            </div>
            <div class="panel-body">
              <b><a href="https://code.visualstudio.com/download">Visual Studio Code</a></b>
              <ul>
                <li>Debugger for Chrome</li>
                <li>EditorConfig for VS Code</li>
                <li>ESLint</li>
                <li>gitflow</li>
                <li>GitLens - Git supercharged</li>
                <li>openapi-viewer</li>
                <li>Prettier - Code formatter</li>
                <li>Vetur</li>
              </ul>

              <b><a href="http://www.sublimetext.com/3">Sublime Text 3</a></b>
              <ul>
                <li><a href="https://sublime.wbond.net/">Package Control</a>
                  <ul>
                    <li><a href="https://github.com/paxtonhare/MarkLogic-Sublime">MarkLogic Plugin</a></li>
                    <li><a href="https://github.com/uipoet/sublime-jshint">sublime-jshint</a></li>
                    <li><a href="https://github.com/alexnj/SublimeOnSaveBuild">SublimeOnSaveBuild</a></li>
                    <li><a href="https://github.com/sindresorhus/editorconfig-sublime">editorconfig-sublime</a></li>
                    <li><a href="https://github.com/danro/LESS-sublime">LESS (CSS-style highlighting)</a></li>
                  </ul>
                </li>
              </ul>

                <!--li><a href="https://www.jetbrains.com/idea/download/#section=mac">IntelliJ IDEA Community</a></li>

                <li><a href="https://www.jetbrains.com/webstorm/">Webstorm</a></li>

                <li><a href="https://macromates.com/download">TextMate</a>
                  <ul>
                    <li><a href="https://github.com/bodnaristvan/JSHint.tmbundle#readme">jshint</a></li>
                    <li><a href="https://github.com/Mr0grog/editorconfig-textmate#readme">editorconfig</a></li>
                    <li><a href="https://github.com/paxtonhare/xquery.tmbundle">MarkLogic auto-completion: XQuery.tmbundle</a></li>
                  </ul>
                </li-->
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
// @ is an alias to /src
import HelloWorld from '@/components/HelloWorld.vue';

export default {
  name: 'LandingPage',
  components: {
    HelloWorld
  }
};
</script>
