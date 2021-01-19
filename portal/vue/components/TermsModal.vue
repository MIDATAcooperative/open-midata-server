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
   <div>	 
  
        <div class="margin-top old">
			 <button class="btn btn-sm btn-primary" @click="close()" v-t="'terms.close_btn'"></button>
			 <div class="extraspace"></div>
		</div>
		<div class="panel panel-primary mi-or-terms">
		   	<div class="panel-heading">		            	    
		   		<h3 class="panel-title">{{ title }}</h3>
		  	</div>
		   	<a class="new mi-mo-menu_item" @click="close()">
                <div class="mi-at-icon mi-at-icon-notifications" >
                    <img src="/images/close.png">
                </div>
                <span class="mi-at-text" v-t="'terms.close_new_btn'">Close</span>
            </a>
		    <div class="panel-body">
                <terms :which="which" @title="title=$event"></terms>			    
			    <div class="old">
			        <div class="extraspace"></div>
			        <button class="btn btn-primary" @click="close()" v-t="'terms.close_btn'"></button>
			    </div>
			    <a class="new mi-mo-menu_item" @click="close()">
                    <div class="mi-at-icon mi-at-icon-notifications" >
                        <svg role="img">
                            <use xlink:href="/img/icons.svg#close" />
                        </svg>
                    </div>
                    <span class="mi-at-text" v-t="'terms.close_new_btn'">Close</span>
                </a>			            	
		    </div>
		</div>				 
    </div>		
</template>

<script>
import Terms from "components/Terms.vue";

export default {    
  props: ['which'],

  data : ()=>({      
      title : ""
  }),

  components : { Terms },

  methods : {
      close() {
        const { $emit } = this;
		$emit("close");
	  }
  }
}
</script>