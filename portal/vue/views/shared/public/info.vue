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
    <div class="welcome">	 
		<div id="mainheader"><div class="container"><div class="row">
			<!-- Login -->
			<div class="col-sm-12" style="margin-top:70px;margin-bottom:70px;font-family:Sans-serif;color:white">


              <h1 style="color:white;" v-t="'member_login.teaser'"></h1>

			
			</div>
			</div></div>
		</div><div class="container"><div class="row">
			<div class="col-sm-12">
				<div class="panel-container" style=" padding-top:30px; margin:0 auto;">
					<div class="panel panel-primary">
		            	<div class="panel-heading">
		              		<h3 class="panel-title" translate="instance.{{ENV.instance}}.name"></h3>
		            	</div>
		            	<div class="panel-body">	
		            	<div class="row">		     
			<router-link class="col-sm-6 col-lg-3" :to="{ name : 'public.login' }"><img class="img-responsive" src="/images/account.jpg"><span v-t="'footer.for_members'"></span></router-link>
			<router-link class="col-sm-6 col-lg-3" :to="{ name : 'public_research.login' }"><img class="img-responsive" src="/images/research2.jpeg"><span v-t="'footer.for_researchers'"></span></router-link>
			<router-link class="col-sm-6 col-lg-3" :to="{ name : 'public_provider.login' }"><img class="img-responsive" src="/images/doctor.jpeg"><span v-t="'footer.for_providers'"></span></router-link>
			<router-link class="col-sm-6 col-lg-3" :to="{ name : 'public_developer.login' }"><img class="img-responsive" src="/images/developer.jpeg"><span v-t="'footer.for_developers'"></span></router-link>
			 </div>
			 <hr>	            	    
			            	<p v-t="'instance.'+ENV.instance+'.intro'"></p>			            				            	
			            	<strong v-t="'instance.purpose'"></strong>
			            	<p v-t="'instance.'+ENV.instance+'.purpose'"></p>
			            	<strong v-t="'instance.access'"></strong>
			            	<p v-t="'instance.'+ENV.instance+'.access'"></p>
			            	<strong v-t="'instance.uptime'"></strong>
			            	<p v-t="'instance.'+ENV.instance+'.uptime'"></p>
			            	<strong v-t="'instance.data'"></strong>
			            	<p v-t="'instance.'+ENV.instance+'.data'"></p>
			            	
			            	<div class="alert alert-info">
			            	  <span class="fas fa-arrow-up"></span> <span v-t="'instance.start'"></span>
			            	</div>
			            	
			            		
		            	</div>
					</div>
				</div>
			</div>
		
			
		</div>
	  </div>
	 
	</div>		  
</template>
<script>
import ENV from "config";

export default {
    data: () => ({
        ENV : {}
    }),

    created() {
        const { $data } = this;
        $data.ENV = ENV;
    }
}
</script>