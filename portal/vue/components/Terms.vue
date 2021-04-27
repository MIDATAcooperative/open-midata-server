<!--
 This file is part of the Open MIDATA Server.
 
 The Open MIDATA Server is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 any later version.
 
 The Open MIDATA Server is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
-->

<template>    
	<div class="terms" v-html="terms.text"></div>			            		
</template>

<script>

import server from "services/server.js";
import terms from "services/terms.js";
import { getLocaleRef } from "services/lang.js";
import sanitizeHtml from 'sanitize-html';
import { ref, reactive } from 'vue'

export default {    
  props: [ 'which' ],

  data : ()=>({
      name : "",
      version : "",
	  terms : { title : "", text : "" },
	  lang : null
  }),

  watch : {
	  lang() {		 
		  const { $data } = this;
		  this.init($data.name, $data.version, $data.lang);
	  },

	  which() {
		  const { $data } = this, me = this;
		  let which = (me.which ? me.which : "").split("--");	      
	      if (which[0]) me.init(which[0], which[1], $data.lang);  
	  }
  },

  methods : {
     loadTerms(name, version, language) {
        const { $data, $emit } = this;
		terms.get(name, version,language)
		.then(function(result) {			
			$data.terms.title = result.data.title;
			$data.terms.text = sanitizeHtml(result.data.text);
			$emit("title", $data.terms.title);
		}, function() {
			$data.terms = { title : "Not found", "text" : "The requested terms and conditions are not available."};
			$emit("title", "Not found");
		});
    },

    init(name, version, language) {
        const { $data, $route } = this, me = this;        
		$data.name = name;
		$data.version = version;
		
		if ($route.meta.termsRole && (name == "midata-privacy-policy" || name == "midata-terms-of-use")) {
			server.get(jsRoutes.controllers.Terms.currentTerms().url).then(function(result) {
				 let w = "--";
			  	 if (name == "midata-terms-of-use") {
			  		w = result.data[$route.meta.termsRole].termsOfUse.split("--");;			  		
			  	 } else if (name == "midata-privacy-policy") {
			  		w = result.data[$route.meta.termsRole].privacyPolicy.split("--");;
			  	 }
			  	name = w[0];
		  		version = w[1];			  	
			  	me.loadTerms(name, version, language);
			});	
		} else me.loadTerms(name, version, language);
		
	}
    

  },

  mounted() {
	const { $data,  $route } = this, me = this;	
	let which = ($route.query.which || (me.which ? me.which : "")).split("--");
	$data.lang = $route.query.lang || getLocaleRef();
	if (which[0]) me.init(which[0], which[1], $data.lang);     
  }
}
</script>