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
    <div class="container">
		<div class="row">
			<!-- Login -->
			<div class="col-sm-12">
				<div class="panel-container" style="max-width:600px; padding-top:120px; margin:0 auto;">
					<div class="panel panel-primary">
		            	<div class="panel-heading">
		              		<h3 class="panel-title" v-t="'lostpw.title'"></h3>
		            	</div>
		            	<div class="panel-body" v-if="!success">
			            	
							<p v-t="'lostpw.description'"></p>
							<form name="form" @submit.prevent="submit()" role="form" class="form form-horizontal">							  
								<form-group name="email" label="lostpw.email" :path="errors.email">
								  <input type="text" class="form-control" v-model="lostpw.email" style="margin-bottom:5px;" autofocus required v-validate>
								</form-group>
                                <error-box :error="error"></error-box>								
								<button type="submit" v-submit class="btn btn-primary btn-block" :disabled="action!=null" v-t="'lostpw.continue'"></button>
							</form>							
														
		            	</div>
		            	<div class="panel-body" v-if="success">
		            	   <p v-t="'lostpw.success'"></p>
		            	   <button class="btn btn-primary" type="button" v-t="'common.back_btn'" @click="back();"></button>
		            	</div>
					</div>
				</div>
			</div>
			
		</div>
	</div>
</template>
<script>
import server from "services/server.js";
import { status, FormGroup, ErrorBox } from 'basic-vue3-components';

export default {
  data: () => ({
     success : false,
     lostpw : { email : "" }    
  }),

  components : {
     FormGroup, ErrorBox
  },

  mixins : [ status ],

  
  methods : {
    back() {
        const { $router } = this;
        $router.go(-1);
    },
    
    submit() {	
        const { $data, $route }	= this;
       
		// send the request
		var data = { "email": $data.lostpw.email, "role" : $route.meta.role };
		this.doAction("pw",server.post(jsRoutes.controllers.Application.requestPasswordResetToken().url, data)).
		then(function() { 
			$data.success = true; 
		});
	}
  },

  created() {    
     this.loadEnd();
  }
}
</script>