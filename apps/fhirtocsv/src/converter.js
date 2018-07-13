var JSONStream = require('JSONStream');
var es = require('event-stream');
var fs = require('fs');
var csvWriter = require('csv-write-stream')


class Converter {
	
	constructor(srcfile, destpath, mapping) {
		this.srcfile = srcfile;
		this.destpath = destpath;
		this.mapping = mapping;		
		if (this.destpath != "" && !this.destpath.endsWith("/")) this.destpath += "/";
	}
	
	prepareMapping() {		
		for (var map of this.mapping) {
			var headers = [];
			for (var f of map.fields) headers.push(f.csv);
			
			for (var p of map.fields) {
				p.path = p.fhir.split(".");				
			}
						
			var writer = csvWriter({ headers: headers });
            writer.pipe(fs.createWriteStream(this.destpath+map.file));            
            map.writer = writer;
		}
	}
	
	endMapping() {
        for (var map of this.mapping) {			
           map.writer.end();
		}
	}
	
	run() {
		var me = this;
		var input = fs.createReadStream(this.srcfile);
		this.prepareMapping();
		
		input.pipe(JSONStream.parse('entry.*.resource'))		
	    .pipe(es.mapSync(function(data) {
	       me.process(data);
	    }))
	    .on("end", function() {			    	
	    	me.endMapping();	    		    	
	    });		
		
	}
	
	process(data) {
		var me = this;
		var type = data.resourceType;
		for (var m=0;m<this.mapping.length;m++) {
			var current = this.mapping[m];
			if (current.filter) {				
			    var match = true;
				for (var key in current.filter) {
					var val = me.extract(data, key.split("."), 0);
					if (current.filter[key] != val) match = false; 
				}
				if (!match) continue;
			}
			if (current.debug && data.extension) console.log(data.extension[0]);
			if (current.forEach && Array.isArray(data[current.forEach])) {
			  var repeats = data[current.forEach];			  
			  for (var item of repeats) {
				  data[current.forEach] = item;
				  me.processMapping(data,current);				  
			  }
			} else {			
			  me.processMapping(data,current);
			}
		}
	}
	
	processMapping(data,map) {
		var out = [];
		for (var field of map.fields) {
			this.all = data;
			this.field = field;
			var value = this.extract(data, field.path, 0);			
			this.all = this.field = null;
			
			if (value == null) value = field.missing || "null";			
			/*if (field.filter) {				
				if (Array.isArray(value)) {
					var value_new = [];
					for (var v of value) {						
						if ((v+"").indexOf(field.filter)>=0) value_new.push(v);
					}
					value = value_new;
				} else if ((value+"").indexOf(field.filter)<0) value = field.missing || "null"; 
			}*/ 
			if (map.onlyFirst && Array.isArray(value)) value = value[0];
			if (Array.isArray(value)) value = value.join(map.separator || " ");
			out.push(value);
		}
		map.writer.write(out);
	}
	
	extract(data,path,idx) {		
		var me = this;
		if (idx < path.length) {
			var dataold = data;
			var setter = function(s) { dataold[path[idx]] = s; };
			data = data[path[idx]];						
			return this.handleArrays(data, function(dat) { return me.extract(dat, path, idx+1); }, setter);			
		} else return me.handleArrays(data, function(dat) { return me.extractFinal(dat); }, setter);
	}
	
	handleArrays(data, func, setter) {
		if (data == null) return null;
		if (Array.isArray(data)) {
			if (data.length == 0) return null;
			if (data.length == 1) return func(data[0]);
			var dat = [];
			for (var i=0;i<data.length;i++) {
				if (setter) setter(data[i]);
				var v = func(data[i]);
				if (v!=null) dat.push(v);
			}
			if (setter) setter(data);
			return dat;				
		} else return func(data);
	}
	
	extractFinal(data) {
		var me = this;
		
		if (this.field && this.field.filter) {
			var current = this.field;
			this.field = null;
			var match = true;
			for (var key in current.filter) {
				var val = me.extract(this.all, key.split("."), 0);
				if (current.filter[key] != val) match = false; 
			}
			this.field = current;
			if (!match) return null;
		}
		
		if (data.coding) {
			var v = me.handleArrays(data.coding, function(dat) { return me.extractFinal(dat); });
			if (v!=null) return v;
		}
		if (data.system && data.code) return data.system+"|"+data.code;
		if (data.system && data.value) return data.system+"|"+data.value;
		if (data.code) return data.code;
		if (data.reference) return data.reference;
		if (data.display) return data.display;
		if (data.text) return data.text;
		if (data.value && data.unit) return data.value+" "+data.unit;
		if (data.value) return data.value;
		return data;
	}
}


exports.run = function(srcfile, destpath, mapping) {
	return new Converter(srcfile, destpath, mapping).run();
};
