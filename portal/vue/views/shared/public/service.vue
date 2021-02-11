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
		if (pluginName) {
			actions.push({ ac : "use", c : pluginName });
		}
		
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
		
		
		if ($route.query.callback) {
			actions.push({ ac : "leave", c : $route.query.callback });
		} else {
			actions.push({ ac : "leave" });
		}
		params.action=JSON.stringify(actions);

		if ($route.query.isnew) {
          $router.push({ path : "./registration", query : params });
		} else {
		  $router.push({ path : "./login", query : params });
		}			
    }
}
</script>