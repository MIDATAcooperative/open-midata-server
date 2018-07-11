include /dev/shm/db.conf

info:
	$(info test)
	
client=../mongodb/bin/mongo
mongoexport=../mongodb/bin/mongoexport
mongoimport=../mongodb/bin/mongoimport

ifndef mongo.user.port
user.db=$(mongo.user.host)
else
user.db=$(patsubst "%",%,$(mongo.user.host)):$(mongo.user.port)
endif

ifndef mongo.user.password
user.pw=
else
user.pw=-u $(mongo.user.username) -p $(mongo.user.password)
endif

ifndef mongo.access.port
access.db=$(mongo.access.host)
else
access.db=$(patsubst "%",%,$(mongo.access.host)):$(mongo.access.port)
endif

ifndef mongo.access.password
access.pw=
else
access.pw=-u $(mongo.access.username) -p $(mongo.access.password)
endif

ifndef mongo.mapping.port
mapping.db=$(mongo.mapping.host)
else
mapping.db=$(patsubst "%",%,$(mongo.mapping.host)):$(mongo.mapping.port)
endif

ifndef mongo.mapping.password
mapping.pw=
else
mapping.pw=-u $(mongo.mapping.username) -p $(mongo.mapping.password)
endif

ifndef mongo.record.port
record.db=$(mongo.record.host)
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
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Password:" pw;stty echo;printf "\n";printf $$pw;)))	
	cp ../platform/conf/secret.conf.gz.nc /dev/shm/secret.conf.gz.nc
	@cd /dev/shm;/usr/bin/mcrypt /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d -k "$(DECRYPT_PW)"	
	mv /dev/shm/secret.conf /dev/shm/db.conf
	rm -f /dev/shm/secret.conf.gz.nc 

build:	
	@$(client) $(user.pw) $(user.db)/$(mongo.user.database) mongo-setup-user.js	
	@$(client) $(access.pw) $(access.db)/$(mongo.access.database) mongo-setup-access.js
	@$(client) $(record.pw) $(record.db)/$(mongo.record.database) mongo-setup-record.js
	
sharding:
	@$(client) $(mapping.pw) $(mapping.db)/$(mongo.mapping.database) init-sharding-mapping.js	
	@$(client) $(access.pw) $(access.db)/$(mongo.access.database) init-sharding-access.js
	@$(client) $(record.pw) $(record.db)/$(mongo.record.database) init-sharding-record.js

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
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c loinc --type csv --file loinc.csv --drop --headerline 
	
reimportplugins:
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c plugins --file plugins.json --upsert --upsertFields filename
	$(mongoimport) -h $(user.db) -d $(mongo.user.database) $(user.pw) -c pluginicons --file pluginicons.json --upsert --upsertFields plugin,use
