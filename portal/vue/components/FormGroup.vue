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
   <div class="form-group form-row" :class="{ 'has-feedback': path }">
          <label :for="name" class="col-lg-4 col-form-label midata-label" v-t="label"></label>
          <div class="col-lg-6">
            <slot></slot>
            <div class="invalid-feedback" v-if="path">{{ $t(path) }}</div>
          </div>
    </div>
</template>

<script>
export default {    
  props: ['label', 'name', 'path']
}
</script>