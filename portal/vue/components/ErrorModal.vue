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
	<modal id ="errormodal" :full-width="true" :open="open" @close="cancel()" :title="$t(key+'_title')">
	      <div class="body">
	          <p>{{ $t(key+'_1') }}</p>	          
			  <p>{{ $t(key+'_2') }}</p>	    
			  <p>{{ $t("error.contact_support") }}</p>      
	          <div class="extraspace"></div>
	      </div>
	      <template v-slot:footer>      	          
	          <button class="btn btn-default space mb-1" v-t="'common.close_btn'" @click="cancel()"></button>
	      </template>
	  </modal>
</template>
<script>

import { Modal } from 'basic-vue3-components'

export default {
    inheritAttrs : false,
    props : {
        error : Object
    },
	
	components: { Modal },
	
	data : ()=>({      
	    key : "",
	    open : false
	}),
	
	watch : {
		error(newError, oldError) {
			let k = newError;
			if (!k) return; 
			if (k.code) k = k.code;
			console.log("TEST: "+k);
			if (this.$t(k+"_show") === "@modal") {
				this.key = k;
				this.open = true;
			}
        }
	},
    
    methods : {
       cancel() {
		 this.open = false;
		 this.key = "";
	   }    
    }
}
</script>
