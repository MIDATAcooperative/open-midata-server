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
   <th @click="setSort(sortby)" class="clickable sort" :class="{ 'asc': modelValue==sortby , 'desc':modelValue=='-'+sortby }"><slot/></th>
</template>
<style>
tr .sort::after {
  content: " ";
  position:relative;
  display:inline-block;
  padding-left:5px;
  width:20px;
}

tr .desc::after {
  content: "▾";
  font-family: Arial, sans-serif;
}

tr .asc::after {
  content: "▴";
  font-family: Arial, sans-serif;
} 
</style>
<script>
import _ from "lodash";

export default {    
  props: ['sortby', 'modelValue' ],
  emits: ['update:modelValue'],

  methods : {
      setSort(key) {                      
        if (this.modelValue==key) this.$emit('update:modelValue', "-"+key);
        else { this.$emit('update:modelValue', key); }        
      },

      sort(what,key) {
          let field = key;
          let direction = "asc";        
          if (key.startsWith("-")) { field = key.substr(1); direction = "desc"; }
          let result = _.orderBy(what, [ field ], [ direction ]);         
          return result;
      }
  }
}
</script>