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

					<router-link v-if="$route.query.actions" class="ms-1 navbar-toggler" :class="{'vishidden':$route.meta.locked}" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="go({ name : 'member.user2', query : { userId : user._id, actions : $route.query.actions }})"><span class="fas fa-user"></span></router-link>						
					<button v-else class="ms-1 navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarToggler"
						aria-controls="navbarTogglerDemo02" aria-expanded="false" aria-label="Toggle navigation">
						<span class="fas fa-list"></span>
					</button>
					<a class="navbar-brand" @click="home('./overview');" href="javascript:"> <span class="midata" id="logotype"><img
							src="/images/logo.png" style="height: 36px;"></span>
					</a>
				</div>
				<div class="collapse navbar-collapse navbar-ex1-collapse" id="navbarToggler">

					<ul class="nav navbar-nav me-auto" :class="{'vishidden d-none d-md-flex':locked()}">
					    <li class="d-lg-none nav-item"><div class="mb-3 mt-3 username"><span class="fas fa-user"></span><span v-if="user.testUserApp" class="ms-1 me-1 badge text-bg-warning"><span class="fas fa-vial"></span></span> {{ user.name }}</div></li>					    
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
							@click="go({ name : 'member.overview' })" v-t="'navbar.me'"></a></li>

						
						<li class="nav-item" v-if="beta" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse"
							data-bs-target=".navbar-collapse.show" @click="go({ name : 'member.records' })" v-t="'navbar.my_data'"></a></li>
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse"
							data-bs-target=".navbar-collapse.show" @click="go({ name : 'member.circles' })"><span v-t="'navbar.consents'"></span> <span
								v-if="circles.unconfirmed" class="badge text-bg-info">{{circles.unconfirmed}}</span></a></li>
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
							@click="go({ name : 'member.studies' })" v-t="'navbar.research'"></a></li>
						<li class="nav-item" ui-sref-active="active"><a class="nav-link" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
							@click="go({ name : 'member.apps' })"><span v-t="'navbar.apps'"></span></a></li>
						

						<li class="nav-item dropdown d-none d-lg-block" ui-sref-active="active"><a href="javascript:" class="dropdown-toggle nav-link"
							data-bs-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false"><span class="caret"></span></a>
							<div class="dropdown-menu">
								<a class="dropdown-item" href="javascript:" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
									@click="showApp('fhir-observation');" v-t="'dashboard.observations'"></a> <a class="dropdown-item" href="javascript:"
									data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="showApp('calendar');" v-t="'dashboard.calendar'"></a> <a
									v-for="entry in me_menu" :key="entry._id" class="dropdown-item" href="javascript:" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show"
									@click="showSpace(entry)" v-t="entry.name"></a>

							</div></li>
							
						<li class="nav-item d-lg-none" ui-sref-active="active"><a class="nav-link" href="javascript:" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="showApp('fhir-observation');" v-t="'dashboard.observations'"></a></li>
						<li class="nav-item d-lg-none" ui-sref-active="active"><a class="nav-link" href="javascript:" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="showApp('calendar');" v-t="'dashboard.calendar'"></a></li>
						<li class="nav-item d-lg-none" ui-sref-active="active" v-for="entry in me_menu" :key="entry._id"><a class="nav-link" href="javascript:" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="showSpace(entry)" v-t="entry.name"></a></li>
						<li class="nav-item d-lg-none" ui-sref-active="active"><a v-if="$route.query.actions" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="go({ name : 'member.user2', query : { actions : $route.query.actions }})" class="nav-link"><span class="fas fa-pencil-alt"></span> <span v-t="'navbar.edit_profile'"></span></a>
								<a v-else data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="go({ name : 'member.user', query : { userId : user._id  }})" class="nav-link"><span class="fas fa-pencil-alt"></span> <span v-t="'navbar.edit_profile'"></span></a>
								</li>
						<li class="nav-item d-lg-none" ui-sref-active="active"><a class="nav-link" href="javascript:" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="logout()"><span class="fas fa-power-off"></span> {{ $t('navbar.sign_out') }}</a></li>
					</ul>

					<ul class="d-none d-lg-block nav navbar-nav">
						<li class="nav-item dropdown"><a class="nav-link" href="javascript:" tabindex="0" data-bs-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false"><span v-if="user.testUserApp" class="ms-1 me-1 badge text-bg-warning"><span class="fas fa-vial"></span></span>{{user.name}}</a>
						<div class="dropdown-menu" style="right:0px;left:auto;min-width:200px;">
							<div style="padding:10px;">								
								<div><b>{{ user.firstname }} {{ user.lastname }}</b></div>
								<div>{{ user.email }}</div>
								<span v-if="user.testUserApp" class="ms-1 me-1 badge text-bg-warning"><span class="fas fa-vial"></span> {{ user.testUserCustomer }}</span>
								<hr>
								<div><b style="min-width:40px;display:inline-block;">{{circles.apps}}</b><span v-t="'navbar.app_count'"></span></div>
								<div><b style="min-width:40px;display:inline-block;">{{circles.studies}}</b><span v-t="'navbar.study_count'"></span></div>
								<div class="extraspace"></div>
								<a v-if="$route.query.actions" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="go({ name : 'member.user2', query : { actions : $route.query.actions }})" class="btn btn-sm btn-default"><span class="fas fa-pencil-alt"></span> <span v-t="'navbar.edit_profile'"></span></a>
								<a v-else data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="go({ name : 'member.user', query : { userId : user._id  }})" class="btn btn-sm btn-default"><span class="fas fa-pencil-alt"></span> <span v-t="'navbar.edit_profile'"></span></a>
								&nbsp;
								<a href="javascript:" data-bs-toggle="collapse" data-bs-target=".navbar-collapse.show" @click="logout()" class="btn btn-sm btn-default"><span class="fas fa-power-off"></span> <span v-t="'navbar.sign_out'"></span></a>
       						</div>
						</div>
						</li>
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
<footer id="footer" class="d-none d-md-block">
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
		addBundle("members");
		addBundle("branding");

		session.login($route.meta.role);		
		this.updateNav();
	
	}
}
</script>