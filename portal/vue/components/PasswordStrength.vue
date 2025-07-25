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
    <div class="small mt-1 ms-1">
      <div v-if="!advanced" :class="liclass(pwlen(8))"><i :class="icon(pwlen(8))"></i>{{ $t("pwstrength.min") }}</div>
      <div v-if="advanced" :class="liclass(pwlen(12))"><i :class="icon(pwlen(12))"></i>{{ $t("pwstrength.min_advanced") }}</div>
      <div :class="liclass(letter())"><i :class="icon(letter())"></i>{{ $t("pwstrength.letter")}}</div>
      <div :class="liclass(num())"><i :class="icon(num())"></i>{{ $t("pwstrength.number")}}</div>
      <div v-if="advanced" :class="liclass(special())"><i :class="icon(special())"></i>{{ $t("pwstrength.special")}}</div>
    </div>
</template>
<script>

export default {
    inheritAttrs : false,
    props : {
        password : String,
        advanced : Boolean
    },
    
    methods : {
        liclass(ok) {
            return ok ? "text-success" : "text-danger";
        },
        
        icon(ok) {
            return ok ? "fas fa-check me-1" : "fas fa-times me-1";
        },
        
        pwlen(l) {
            return this.password != null && this.password.length > l;
        },
        
        letter() {
            return this.password != null && /[a-zA-Z]/.test(this.password);            
        },
        
        num() {
           return this.password != null && /[0-9]/.test(this.password);
        },
        
        special() {
            return this.password != null && /[^0-9a-zA-Z]/.test(this.password);
        }
    }
}
</script>
