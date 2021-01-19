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
				<div class="panel-container" style=" padding-top:30px; margin:0 auto;">
					<div class="panel panel-primary">
		            	<div class="panel-heading">
		              		<h3 class="panel-title">{{ title }}</h3>
		            	</div>
		            	<div class="panel-body">		            	    
			              <terms :which="which" @title="title=$event"></terms>
		            	</div>
					</div>
				</div>
			</div>
        </div>			
	</div>  
</template>
<script>
import Terms from "components/Terms.vue";

export default {
    data : ()=>({
        title : "",
        which : ""
    }),

    components : {
       Terms
    },

    created() {
        const { $data, $methods, $route } = this, me = this;
        $data.which = $route.query.which.split("--");	    
  	}
}
</script>