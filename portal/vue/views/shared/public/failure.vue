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
            <panel :title="$('failure.title')" style="max-width: 600px; padding-top: 20px; margin: 0 auto;">				
				<p>&nbsp;</p>
				<p v-t="'failure.intro'"></p>
				<p>&nbsp;</p>
				<p>{{ $t('failure.'+reason+'1') }}"></p>
				<p>&nbsp;</p>
				<p>{{ $t('failure.'+reason+'2') }}"></p>
				<p>&nbsp;</p>
				<div class="extraspace"></div>
				<button @click="showLogin()" type="button" class="btn btn-default btn-block" v-t="'failure.loginpage_btn'"></button>
				<div class="extraspace"></div>						  
				
            </panel>			
		</div>
    </div>
  </div>
</template>
<script>
import ENV from "config";

export default {
    data: () => ({
        ENV : {},
        reason : ""
    }),

    methods : {
        showLogin() {		
            const { $router, $route } = this;
		    $router.push({ path : "./oauth2", query : $route.query });
	    }
    },

    created() {
        const { $data, $route } = this;
        $data.ENV = ENV;
        $data.reason = $route.query.reason;
    }
}
</script>