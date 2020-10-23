#
# This file is part of the Open MIDATA Platform.
#
# The Open MIDATA platform is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# any later version.
#
# The Open MIDATA Platform is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
#

include /dev/shm/db.conf

info:
	$(info test)
	
client=../mongodb/bin/mongo
mongoexport=../mongodb/bin/mongoexport
mongoimport=../mongodb/bin/mongoimport

ifndef mongo.user.port
user.db=$(patsubst "%",%,$(mongo.user.host))
else ifeq ($(mongo.user.port),0)
user.db=$(patsubst "%",%,$(mongo.user.host))
else
user.db=$(patsubst "%",%,$(mongo.user.host)):$(mongo.user.port)
endif

ifndef mongo.user.password
user.pw=
else
user.pw=-u $(mongo.user.username) -p $(mongo.user.password)
endif

ifndef mongo.access.port
access.db=$(patsubst "%",%,$(mongo.access.host))
else ifeq ($(mongo.access.port),0)
access.db=$(patsubst "%",%,$(mongo.access.host))
else
access.db=$(patsubst "%",%,$(mongo.access.host)):$(mongo.access.port)
endif

ifndef mongo.access.password
access.pw=
else
access.pw=-u $(mongo.access.username) -p $(mongo.access.password)
endif

ifndef mongo.mapping.port
mapping.db=$(patsubst "%",%,$(mongo.mapping.host))
else ifeq ($(mongo.mapping.port),0)
mapping.db=$(patsubst "%",%,$(mongo.mapping.host))
else
mapping.db=$(patsubst "%",%,$(mongo.mapping.host)):$(mongo.mapping.port)
endif

ifndef mongo.mapping.password
mapping.pw=
else
mapping.pw=-u $(mongo.mapping.username) -p $(mongo.mapping.password)
endif

ifndef mongo.record.port
record.db=$(patsubst "%",%,$(mongo.record.host))
else ifeq ($(mongo.record.port),0)
record.db=$(patsubst "%",%,$(mongo.record.host))
else
record.db=$(patsubst "%",%,$(mongo.record.host)):$(mongo.record.port)
endif

ifndef mongo.record.password
record.pw=
else
record.pw=-u $(mongo.record.username) -p $(mongo.record.password)
endif

/dev/shm/db.conf: 
	@echo "Decrypting configfile..."
	rm -f /dev/shm/secret.conf*
	rm -f /dev/shm/db.conf
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Mantra for config file:" pw;stty echo;printf "\n";printf $$pw;)))	
	cp ../platform/conf/secret.conf.gz.nc /dev/shm/secret.conf.gz.nc
	@cd /dev/shm;/usr/bin/mcrypt /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d -k "$(DECRYPT_PW)"	
	mv /dev/shm/secret.conf /dev/shm/db.conf
	rm -f /dev/shm/secret.conf.gz.nc 

build:	
	@$(client) $(user.pw) --host $(user.db) $(mongo.user.database) mongo-setup-user.js	
	@$(client) $(access.pw) --host $(access.db) $(mongo.access.database) mongo-setup-access.js
	@$(client) $(record.pw) --host $(record.db) $(mongo.record.database) mongo-setup-record.js
	
sharding:
	@$(client) $(mapping.pw) --host $(mapping.db) $(mongo.mapping.database) init-sharding-mapping.js	
	@$(client) $(access.pw) --host $(access.db) $(mongo.access.database) init-sharding-access.js
	@$(client) $(record.pw) --host $(record.db) $(mongo.record.database) init-sharding-record.js

export:
	@$(mongoexport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c formatgroups -o formatgroups.json
	@$(mongoexport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c contentinfo -o contentinfo.json
	@$(mongoexport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c formatinfo -o formatinfo.json
	@$(mongoexport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c coding -o coding.json	
	@rm /dev/shm/db.conf

exportplugins:
	@$(mongoexport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c plugins -o plugins.json -q '{ "status" : { "$$in" : ["BETA","ACTIVE","DEPRECATED"] } }'
	/bin/sed -i '/"_id":/s/"_id":[^,]*,//' plugins.json
	@$(mongoexport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c pluginicons -o pluginicons.json -q '{ "status" : { "$$in" : ["BETA","ACTIVE","DEPRECATED"] } }'
	/bin/sed -i '/"_id":/s/"_id":[^,]*,//' pluginicons.json
	@rm /dev/shm/db.conf
	
reimport:
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c formatgroups --file formatgroups.json --drop --upsertFields _id
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c contentinfo --file contentinfo.json --drop --upsertFields _id
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c formatinfo --file formatinfo.json --drop --upsertFields _id
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c coding --file coding.json --drop --upsertFields _id	 
	
changelog:
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c changelog --type tsv --file ../CHANGELOG.tsv --headerline --upsertFields changeId
		
reimportplugins:
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c plugins --file plugins.json --upsert --upsertFields filename
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c pluginicons --file pluginicons.json --upsert --upsertFields plugin,use
