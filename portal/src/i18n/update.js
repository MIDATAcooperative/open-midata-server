fs = require('fs');
jsonminify = require("jsonminify");

var files = ["shared", "admins", "developers", "members", "providers", "researchers"];

function prepare(name, language) {
	return {
		nameSrc : name + '_en.json',
		nameTarget : name + '_' + language + '.json',
		nameRef : name + '_' + language + '.bak',
		src : { line:0, path:[] },
		target : { line:0, path:[] },
		ref : { line:0, path:[] }
	};
}

function load(info) {
	var done = 0;
	fs.readFile(info.nameSrc, 'utf8', function (err,data) {	
	   if (err) {
		   console.log("file: "+info.nameSrc+" error:"+err);
	   }
	   info.src.text = data.split("\n");	 
	   info.src.json = JSON.parse(JSON.minify(data));
	   console.log("loaded: "+info.nameSrc+" #lines="+info.src.text.length);
	   
	   done++;
	   if (done == 3) doprocess(info);
	});
	fs.readFile(info.nameTarget, 'utf8', function(err,data) {
	   if (err) {
		   console.log("file: "+info.nameTarget+" error:"+err);
	   } 		   
	 
	   info.target.text = (data || "").split("\n");
	   info.target.json = JSON.parse(JSON.minify(data || '{ "a" : "b" }'));
	   
	   console.log("loaded: "+info.nameTarget+" #lines="+info.target.text.length);
	   done++;
	   if (done == 3) doprocess(info);
	});
	fs.readFile(info.nameRef, 'utf8', function(err,data) {
	   if (err) {
		   console.log("file: "+info.nameRef+" error:"+err);
	   }
	   info.ref.text = (data || "").split("\n");
	   info.ref.json = JSON.parse(JSON.minify(data || '{ "a" : "b" }'));
	   done++;
	   
	   console.log("loaded: "+info.nameRef+" #lines="+info.ref.text.length);
	   if (done == 3) doprocess(info);
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
	if (!hasvar && trimmed.indexOf("{") > 0) {
	   k = key(trimmed);
	   part.path.push(k);
	   return { type : 1, key : k, full : part.path.join("."), ws : ws(result), comments:[], blanks:0 };
	} else if (!hasvar && trimmed.indexOf("}") == 0) {
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

function esc(str) {
	return str.replace(/"/g,'\\"');
}

 
function doprocess(info) {
	var output = [];
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
		} else if (srcLine.type == 2) {
		  wsIdx-=4;
		  output.push(spaces(wsIdx)+'}'+(srcLine.komma ? "," : ""));		  
		} else if (srcLine.type == 3) {
		  var srcVal = fetch(info.src, srcLine.full);
		  var targetVal = fetch(info.target, srcLine.full);
		  var refVal = fetch(info.ref, srcLine.full);
		 
		  if (!targetVal) {		
			  output.push("");
			  output.push(spaces(wsIdx)+"// TODO NEW: "+srcVal);
			  output.push(spaces(wsIdx)+'"'+srcLine.key+'" : "'+esc(srcVal)+'"'+(srcLine.komma ? "," : ""));
		  } else if (srcVal === refVal) {
			  addcmt(srcLine.full, targetVal != srcVal);
			  output.push(spaces(wsIdx)+'"'+srcLine.key+'" : "'+esc(targetVal)+'"'+(srcLine.komma ? "," : ""));
		  } else {
			  addcmt(srcLine.full);
			  output.push("");
			  output.push(spaces(wsIdx)+"// TODO CHANGED");
			  output.push(spaces(wsIdx)+"// OLD: "+refVal);
			  output.push(spaces(wsIdx)+"// NEW: "+srcVal);
			  output.push(spaces(wsIdx)+'"'+srcLine.key+'" : "'+esc(targetVal)+'"'+(srcLine.komma ? "," : ""));
		  }
		}
		srcLine = nextLine(info.src);	
	} while (srcLine.type !== 0);
	output.push("}");			
	
	fs.writeFileSync(info.nameTarget, output.join("\n"));
	fs.writeFileSync(info.nameRef, info.src.text.join("\n"));
	console.log("done: "+info.nameTarget);
}

var languages = process.argv.slice(2);
console.log(languages);
for (var i=0;i<files.length;i++) {
	for (var j=0;j<languages.length;j++) {
		var p = prepare(files[i], languages[j]);		
		load(p);
	}
}
