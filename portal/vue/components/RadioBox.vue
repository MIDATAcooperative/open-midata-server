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
  <div class="form-check">
    <input class="form-check-input" type="radio" :id="name+value" :name="name" :value="value" :checked="(modelValue==value)" @input="$emit('update:modelValue', value)" required v-validate v-bind="$attrs">
    <label class="form-check-label" :for="name+value">
      <slot></slot>
    </label>
    <div class="invalid-feedback" v-if="path">{{ $t(path) }}</div>
  </div>
</template>

<script>
export default {    
  inheritAttrs: false,
  props: ['name', 'modelValue', 'path', 'value']
}
</script>