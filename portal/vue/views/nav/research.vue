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
<div><div id="maincontent">
	<!-- Navbar -->
	<div class="colored">
		<div id="navbar" class="navbar navbar-expand-lg navbar-light bg-light" role="navigation">			
			<div class="container">
				<div class="navbar-header">
					<button class="ms-1 navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
						aria-controls="navbarTogglerDemo02" aria-expanded="false" aria-label="Toggle navigation">
						<span class="fas fa-list"></span>
					</button>
					<a class="navbar-brand" @click="home('studies');" href="javascript:"> <span class="midata" id="logotype"><img
							src="/images/logo.png" style="height: 36px;"></span>
					</a>
				</div>
				<div class="collapse navbar-collapse navbar-ex1-collapse">

					<ul class="nav navbar-nav me-auto" :class="{'vishidden':locked()}">
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
							@click="go({ path : './studies' })" v-t="'researcher_navbar.studies'"></a></li>
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
							@click="go({ path : './circles' })" v-t="'navbar.consents'"></a></li>
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
							@click="go({ path : './dashboard', query : { dashId : 'workspace' }})" v-t="'researcher_navbar.workspace'"></a></li>
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
							@click="go({ path : './records' })" v-t="'researcher_navbar.data'"></a></li>
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
							@click="go({ path : './organization' })" v-t="'researcher_navbar.organization'"></a></li>
					</ul>

					<ul class="nav navbar-nav">
						<li class="nav-item"><a data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" class="nav-link" @click="go({ name : 'research.user', query : { userId : user._id}})">{{user.name}}</a></li>
						<li class="nav-item"><a data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" class="nav-link" @click="logout()" href="javascript:"> <span class="fas fa-power-off"></span> <span
								v-t="'navbar.sign_out'"></span>
						</a></li>
					</ul>				

				</div>
				<!--/.nav-collapse -->
			</div>
		</div>		
	</div>
	<div class="mainpart">
		<div class="container">
			<router-view></router-view>
		</div>
	</div>
</div>
<footer id="footer">
	<div class="container">
		<p>&nbsp;</p>

		<ul>
			<li><a :href="homepage" v-t="'footer.homepage'"></a></li>			
			<li><a @click="go({ path : './terms', query : {which : 'midata-terms-of-use'} })" v-t="'registration.agb3'">Terms of Use</a></li>
			<li><a @click="go({ path : './terms' ,query : {which : 'midata-privacy-policy'} })" v-t="'registration.privacypolicy3'">Privacy Policy</a></li>

		</ul>
	</div>
</footer></div>
</template>
<script>
import ENV from "config";
import { setLocale, addBundle } from "services/lang.js";
import server from "services/server.js";
import session from "services/session.js";
import spaces from "services/spaces.js";
import circles from "services/circles.js";
import actions from "services/actions.js";

export default {
	
  data: () => ({	
	currentYear : "",        
	user : { subroles:[] },
	circles : { unconfirmed : 0, apps : 0, studies : 0 },
	beta : ENV.beta,
	me_menu : [],
	homepage : ENV.homepage
  }),
      
    methods : {
	    
	    changeLanguage(language) {
		    setLocale(language);
        },
      
        logout() {		
			const { $route } = this;
		    server.post('/api/logout')
		    .then(function() { 
			    session.logout();
		    	if ($route.meta.role=="provider") document.location.href="/#/public_provider/login";
			    else if ($route.meta.role=="research") document.location.href="/#/public_research/login";
			    else if ($route.meta.role=="admin" || $route.meta.role=="developer")  document.location.href="/#/public_developer/login";
			    else document.location.href="/#/public/login"; });
        },
		
	    hasSubRole(subRole) {	
            const { $data } = this;
		    return $data.user.subroles.indexOf(subRole) >= 0;
	    },
	
	    showSpace(space) {
            const { $router } = this;
		    $router.push({ path : './spaces', query : { spaceId : space._id } });
	    },
	
	    showApp(app) {
            const { $data, $router, $route } = this;
		    spaces.openAppLink($router, $route, $data.userId, { app : app });
	    },
	
	    home(page) {
            const { $router, $route } = this;
		    if (!actions.showAction($router, $route)) $router.push({ path : "./"+page });
        },
        
        locked() {
            const { $data, $route } = this;
		    return $route.query.actions != null || actions.hasMore();	
	    },

		updateNav() {
			const { $data } = this, me = this;
			
			$data.circles.unconfirmed = 0;
		
			session.currentUser.then(function(userId) {			
				$data.user = session.user;	
				$data.userId = userId;
			
				spaces.getSpacesOfUserContext($data.userId, "menu")
	    		.then(function(results) {
	    			$data.me_menu = results.data;
	    		});
				
				circles.listConsents({  }, ["type", "status"])
				.then(function(results) {
					var l = results.data.length;
					$data.circles.apps = 0;
					$data.circles.studies = 0;
					$data.circles.unconfirmed = 0;
					for (let i=0;i<l;i++) {
						let c = results.data[i];
						if (c.type == "EXTERNALSERVICE" || c.type == "API") $data.circles.apps++;
						else if (c.type == "STUDYPARTICIPATION") $data.circles.studies++;
						else if (c.status == "UNCONFIRMED") $data.circles.unconfirmed++;
					}								
				});
	    
			});
		},
			  
		go(to) {
		  this.$router.push(to);
		} 

	},

	created() {    
		const { $data, $route } = this;
		$data.currentYear = new Date().getFullYear();
		$data.notPublic = ENV.instanceType == "prod";
		$data.actions = $route.query.actions;
		$data.hideCookieBar = localStorage.hideCookieBar;

		//if (!$route.meta || $route.meta.keep) session.logout();		
		addBundle("researchers");
		addBundle("branding");

		session.login($route.meta.role);		
		this.updateNav();
	
	}
}
</script>