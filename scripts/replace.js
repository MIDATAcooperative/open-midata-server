var fs = require('fs');
var fileName = 'config/instance.json';
var file = require("../"+fileName);

if (process.argv.length==5) {
  file[process.argv[2]][process.argv[3]] = process.argv[4];
} else if (process.argv.length==4) {
  file[process.argv[2]] = process.argv[3];
}

fs.writeFileSync(fileName, JSON.stringify(file, null, 4));