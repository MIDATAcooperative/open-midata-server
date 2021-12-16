/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

import { createApp, watch } from 'vue'

import App from './App.vue';
import router from './router';
import { setupI18n, loadLocaleMessages } from "services/lang.js";
import Vue3Components, { Filters } from 'basic-vue3-components';
import VueCookies from 'vue3-cookies';
import PluginFrame from "directives/pluginframe.js";

require('bootstrap/dist/js/bootstrap.bundle');

const i18n = setupI18n({
    locale: "en",
    fallback: "en",
    messages : { en : { dummy : "test" } } 
});


const app = createApp(App);   
app.config.globalProperties.$filters = Filters;
app.config.globalProperties.$t = i18n.$ts;
app.use(Vue3Components);
app.use(VueCookies);
app.directive('t', {
  mounted(el, binding) { 	 
	let v = i18n.$t(binding.value);
    el.innerText = v.value;    
    watch(v, (x) => { el.innerText = x;});
  }
});

app.directive('floating', {
  
    mounted: function( elem, binding ) {
       
        elem.addEventListener('keyup', function() { if (elem.value) elem.classList.add("mi-x-has_value"); else elem.classList.remove("mi-x-has_value"); /*setAttribute('value', elem.value);*/ });
        elem.addEventListener('change', function() { if (elem.value) elem.classList.add("mi-x-has_value"); else elem.classList.remove("mi-x-has_value"); /*setAttribute('value', elem.value);*/ });          
        if (elem.value!="") elem.classList.add("mi-x-has_value");
    }
});


app.directive("pluginframe", PluginFrame);

app.use(router);

let domain = document.location.hostname;

document.addEventListener("DOMContentLoaded", function(){      
    router.isReady().then(      
      () => {
        let loader = document.getElementById("loadscreen");
        if (loader) loader.style.display="none";
        app.mount('#app')
      }
    );
});


