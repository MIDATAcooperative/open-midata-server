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
    <nav class="nav" aria-label="Page navigation" v-if="modelValue.all && modelValue.all.length > modelValue.pagesize">
        <ul class="pagination mr-auto">
            <li class="page-item" :class="{ disabled : modelValue.current <= 1 }">
                <a class="page-link"  href="javascript:" @click="prev()" aria-label="Previous">
                    <span aria-hidden="true">&laquo;</span>
                </a>
            </li>
            <li v-for="page in modelValue.pages" :key="page" class="page-item" :class="{ active : modelValue.current == page }"><a class="page-link" @click="setPage(page)" href="javascript:">{{ page }}</a></li>    
            <li class="page-item" :class="{ disabled : modelValue.current >= modelValue.pages.length }">
                <a class="page-link"  href="javascript:" @click="next()" aria-label="Next">
                    <span aria-hidden="true">&raquo;</span>
                </a>
            </li>
        </ul>
        <div v-if="search" class="col-sm-4 col-xs-2 col-lg-3">
            <div class="input-group">
                <input :placeholder="$t(label || 'common.search_btn')" type="text" :value="modelValue.filter[search]" @input="modelValue.filter[search]=$event.target.value" class="form-control py-2 border-right-0 border">
                <span class="input-group-append">
                    <div class="input-group-text bg-transparent"><i class="fa fa-search"></i></div>
                </span>
            </div>
        </div>
         
    </nav>
</template>

<script>

export default {    
  props: ['modelValue', 'search', 'label' ],
  emits: ['update:modelValue'],

  methods : {
      setPage(key) {                      
        this.modelValue.current = key;       
      },

      next() {
          this.modelValue.current++;
      },

      prev() {
          this.modelValue.current--;
      }
  }
}
</script>