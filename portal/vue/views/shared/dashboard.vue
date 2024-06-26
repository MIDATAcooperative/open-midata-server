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
        <panel :title="$t('visualization.location.'+dashId)" :busy="isBusy">
		        
                <div class="row">
                    <div v-if="role=='MEMBER'" class="col-6 col-md-3 col-lg-2 spacesiconbar" style="position:relative">
                        <a href="javascript:" @click="use('fitness')"><img class="img-responsive" src="/images/fitness.jpeg"></a>
                        <div class="position:relative"><a href="javascript:" @click="use('fitness')" v-t="'timeline.fitness'">Fitness</a></div>
                    </div>
                    <div class="col-6 col-md-3 col-lg-2 spacesiconbar" style="position:relative">
                        <router-link :to="{ path : './market', query : { tag : 'Protocol', context : dashId,user : targetUser } }"><img class="img-responsive" src="/images/papers.jpg"></router-link>
                        <div class="position:relative"><router-link :to="{ path : './market', query : { tag : 'Protocol', context : dashId, user : targetUser } }" v-t="'timeline.health'">Health Documentation</router-link></div>
                    </div>
                    <div class="col-6 col-md-3 col-lg-2 spacesiconbar" style="position:relative">
                        <router-link :to="{ path : './market', query : { tag : 'Analysis', context : dashId, user : targetUser } }"><img class="img-responsive" src="/images/question.jpeg"></router-link>
                        <div class="position:relative"><router-link :to="{ path : './market', query : { tag : 'Analysis', context : dashId, user : targetUser } }" v-t="'timeline.analyze'">Analyze your Data</router-link></div>
                    </div>
                    <div class="col-6 col-md-3 col-lg-2 spacesiconbar" v-for="space in spaces" :key="space._id" style="position:relative">
                        <router-link :to="{ path : './spaces', query : { spaceId : space._id, context : dashId, user : targetUser  }}"><img class="img-responsive" :src="getIconUrl(space)"></router-link>
                        <div class="position:relative">
                            <div style="position:absolute;right:16px;"><a href="javascript:" @click="deleteSpace(space);"><span class="fas fa-times-circle"></span></a></div>
                            <router-link :to="{ path : './spaces', query : { spaceId : space._id } }">{{ space.name}}</router-link>
                        </div>		 
                    </div>
                </div>                                            
            
        </panel>	    
    </div>
</template>
<script>
import Panel from 'components/Panel.vue';
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
        dashId : "me",
        role : null,
	    params : null,
        targetUser : "",
		spaces : [],
		url : null
	}),				

	components : { ErrorBox, Panel },

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
		    spaces.openAppLink($router, $route, $data.userId, { app : view, user : $data.targetUser, context : $data.dashId });
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
        }
    },

    created() {
        const { $data, $route, $router } = this, me = this;
        $data.role = $route.meta.role.toUpperCase();
        if ($route.query.params) $data.params = JSON.parse($route.query.params); else $data.params = null;
        if ($route.query.user) $data.targetUser = $route.query.user;        
        if ($route.meta.dashId) $data.dashId = $route.meta.dashId;
        else if ($route.query.dashId) $data.dashId = $route.query.dashId;        

        session.currentUser
	    .then(function(userId) {
			$data.userId = userId;
			
			me.doBusy(spaces.autoadd().then(function() {
			    me.doBusy(spaces.getSpacesOfUserContext(userId, $data.dashId).then(function(result) {
				    let spaces = [];
				    for (let s of result.data) {
					    if (s.name != "Fitness" && s.name != "Timeline") {
					        spaces.push(s);
					    }
				    }
                    $data.spaces = spaces;
			    }));
			}));
						
	    });
    }
}
</script>