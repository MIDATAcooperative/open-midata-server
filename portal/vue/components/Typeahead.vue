<template>
    <div class="dropdown">
        <input type="text" @change = 'mychange' v-validate :value="modelValue" @input="input($event.target.value)"
            @keydown.enter.prevent = 'enter'
            @keydown.down = 'down'
            @keydown.up = 'up'
            @blur = 'blur'
            v-bind="$attrs"
            >
        <ul class="dropdown-menu" :class="{'show':isOpen}">
            <a class="dropdown-item typeahead-item" v-for="(suggestion, idx) in matches" :key="suggestion" :class="{'active': isActive(idx)}" @click="suggestionClick(idx)" href="javascript:">
                {{ suggestion }}
            </a>
        </ul>
    </div>
</template>
<script>

export default {
    inheritAttrs : false,
    props : {
        suggestions : Array,
        field : String,
        modelValue : String
    },
    emits: ['update:modelValue','change','selection'],

    data: () => ({
        open : false,
        selection : "",
        current : 0
    }),

    
    computed : {
        isOpen() {
            const { $data } = this;
            return $data.selection != "" && this.matches.length != 0 && $data.open == true;
        },

        matches() {
            let result = [];
            for (let entry of this.suggestions) {
                let v = this.val(entry);
                if (v.indexOf(this.selection) >= 0) result.push(v);
            }
            return result;
        }
    },

    methods : {
      

        val(v) {
            if (!this.field) return v;
            return v[this.field];
        },

        enter() {
            const { $data } = this;
            $data.selection = this.matches[$data.current];
            this.$emit("update:modelValue", $data.selection);
            $data.open = false;
            this.$emit("change", $data.selection);
            this.$emit("selection", $data.selection);
        },

        
        up() {
            const { $data } = this;
            if ($data.current > 0) $data.current--;
        },
        
        down() {
            const { $data } = this;
            if ($data.current < this.matches.length - 1) $data.current++;
        },
        
        isActive(index) {
            const { $data } = this;
            return index === $data.current;
        },
        
        input(v) {            
            const { $data } = this;
            this.$data.selection = v;
            this.$emit("update:modelValue", v);
            if ($data.open == false) {
                $data.open = true;
                $data.current = 0;
            }
        },
        
        suggestionClick(index) {            
            const { $data } = this;
            $data.selection = this.matches[index];
            this.$emit("update:modelValue", $data.selection);
            $data.open = false;
            this.$emit("change", $data.selection);
            this.$emit("selection", $data.selection);
        },

        blur(evt) {            
            if (evt.relatedTarget && evt.relatedTarget.classList && evt.relatedTarget.classList.contains("typeahead-item")) return;
            const { $data } = this;
            $data.open = false;
            this.$emit("selection", $data.selection);
        }
    },

    mounted() {
        this.$data.selection = this.modelValue;
    }
}
</script>
