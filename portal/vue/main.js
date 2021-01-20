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

import { createApp } from 'vue'

import App from './App.vue';
import router from './router';
import { setupI18n, loadLocaleMessages } from "services/lang.js";
import Filters from 'services/filters.js';
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

app.directive('t', {
  mounted(el, binding) { 	 
    el.innerText = i18n.$ts(binding.value);    
  }
});

app.directive('floating', {
  
    mounted: function( elem, binding ) {
        console.log(elem);
        elem.addEventListener('keyup', function() { if (elem.value) elem.classList.add("mi-x-has_value"); else elem.classList.remove("mi-x-has_value"); /*setAttribute('value', elem.value);*/ });
        elem.addEventListener('change', function() { if (elem.value) elem.classList.add("mi-x-has_value"); else elem.classList.remove("mi-x-has_value"); /*setAttribute('value', elem.value);*/ });          
        if (elem.value!="") elem.classList.add("mi-x-has_value");
    }
});

app.directive('validate', {
  mounted(el, binding) {
    console.log("USE");
    // We don't care about binding here.
    el.addEventListener('input', (e) => {
      const vm = binding.instance; // this is the Vue instance.   
      
      if (binding.value) {
        el.setCustomValidity(binding.value.call(binding.instance, el.value));
      }
      
      vm.errors = Object.assign({}, vm.errors, {
        [el.name]: e.target.validationMessage
      });
     
    });
    el.addEventListener('invalid', (e) => {
      const vm = binding.instance; // this is the Vue instance.              
      vm.errors = Object.assign({}, vm.errors, {
        [el.name]: e.target.validationMessage
      });
     
    });
  },
});

app.directive('submit', {
  mounted(el2, binding) {
    console.log("FORM");
    // We don't care about binding here.
    let el = el2.form;
    el2.addEventListener('click', (e) => {
               
        if (!el.checkValidity()) {
          e.preventDefault();
          e.stopPropagation();
          console.log("STOPPED");
        }

        const vm = binding.instance;
        vm.error = null;
        for (let field of vm.errors._custom) {
          el[field].classList.remove("is-invalid");
        }
        vm.errors = Object.assign({}, vm.errors, {
          "_custom": []
        });

        el.classList.add('was-validated')
      }, true);
    
  }
});
app.directive("pluginframe", PluginFrame);

app.use(router);

document.addEventListener("DOMContentLoaded", function(){      
    router.isReady().then(      
      () => {
        let loader = document.getElementById("loadscreen");
        if (loader) loader.style.display="none";
        app.mount('#app')
      }
    );
});


console.log("EXECUTED!");
