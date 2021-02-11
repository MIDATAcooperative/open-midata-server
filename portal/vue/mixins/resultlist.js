import _ from "lodash";
import { computed, reactive } from "vue";
import Sorter from "components/Sorter.vue"
import Restrict from "components/Filter.vue"
import Pagination from "components/Pagination.vue"

export default {

    components : { Sorter, Restrict, Pagination },
    
    methods : {
        process(result, options) {
            options = options || {};
            let h = Math.floor(document.documentElement.clientHeight / 36)-6;
            if (h<5) h=5;
            if (h>20) h=20;          
            let input = reactive({ 
                load:options.load,
                all:result, 
                filtered:[], 
                sort:options.sort, 
                filter:options.filter,
                total:0,
                pagesize:options.pagesize || h,
                pages:[],
                current:1,
                promise:computed(() => this.fetch(input)),
                filtered:computed(() => this.calc(input))
            });                     
            return input;
        },

        fetch(input) {
            const me = this;
            if (input.load) {
                return input.load(input).then((result) => { input.all = result; } );
            } 
            return null;
        },

        calc(input) {
            let process = input.all;

            if (input.filter) {
                for (let key in input.filter) {                    
                    let v = input.filter[key];
                    if (typeof v === 'function') {
                        process = _.filter(process, v);
                    } else if (v && v!="") {
                        let filterMethod = (t) => (t[key] && t[key].indexOf(v)>=0);
                        process = _.filter(process, filterMethod);
                    }
                }                
            }

            if (input.sort) {
                let field = input.sort;
                let direction = "asc";        
                if (field.startsWith("-")) { field = field.substr(1); direction = "desc"; }
                process = _.orderBy(process, [ field ], [ direction ]);  
            }

            input.total = process.length;

            if (input.pagesize && input.total > input.pagesize) {
                let pages = [];
                let pageCount = Math.floor((input.total-1) / input.pagesize)+1;
                for (let i=1;i<=pageCount;i++) pages.push(i);
                input.pages = pages;
                if (input.current > pageCount) input.current = 1;
                let end = input.current * input.pagesize;
                if (end > input.total) end = input.total+1;
                process = _.slice(process, (input.current-1) * input.pagesize, end);
            } else {
                input.pages = 0;
                input.current = 1;                
            }

            //input.filtered = process;
            console.log(process);
            return process;
        }
    }
}