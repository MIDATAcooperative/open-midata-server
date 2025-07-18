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
    <div class="midata-overlay borderless">
	    <div class="overlay-body">
            <panel :title="title" :busy="isBusy">
                <error-box :error="error"></error-box>
                <div id="iframe" style="min-height:200px;width:100%;" v-pluginframe="url"></div>								
            </panel>
        </div>
    </div>
</template>
<script>
import Panel from 'components/Panel.vue';
import { getLocale } from 'services/lang.js';
import spaces from 'services/spaces.js';
import session from 'services/session.js';
import actions from 'services/actions';
import { status, ErrorBox } from 'basic-vue3-components';


export default {

	data: () => ({
		title : "",
		userId : null,
		spaceId : null,
		url : null,
		params : null		
	}),				

	components : { ErrorBox, Panel },

    mixins : [ status ],

    methods : {
        openAppLink(data) {
            const { $data, $route, $router } = this;
		    data = data || {};
		    data.user = $route.query.user;
		    
		    if (data.pos == "return") {
		      actions.doActionsFirst($router, $route, [{ ac : "open", c : data.app }, { ac : "space", c : this.$data.spaceId }]); 
		    } else {		    
		      spaces.openAppLink($router, $route, $data.userId, data);
		    }	 
		},
		
		done() {
		  const { $route, $router } = this;
		  actions.showAction($router, $route);
		},
		
		getAuthToken(space) {
			const { $data, $route } = this;
			
			if (space) {
				this.doBusy(spaces.getUrl(space, $route.query.user)
				.then(function(result) {   
					$data.title = result.data.name;
					$data.url = spaces.mainUrl(result.data, getLocale(), $data.params);	
					
				}));
			}
		},
	
		init() {
			const { $data, $route } = this, me = this;
			$data.spaceId = $route.query.spaceId;		
			$data.params = $route.query.params ? JSON.parse($route.query.params) : null;
			this.doBusy(session.currentUser
			.then(function(userId) {
				$data.userId = userId;
			
				if ($route.query.app && !$route.query.spaceId) {				
					me.openAppLink({ app : $route.query.app });
				} else me.getAuthToken($data.spaceId/*, $state.params.user*/);
			}));
		}
    },

	watch : {		
		$route(to, from) {
		  if (to.path.indexOf("space")>=0) this.init(); 
		}
	},

    created() {
		this.init();
    }
}
</script>