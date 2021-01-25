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
  	<div class="panel-container" :style="style">
		<div class="panel panel-primary">
            <div class="panel-heading" v-if="title">
		    	<h3 class="panel-title">{{ title }} 
                    <a v-if="$attrs.onClose" @click="$emit('close')" href="javascript:" class="close-icon float-right"><span class="fas fa-times"></span></a>
                </h3>                
		    </div>					
			<div class="panel-body">
                <div v-if="busy" class="loader">
                    <div>
                        <img  src="/images/ajax-loader.gif">
                    </div>
                </div>               
                <slot v-else></slot>
            </div>
        </div>
    </div>
</template>

<script>
export default {    
  inheritAttrs : false,
  props: ['style', 'title', 'busy' ]
}
</script>