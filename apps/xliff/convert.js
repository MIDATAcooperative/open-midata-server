/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

fs = require('fs');
jsonminify = require("jsonminify");
convert = require("xml-js");
assert = require("assert");

var files = ["shared", "admins", "developers", "members", "providers", "researchers", "branding"];

var xliffout = [];
var res = [];
var allFiles = 0;
var doneFiles = 0;
var xliffin = null;
var sf = null;
var bak = null;

function prepare(name, language) {
	return {
		nameSrc : name + '_en.json',
		nameTarget : name + '_' + language + '.json',
		nameRef : name + '_' + language + '.bak',
		src : { line:0, path:[] },
		target : { line:0, path:[] },
		ref : { line:0, path:[] },
		language : language
	};
}

function load(info, cb) {
	var done = 0;
	fs.readFile(info.nameSrc, 'utf8', function (err,data) {	
	   if (err) {
		   console.log("file: "+info.nameSrc+" error:"+err);
	   }
	   info.src.text = data.split("\n");	 
	   info.src.json = JSON.parse(JSON.minify(data));
	   console.log("loaded: "+info.nameSrc+" #lines="+info.src.text.length);
	   
	   done++;
	   if (done == 3) {
		   res.push(info);
		   cb();
	   }
	});
	fs.readFile(info.nameTarget, 'utf8', function(err,data) {
	   if (err) {
		   console.log("file: "+info.nameTarget+" error:"+err);
	   } 		   
	 
	   info.target.text = (data || "").split("\n");
	   info.target.json = JSON.parse(JSON.minify(data || '{ "a" : "b" }'));
	   
	   console.log("loaded: "+info.nameTarget+" #lines="+info.target.text.length);
	   done++;
	   if (done == 3) {
		   res.push(info);
		   cb();
	   }
	});
	fs.readFile(info.nameRef, 'utf8', function(err,data) {
	   if (err) {
		   console.log("file: "+info.nameRef+" error:"+err);
	   }
	   info.ref.text = (data || "").split("\n");
	   info.ref.json = JSON.parse(JSON.minify(data || '{ "a" : "b" }'));
	   done++;
	   
	   console.log("loaded: "+info.nameRef+" #lines="+info.ref.text.length);
	   if (done == 3) {
		   res.push(info);
		   cb();
	   }
	});
}

function key(line) {
	return /\"([^\"]*)\"/.exec(line)[1];
}

function ws(line) {
	return /\S/.exec(line).index;
}

function spaces(cnt) {
	return "                                                                                        ".substring(0,cnt);
}

function endsWith(s1, s2) {
	var position = s1.length - s2.length;    
    var lastIndex = s1.indexOf(s2, position);
    return lastIndex !== -1 && lastIndex === position;
}

function nextLine(part) {	
	if (part.line >= part.text.length) return { type : 0};
	var result = part.text[part.line];
	part.line++;
	var trimmed = result.trim();
	var res;
	if (trimmed === "" || trimmed == "{") {
		res = nextLine(part);
		res.blanks++;
		return res;
	} else if (trimmed.indexOf("//") === 0) {
		res = nextLine(part);
		res.comments.splice(0, 0, trimmed);
		return res;
	}
	var hasvar = trimmed.indexOf("{{") > 0;
	var k;
	if (!hasvar && trimmed.indexOf("{") >= trimmed.length - 1) {
	   k = key(trimmed);
	   part.path.push(k);
	   return { type : 1, key : k, full : part.path.join("."), ws : ws(result), comments:[], blanks:0 };
	} else if (!hasvar && trimmed.indexOf("}") === 0) {
	   if (part.path.length === 0) return { type : 0 }; 
	   k = part.path.pop();
	   return { type : 2, key : k, ws : ws(result), komma : trimmed.indexOf(",") > 0, comments:[], blanks:0 };
	} else {
	   k = key(trimmed);
	   part.path.push(k);
	   var ret = { type : 3, key : k, full : part.path.join("."), ws : ws(result), komma : endsWith(trimmed, ","), comments:[], blanks:0 };
	   part.path.pop();
	   return ret;
	}
}

function fetch(part, path) {
  var p = path.split(".");
  var r = part.json;
  for (var i=0;i<p.length;i++) {
	  if (r===undefined) return undefined;
	  r = r[p[i]];
  }
  return r;
}

function esc(str, line) {
	if (!str) {
		console.log("Error in line: "+line);
	}
	return str.replace(/"/g,'\\"');
}

 
function doprocess(info) {
	var output = [];
	var xliff = [];
	var stack = [];
	var comments = {};
	var wsIdx = 4;
	var tgLine = nextLine(info.target);
	do {
		if (tgLine.full && tgLine.comments.length > 0) comments[tgLine.full] = tgLine.comments;
		tgLine = nextLine(info.target);
	} while (tgLine.type !== 0);
	var addcmt = function(full, removenewtodo) {
		if (comments[full] && comments[full].length>0) {
			output.push("");
			for (var i=0;i<comments[full].length;i++) { 
				if (removenewtodo && comments[full][i].indexOf("TODO NEW")>=0) {}
				else { output.push(spaces(wsIdx)+comments[full][i]); } 
			}
		}
	};
	
	var srcLine = nextLine(info.src);		
	//var targetLine = nextLine(info.target);
	output.push("{");
	do {
		for (var i=0;i<srcLine.blanks;i++) output.push("");
		if (srcLine.type == 1) {
		  output.push(spaces(wsIdx)+'"'+srcLine.key+'" : {');
		  wsIdx+=4;
		  xliff.push(xliffgroup(srcLine.key, [], srcLine.full));
		  stack.push(xliff);
		  xliff = [];
		} else if (srcLine.type == 2) {
		  wsIdx-=4;
		  output.push(spaces(wsIdx)+'}'+(srcLine.komma ? "," : ""));
		  var oldxliff = xliff;
		  xliff = stack.pop();
		  if (oldxliff.length > 0) {
			  xliff[xliff.length-1].elements = oldxliff;
		  } else {
			  xliff.pop();
		  }
		} else if (srcLine.type == 3) {		  
		  var srcVal = fetch(info.src, srcLine.full);
		  var targetVal = fetch(info.target, srcLine.full);
		  var refVal = fetch(info.ref, srcLine.full);		  		  
		  
		  if (!targetVal) {		
			  output.push("");
			  output.push(spaces(wsIdx)+"// TODO NEW: "+srcVal);
			  output.push(spaces(wsIdx)+'"'+srcLine.key+'" : "'+esc(srcVal, srcLine.full)+'"'+(srcLine.komma ? "," : ""));
			  xliff.push(xliffentry(srcLine.key, srcVal, null, srcLine.full));
		  } else if (srcVal === refVal) {
			  addcmt(srcLine.full, targetVal != srcVal);
			  if (targetVal == srcVal) xliff.push(xliffentry(srcLine.key, srcVal, null, srcLine.full));
			  output.push(spaces(wsIdx)+'"'+srcLine.key+'" : "'+esc(targetVal, srcLine.full)+'"'+(srcLine.komma ? "," : ""));
		  } else {
			  addcmt(srcLine.full);
			  output.push("");
			  output.push(spaces(wsIdx)+"// TODO CHANGED");
			  output.push(spaces(wsIdx)+"// OLD: "+refVal);
			  output.push(spaces(wsIdx)+"// NEW: "+srcVal);
			  output.push(spaces(wsIdx)+'"'+srcLine.key+'" : "'+esc(targetVal, srcLine.full)+'"'+(srcLine.komma ? "," : ""));
			  xliff.push(xliffentry(srcLine.key, srcVal, targetVal, srcLine.full));
		  }
		}
		srcLine = nextLine(info.src);	
	} while (srcLine.type !== 0);
	output.push("}");				
	info.output = output.join("\n");
	
	xliffout.push(xlifffile(info.language, info.nameTarget, xliff));
	console.log("done: "+info.nameTarget);
	doneFiles++;
	/*if (allFiles == doneFiles) {
		saveFiles();		
	}*/
}


function saveFiles(info) {
	for (var i=0;i<res.length;i++) {
		var info = res[i];
		fs.writeFileSync(info.nameTarget, info.output);
		fs.writeFileSync(info.nameRef, info.src.text.join("\n"));
	}	
	fs.writeFileSync("xliff-out.xliff", createxliff(xliffout));
}




function xliffgroup(id,contents, path) {
	return {
		name : "group",
		type : "element",
		attributes : { resname : id, id : path },
		elements : contents
	};
}

function xliffentry(id, source, target, path) {
	var result = {
		name : "trans-unit",
		type : "element",
		attributes : { resname : id, id : path },
		elements : [
			{
				name : "source",
				type : "element",
				elements : [
					{ type : "text", text : source }
				]
			}
		]	    
	};
	if (target) result.elements.push(
		{
	       name : "target",
	       type : "element",
	       elements : [
	    	   { type : "text", text : target }
	       ]
		}
	);
	return result;
}

function xlifffile(lang,filename,content) {
  return {
	  name : "file",
	  type : "element",
	  attributes : {
		  "source-language" : "en",
		  "target-language" : lang,
		  "datatype" : "winres",
		  "original" : filename
	  },
	  elements : [
		  { name : "body",
			type : "element",
			elements : content
		  }		 
	 ]	  
  };
}

function findelem(parent, name) {
	if (!parent.elements) return null;
	for (var i=0;i<parent.elements.length;i++) {
		if (parent.elements[i].name == name) return parent.elements[i];
	}
	return null;
}

function workxliff(json, elements, path) {
	assert(json, "No JSON at path: "+path);
	for (var i=0;i<elements.length;i++) {
		var elem = elements[i];
		if (elem.name == "group") {
			var id = elem.attributes.resname; // TODO Maybe change "resname" to "id" ?
			if (!json[id]) json[id] = {};
			workxliff(json[id], elem.elements, path + id + ".");
		} else if (elem.name == "trans-unit") {
			var id = elem.attributes.resname;
			var target = findelem(elem, "target");
			if (target && target.elements && target.elements.length) {
				var txt = target.elements[0].text;
				console.log("xliff: "+path+id+"="+txt);
				json[id] = txt;
			}
		}
	}
}

function parsexliff(xliff) {
	var data = JSON.parse(convert.xml2json(xliff));	
	var fileelements = data.elements[0].elements;
	for (var i=0;i<fileelements.length;i++) {
	  var fileelement = fileelements[i];
	  assert(fileelement.name == "file", "No xliff file element");
	  
	  var fileName = fileelement.attributes.original;
	  var file = null;
	  for (var i2=0;i2<res.length;i2++) { 
		  if (res[i2].nameTarget == fileName) { file = res[i2]; }
	  }
	  assert(file, "Matching file not found:"+fileName);
	  workxliff(file.target.json, fileelement.elements[0].elements, "");
	}
}

function createxliff(files) {
		
	var xl = {
		"declaration" : {
			"attributes" : {
				"version" : "1.0",
				"encoding" : "utf-8" 
			}
		},
		"elements" : [
			{ name : "xliff",
			  type : "element",
			  attributes : { "version" : "1.1", "xml:lang" : "en" },
			  elements : files
			}
		]
	};
	return convert.json2xml(xl, {compact: false, spaces: 4});
}

function loadFiles(languages) {
	for (var i=0;i<files.length;i++) {
		for (var j=0;j<languages.length;j++) {
			var p = prepare(files[i], languages[j]);			
			allFiles++;
			load(p, processfiles);
		}
	}
};

function filereplace(text, lang, replacement) { 	
	   var result = [];
	   var initial = -1;
	   var end = -1;
	   for (var line = 0;line < text.length; line++) {
		   var str = text[line].trim();
		   if (str.startsWith("//$START_LANGUAGE") && str.endsWith(lang)) {
			   initial = line+1;				   
		   } else if (str.startsWith("//$END_LANGUAGE") && str.endsWith(lang)) {
			   end = line;				   
		   } 
	   }
	   if (initial >= 0 && end >= 0 && end > initial) {
		   result = text.slice(initial, end);
		   if (replacement) {
			   console.log("text replace from="+initial+" rem-lines="+(end-initial)+" insert:"+replacement.length);
			   text.splice(initial, (end-initial), replacement);
		   }
	   }	
	   return result;
}

function loadSingle(filename, languages) {
	

	var process = function() {
		if (sf && bak) {
			for (var j=0;j<languages.length;j++) {
				var lang = languages[j];
				var info = prepare(filename, languages[j]);			
				info.src.text = filereplace(sf, "en");
				info.src.json = JSON.parse(JSON.minify(info.src.text.join("\n")));
				
				info.target.text = filereplace(sf, lang);
				info.target.json = JSON.parse(JSON.minify(info.target.text.join("\n")));
				
				info.ref.text = filereplace(bak, lang) || '{ "a" : "b" }';
				
				info.ref.json = JSON.parse(JSON.minify(info.ref.text.join("\n") || '{ "a" : "b" }'));
											
				res.push(info);
				allFiles++;
			}
			
			if (bak.length == 0) bak = sf.slice();
			
			processfiles();
			
		}
	}
	
	fs.readFile(filename, 'utf8', function (err,data) {	
		   if (err) {
			   console.log("file: "+filename+" error:"+err);
		   }
		   sf = data.split("\n");
		   console.log("loaded: "+filename+" #lines="+sf.length);
		   process();
	});
	fs.readFile(filename+".bak", 'utf8', function (err,data) {	
		   if (err) {
			   console.log("file: "+filename+" error:"+err);
		   }
		   if (!data) bak = [];
		   else bak = data.split("\n");
		   
		   console.log("loaded: "+filename+".bak #lines="+bak.length);
		   
		   process();
	});
	
 
}

function saveSingleFile() {
	for (var i=0;i<res.length;i++) {
		var info = res[i];
		filereplace(sf, info.language, info.output);
		filereplace(bak, info.language, info.src.text.join("\n"));
	}
	
	fs.writeFileSync(singleFileName, sf.join("\n"));
	console.log("Written "+singleFileName+" #lines="+sf.length);
	fs.writeFileSync(singleFileName+".bak", bak.join("\n"));	
	console.log("Written "+singleFileName+".bak #lines="+bak.length);
	fs.writeFileSync("xliff-out.xliff", createxliff(xliffout));
	console.log("Written xliff-out.xliff");
}

function processfiles() {
	if (res.length < allFiles) return;
	
	if (xliffin) parsexliff(xliffin);
	
	for (var i=0;i<res.length;i++) {
		doprocess(res[i]);
	}
	
	if (singleFileName) {
	  saveSingleFile();
	} else {
	  saveFiles();
	}
	
}

function loadxliff(xlname, next) {
	if (!xlname) next();
	else fs.readFile(xlname, 'utf8', function (err,data) {	
		   if (err) {
			   console.log("file: "+xlname+" error:"+err);
		   }
		   xliffin = data;
		   next();  
	});
}

var params = process.argv.slice(2);

if (params.length == 0) {
	console.log("Usage:");
	console.log("node convert.js [<i18n.js>] [<xliff-in.xliff>] <lang>");
	console.log("<i18n.js> : (optional) name of JS file containing i18n resources.");
	console.log("<xliff-in.xliff> : (optional) name of xliff file containg changes to merge. ");
	console.log("<lang> : language to process");
	process.exit();
} 

var singleFileName = null;
if (params[0].endsWith(".js")) {
	singleFileName = params[0];
	params = params.slice(1);
}
var languages = params;

var loader = function() { loadFiles(languages); };
if (singleFileName) loader = function() { loadSingle(singleFileName, languages); };

if (params[0].endsWith(".xliff")) {	
	languages = params.slice(1);
	loadxliff(params[0], loader);
} else loader();

