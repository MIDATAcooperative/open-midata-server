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
			    <div class="d-none d-lg-block" style="padding-top:100px;"></div>
				<div class="panel-container" style="max-width:600px; padding-top:20px; margin:0 auto;">
					<div class="panel panel-primary">
		            	<div class="panel-heading">
		              		<h3 class="panel-title" v-t="'service.title'"></h3>
		            	</div>
		            	<div class="panel-body">
			            	<error-box :error="error" />

							<div class="alert alert-success" v-if="success">
								<p v-t="'service.success'"></p>								
							</div>						
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
        const { $route, $router, $data } = this;
        		
        if ($route.query.token) {
           this.doBusy(server.post(jsRoutes.controllers.TokenActions.action().url, { token : $route.query.token }))
           .then(() => { $data.success = true; });
           return;
        }
        		
		let actions = [];
		let params = {};
		
		let copy = ["login","family","given","country","language","birthdate", "role"];
		for (let i=0;i<copy.length;i++)
		if ($route.query[copy[i]]) {
			params[copy[i]] = $route.query[copy[i]];
		}
        
        let pluginName = $route.query.pluginName || $route.params.pluginName;
        let pluginName2 = $route.query.open;        
		if ($route.meta.account) {
            actions.push({ ac : "account"});
		} else if (pluginName) {
			actions.push({ ac : "use", c : pluginName });
		} else if (pluginName2) {
		    actions.push({ ac : "open", c : pluginName2 });
		}					

		if (!$route.meta.account && !pluginName2) {
			if ($route.query.consent) {
				actions.push({ ac : "confirm", c : $route.query.consent });
			} else if ($route.query.project) {
				var prjs = $route.query.project.split(",");
				for (var j=0;j<prjs.length;j++) {
				    let prj = prjs[j];
				    if (prj.indexOf("|")>0) {
				      let p = prj.split("|");
				      actions.push({ ac : "study", s : p[0], c : p[1] });
				    } else {
					  actions.push({ ac : "study", s : prj });
					}		
				}					
			} else {
				actions.push({ ac : "unconfirmed" });
			}
		}
		
		
		if ($route.query.callback) {
			actions.push({ ac : "leave", c : $route.query.callback });
		} else {
			actions.push({ ac : "leave" });
		}
		params.actions=JSON.stringify(actions);

        let base = "/public";
        if (document.location.hash.indexOf('/portal')>=0) base = "/portal";

        /*if ($route.query.authToken) {
            $route.query.actions = params.actions;
            
            let data = {"authToken": $route.query.authToken };
		    let func = function(data) {
			    return server.post(jsRoutes.controllers.Application.authenticate().url, data);
		    };
		
		    session.performLogin(func, data, null)
		    .then(function(result) {
		        session.postLogin(result, $router, $route);
		    });
		} else*/ 
		if ($route.query.isnew) {
          $router.push({ path : base+"/registration", query : params });
		} else {
		  $router.push({ path : base+"/login", query : params });
		}			
    }
}
</script>