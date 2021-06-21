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
		<header>

			<div class="cookie-bar" v-if="!hideCookieBar">
				<div class="row align-items-center">
					<div class="col">
						<p class="cell">
							<span v-t="'navbar.cookiebanner'">Diese Website verwendet Cookies. Weitere Informationen finden Sie unter</span> 
							<router-link 
								class="ctcc-more-info-link" tabindex="0" :to="{ path : '/public/terms', query : {which : 'midata-privacy-policy'} }"
								v-t="'navbar.privacypolicy'">Datenschutzerklärung</router-link>.
						</p>
					</div>
					<div class="col-auto">
						<div class="cell">
							<a class="btn btn-primary" href="javascript:" @click="dismissCookieBar()" v-t="'navbar.cookiebanner_close_btn'">Schliessen</a>
						</div>
					</div>
				</div>
			</div>

			<div id="pubnavbar" class="navbar navbar-expand-lg navbar-light bg-light" role="navigation">



				<div class="container">


					<div class="navbar-header">

						<button class="navbar-toggler" type="button" data-toggle="collapse" data-target=".navbar-ex1-collapse"
							aria-controls="navbarTogglerDemo02" aria-expanded="false" aria-label="Toggle navigation">
							<span class="fas fa-list"></span>
						</button>

						<router-link class="navbar-brand" :to="{ name : 'public.info' }"> <span class="midata" id="logotype"><img src="/images/logo.png"
								style="height: 36px;"></span>
						</router-link>
					</div>
					<div class="collapse navbar-collapse navbar-ex1-collapse">
						<ul class="nav navbar-nav mr-auto">
							<li class="nav-item"><router-link class="nav-link" data-toggle="collapse" data-target=".navbar-collapse.show"
								:to="{ path : './login', query : {actions:actions} }" v-t="'navbar.login'"></router-link></li>
							<li class="nav-item"><router-link class="nav-link" data-toggle="collapse" data-target=".navbar-collapse.show" v-if="!notPublic"
								:to="{ path : './registration', query : {actions:actions} }" v-t="'navbar.sign_up'"></router-link></li>
						</ul>
						<ul class="nav navbar-nav">
							<li class="nav-item"><a class="nav-link" data-toggle="collapse" data-target=".navbar-collapse.show"
								@click="changeLanguage('en')" href="javascript:">en</a></li>
							<li class="nav-item"><a class="nav-link" data-toggle="collapse" data-target=".navbar-collapse.show"
								@click="changeLanguage('de')" href="javascript:">de</a></li>
							<li class="nav-item"><a class="nav-link" data-toggle="collapse" data-target=".navbar-collapse.show"
								@click="changeLanguage('fr')" href="javascript:">fr</a></li>
							<li class="nav-item"><a class="nav-link" data-toggle="collapse" data-target=".navbar-collapse.show"
								@click="changeLanguage('it')" href="javascript:">it</a></li>
						</ul>
					</div>
				</div>
			</div>
		</header>
	</div>
	<div style="position: relative; overflow: hidden;">
		<router-view></router-view>
	</div>
</div>
<footer id="footer">
	<div class="container">
		<p>&nbsp;</p>

		<ul>
			<li><a :href="homepage" v-t="'footer.homepage'"></a></li>			
			<li><router-link :to="{ path : './terms', query : {which : 'midata-terms-of-use'} }" v-t="'registration.agb3'">Terms of Use</router-link></li>
			<li><router-link :to="{ path : './terms' ,query : {which : 'midata-privacy-policy'} }" v-t="'registration.privacypolicy3'">Privacy Policy</router-link></li>

		</ul>
	</div>
</footer></div>
</template>
<script>
import ENV from "config";
import { setLocale, addBundle } from "services/lang.js";
import session from "services/session.js"

export default {
	
  data: () => ({
	hideCookieBar : false,
	notPublic : false,
	now : "",
	actions : null,
	homepage : ENV.homepage
  }),
      
  methods : {
	  dismissCookieBar() {
		const { $data } = this;
	    $data.hideCookieBar = localStorage.hideCookieBar = true;
	  },
	  
	  changeLanguage(language) {
		setLocale(language);
	  } 

  },

  created() {    
	 const { $data, $route } = this;
	 $data.now = new Date().getFullYear();
	 $data.notPublic = ENV.instanceType == "prod";
	 $data.actions = $route.query.actions;
	 $data.hideCookieBar = localStorage.hideCookieBar;

	 if (!$route.meta || !$route.meta.keep) {		 	
		session.logout();
	 }

	 addBundle("members");
	 addBundle("branding");
  }
}
</script>