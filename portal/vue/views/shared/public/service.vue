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
    <div></div>
</template>
<script>
export default {
    created() {
        const { $route, $router} = this;
        		
		let actions = [];
		let params = {};
		
		let copy = ["login","family","given","country","language","birthdate"];
		for (let i=0;i<copy.length;i++)
		if ($route.query[copy[i]]) {
			params[copy[i]] = $route.query[copy[i]];
		}
        
        let pluginName = $route.query.pluginName || $route.params.pluginName;
		if ($route.meta.account) {
            actions.push({ ac : "account"});
		} else if (pluginName) {
			actions.push({ ac : "use", c : pluginName });
		}		

		if (!$route.meta.account) {
			if ($route.query.consent) {
				actions.push({ ac : "confirm", c : $route.query.consent });
			} else if ($route.query.project) {
				var prjs = $route.query.project.split(",");
				for (var j=0;j<prjs.length;j++) {
					actions.push({ ac : "study", s : prjs[j] });		
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

		if ($route.query.isnew) {
          $router.push({ path : base+"/registration", query : params });
		} else {
		  $router.push({ path : base+"/login", query : params });
		}			
    }
}
</script>