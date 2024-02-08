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
				<div class="col-sm-12">
				    <div class="d-none d-lg-block" style="padding-top:100px;"></div>
					<div class="panel-container" style="max-width:600px; padding-top:20px; margin:0 auto;">
						<div class="panel panel-primary">
			            	<div class="panel-heading">
			              		<h3 class="panel-title" v-t="'unsubscribe.title'"></h3>
			            	</div>
			            	<div class="panel-body">
				            	<error-box :error="error" />
	
								<div class="alert alert-success" v-if="success">														 
								  {{ $t("unsubscribe.success") }}								
								</div>							
								<p><router-link :to="{ path : '/public/login' }">{{ $t("unsubscribe.back_to_login") }}</router-link></p>
														
			            	</div>
						</div>
					</div>
				</div>				
			</div>
	</div>
</template>
<script>
import server from "services/server.js";
import session from "services/session.js";
import { status, ErrorBox } from 'basic-vue3-components';
export default {
   data: () => ({
     success : false,         
   }),
   
    components : {
      ErrorBox
    },
    
    mixins : [ status ],

    created() {
        const { $route, $data } = this;
        		                                  
        this.doBusy(server.post(jsRoutes.controllers.BulkMails.unsubscribe().url, { token : $route.query.token }))
        .then(() => { $data.success = true; });
        return;
        			
    }
}
</script>