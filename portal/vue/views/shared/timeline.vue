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
    <div  class="midata-overlay borderless">
    	<div class="panel panel-primary">
			<div class="panel-heading">
                <span v-t="'timeline.title'"></span>
		    </div>
            <div class="panel-body">
                <p v-t="'help_welcome.starts'"></p>
                <div class="row">
                    <div v-if="role=='MEMBER'" class="col-6 col-md-3 col-lg-2 spacesiconbar" style="position:relative">
                        <a href="javascript:" @click="use('fitness')"><img class="img-responsive" src="/images/fitness.jpeg"></a>
                        <div class="position:relative"><a href="javascript:" @click="use('fitness')" v-t="'timeline.fitness'">Fitness</a></div>
                    </div>
                    <div class="col-6 col-md-3 col-lg-2 spacesiconbar" style="position:relative">
                        <router-link :to="{ path : './market', query : { tag : 'Protocol', user : targetUser } }"><img class="img-responsive" src="/images/papers.jpg"></router-link>
                        <div class="position:relative"><router-link :to="{ path : './market', query : { tag : 'Protocol', user : targetUser } }" v-t="'timeline.health'">Health Documentation</router-link></div>
                    </div>
                    <div class="col-6 col-md-3 col-lg-2 spacesiconbar" style="position:relative">
                        <router-link :to="{ path : './market', query : { tag : 'Analysis', user : targetUser } }"><img class="img-responsive" src="/images/question.jpeg"></router-link>
                        <div class="position:relative"><router-link :to="{ path : './market', query : { tag : 'Analysis', user : targetUser } }" v-t="'timeline.analyze'">Analyze your Data</router-link></div>
                    </div>
                    <div class="col-6 col-md-3 col-lg-2 spacesiconbar" v-for="space in spaces" :key="space._id" style="position:relative">
                        <router-link :to="{ path : './spaces', query : { spaceId : space._id, user : targetUser }}"><img class="img-responsive" :src="getIconUrl(space)"></router-link>
                        <div class="position:relative">
                            <div style="position:absolute;right:16px;"><a href="javascript:" @click="deleteSpace(space);"><span class="fas fa-times-circle"></span></a></div>
                            <router-link :to="{ path : './spaces', query : { spaceId : space._id } }">{{ space.name}}</router-link>
                        </div>		 
                    </div>
                </div>
    
                <div class="row">
                    <div class="col-sm-12" v-if="url">		    	         														
                        <div id="iframe-placeholder">
                            <div id="iframe" style="min-height:200px;width:100%;" v-pluginframe="url"></div>						
                        </div>											
                    </div>
                </div>							 				
                       
                        
            </div>
	    </div>
    </div>
</template>
<script>
import { getLocale } from 'services/lang.js';
import spaces from 'services/spaces.js';
import server from 'services/server.js';
import apps from 'services/apps.js';
import session from 'services/session.js';
import { status, ErrorBox } from 'basic-vue3-components';
import ENV from "config";


export default {
    
    data: () => ({
        
	    userId : null,
        spaceId : null,
        role : null,
	    params : null,
        targetUser : null,
		spaces : [],
		url : null
	}),				

	components : { ErrorBox },

    mixins : [ status ],

    methods : {
        openAppLink(data) {
            const { $data, $route, $router } = this;
		    data = data || {};
		    data.user = $route.query.user;
		    spaces.openAppLink($router, $route, $data.userId, data);	 
        },
        
        use(view) {
            const { $data, $route, $router } = this;
		    spaces.openAppLink($router, $route, $data.userId, { app : view, user : $data.targetUser });
	    },
	
	    getIconUrl(space) {
		    if (!space.visualization) return null;
		    return ENV.apiurl + "/api/shared/icon/APPICON/" + space.visualization;
        },
        
        deleteSpace(space) {
            const { $data } = this;
		    server.delete(jsRoutes.controllers.Spaces["delete"](space._id).url).
			then(function() {
				$data.spaces.splice($data.spaces.indexOf(space), 1);
			});
        },
        
        getAuthToken(space) {
			const { $data, $route } = this;
			this.doBusy(spaces.getUrl(space._id, $route.query.user)
			.then(function(result) {   
				$data.title = result.data.name;
				$data.url = spaces.mainUrl(result.data, getLocale(), $data.params);							
			}));
		}
    },

    created() {
        const { $data, $route, $router } = this, me = this;
        $data.role = $route.meta.role.toUpperCase();
        if ($route.query.params) $data.params = JSON.parse($route.query.params); else $data.params = null;
        if ($route.query.user) $data.targetUser = $route.query.user;
        if ($route.query.spaceId) $data.spaceId = $route.query.spaceId; // ????

        session.currentUser
	    .then(function(userId) {
			$data.userId = userId;
			
			spaces.autoadd().then(function() {
			    spaces.getSpacesOfUserContext(userId, "me").then(function(result) {
				    $data.spaces = [];
				    for (let s of result.data) {
					    if (s.name != "Fitness" && s.name != "Timeline") {
					        $data.spaces.push(s);
					    }
				    }
			    });
			});
			
			server.post(jsRoutes.controllers.Plugins.get().url, { "properties" : { "filename" : "timeline" }, "fields": ["_id", "type"] })
			.then(function(result) {				
				  if (result.data.length == 1) {
					  let app = result.data[0];
					  spaces.get({ "owner": userId, "visualization" : result.data[0]._id }, ["_id"])
					  .then(function(spaceresult) {
						 if (spaceresult.data.length > 0) {
							 let target = spaceresult.data[0];
							 me.getAuthToken(target, $route.query.user);							 
						 } else {
							 
							 
							 apps.installPlugin(app._id, { applyRules : true, context : "me" })
							 .then(function(result) {				
									//
									if (result.data && result.data._id) {
										me.getAuthToken(result.data, $data.user);
									} 
								});
						 }
					  });
				  } else {
					  $router.push({ path : "./overview2" });
				  }
			});				
			
	    });
    }
}
</script>