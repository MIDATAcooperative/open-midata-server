##
# This file is part of the Open MIDATA Server.
#
# The Open MIDATA Server is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# any later version.
#
# The Open MIDATA Server is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
#

include conf/*.conf

.PHONY: info
info:
	$(info -----------------------------------)
	$(info Welcome to the Open MIDATA Server)
	$(info -----------------------------------)
	$(info   )
	$(info config : Configure a localhost instance (run pre installation))	
	$(info install : Install a localhost instance)
	$(info   )
	$(info configure-connection : Reconfigure database connection)
	$(info   )	
	$(info update : Update and start current instance)
	$(info start : Start current instance)
	$(info stop : Stop current instance)
	$(info   )	
	$(info lock : Lock current instance (exclude from load balancer) )
	$(info unlock : Unlock current instance)

space := $(null) #
komma := ,
join-with = $(subst $(space),$1,$(strip $2))

install-from-servertools: lock tasks/install-packages tasks/config-firejail tasks/install-node tasks/bugfixes tasks/install-dbtools $(CERTIFICATE_DIR)/dhparams.pem /etc/ssl/certs/ssl-cert-snakeoil.pem tasks/precompile
	touch switches/use-hotdeploy
	touch tasks/check-config	

install: tasks/install-packages tasks/check-plugins tasks/config-firejail tasks/install-node tasks/bugfixes tasks/prepare-local tasks/check-config $(CERTIFICATE_DIR)/selfsign.crt $(CERTIFICATE_DIR)/dhparams.pem $(CERTIFICATE_DIR)/clientca.pem tasks/install-localmongo platform/conf/secret.conf.gz.nc tasks/precompile 
	touch switches/local-mongo
	$(info Please run "make update" to build)
	touch switches/local-mongo

.PHONY: config
config:
	cp -n conf/setup.conf.template conf/setup.conf
	nano conf/setup.conf
	nano conf/pathes.conf
	touch tasks/check-config
	
.PHONY: pull
pull:
	git pull

.PHONY: start
start: /var/log/midata/application.log /dev/shm/secret.conf start1 clear-secrets

start1: 
	if [ -e switches/use-hotdeploy ]; then sh ./hotdeploy.sh; fi;
	if [ -e switches/use-run ]; then sudo service nginx start; fi;
	if [ -e switches/use-run ]; then mkdir -p locks;cd platform;./sbt run -Dpidfile.path=/dev/shm/play.pid -J-Xverify:none -Dconfig.file=/dev/shm/secret.conf -Dhttps.port=9000 -Dhttp.port=9001; fi;

stop-mongo:
	@echo 'Shutting down MongoDB...'
	if [ -e switches/local-mongo ]; then pkill mongod; fi

update: tasks/check-config tasks/check-plugins tasks/install-packages tasks/install-node tasks/config-firejail start-mongo tasks/build-mongodb tasks/build-portal tasks/build-platform conf/config.js tasks/setup-nginx start

test: tasks/build-portal tasks/build-platform
	
.PHONY: stop
stop: stop-platform stop-mongo

stop-platform:
	$(info ------------------------------)
	$(info Locking and stopping platform)
	$(info ------------------------------)
	touch locks/lock
	-pkill -f sbt
	-pkill -f java
	

.PHONY: lock
lock:
	$(info ------------------------------)
	$(info Locking platform)
	$(info ------------------------------)
	mkdir -p locks
	touch locks/lock
	
.PHONY: unlock
unlock:
	$(info ------------------------------)
	$(info Unlocking platform)
	$(info ------------------------------)
	rm locks/lock

.PHONY: reconfig
reconfig:
	rm -f tasks/check-config

.PHONY: start-mongo
start-mongo:
	@echo Check that MongoDB is available
	test=`pgrep mongo`; if [ -e switches/local-mongo -a -z "$$test" ]; then mongodb/bin/mongod --config mongodb/mongod.conf; fi 

$(CERTIFICATE_DIR)/dhparams.pem:
	$(info ------------------------------)
	$(info Generating DH Params file)
	$(info ------------------------------)
	mkdir -p $(CERTIFICATE_DIR)
	openssl dhparam -out $(CERTIFICATE_DIR)/dhparams.pem 2048	

tasks/check-plugins: $(PLUGINS_DIR)/plugins $(PLUGINS_DIR)/staging $(PLUGINS_DIR)/web $(PLUGINS_DIR)/scripts visualizations

$(PLUGINS_DIR)/plugins:
	mkdir -p $(PLUGINS_DIR)/plugins

$(PLUGINS_DIR)/staging:
	mkdir -p $(PLUGINS_DIR)/staging	

$(PLUGINS_DIR)/web:
	mkdir -p $(PLUGINS_DIR)/web
	cp -r $(PLUGINS_DIR)/plugins/* $(PLUGINS_DIR)/web
	chmod -R 755 $(PLUGINS_DIR)/web		

$(PLUGINS_DIR)/scripts:
	mkdir -p $(PLUGINS_DIR)/scripts
	cp -r $(PLUGINS_DIR)/plugins/* $(PLUGINS_DIR)/scripts

visualizations:
	ln -s $(PLUGINS_DIR) visualizations 		

tasks/prepare-local:
	touch switches/use-run
	touch switches/local-mongo
	cp -n conf/setup.conf.template conf/setup.conf		
	touch tasks/prepare-local
		
tasks/install-packages: trigger/install-packages
	$(info ------------------------------)
	$(info Installing Packages... )
	$(info ------------------------------)
	sudo apt-get install git curl openssl openjdk-21-jdk mcrypt unzip ruby-sass software-properties-common clamav-daemon firejail
	sudo apt-get install nginx
	sudo service clamav-daemon stop
	sudo service clamav-freshclam stop
	sudo cp config/clamd.conf /etc/clamav/clamd.conf
	sudo chmod ugo-wx /etc/clamav/clamd.conf
	sudo freshclam
	sudo service clamav-daemon start
	sudo service clamav-freshclam start	
	touch tasks/install-packages
	
tasks/install-node: tasks/install-packages trigger/install-node
	$(info ------------------------------)
	$(info Installing Node JS... )
	$(info ------------------------------)
	curl -sL https://deb.nodesource.com/setup_22.x | sudo -E bash -
	sudo apt-get install -y nodejs	
	sudo chmod -R ugo+rx /usr/lib/node_modules
	touch tasks/install-node

tasks/install-firejail:
	$(info ------------------------------)
	$(info Install Firejail )
	$(info ------------------------------)
	sudo add-apt-repository ppa:deki/firejail
	sudo apt-get update
	sudo apt-get install firejail
	touch tasks/install-firejail

tasks/config-firejail: tasks/install-firejail config/firejail-node.profile config/firejail-npm.profile
	$(info ------------------------------)
	$(info Configure Firejail )
	$(info ------------------------------)
	mkdir -p ~/.config/firejail
	cp config/firejail-node.profile ~/.config/firejail/node.profile
	cp config/firejail-npm.profile ~/.config/firejail/npm.profile
	touch tasks/config-firejail	

tasks/check-config: trigger/check-config
	nano conf/setup.conf
	nano conf/pathes.conf
	touch tasks/check-config

tasks/precompile:
	$(info ------------------------------)
	$(info Preparing Play Framework)
	$(info ------------------------------)
	cd platform;./sbt compile;	
	touch tasks/precompile

tasks/install-localmongo: trigger/install-localmongo
	$(info ------------------------------)
	$(info Installing Local Version of MongoDB... )
	$(info ------------------------------)
	wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-$(MONGO_VERSION).tgz
	tar xzf mongodb-linux-x86_64-$(MONGO_VERSION).tgz
	ln -s mongodb-linux-x86_64-$(MONGO_VERSION) mongodb
	mkdir -p mongodb/data	
	mkdir -p logs
	cp config/mongod.conf mongodb/mongod.conf
	sed -i 's|MONGODB_DATA_PATH|$(abspath mongodb/data)|' mongodb/mongod.conf
	sed -i 's|MONGODB_LOG_PATH|$(abspath logs/mongod.log)|' mongodb/mongod.conf
	rm mongodb-linux-x86_64-$(MONGO_VERSION).tgz			
	touch tasks/install-localmongo

tasks/install-dbtools: 
	$(info ------------------------------)
	$(info Installing Database Tools... )
	$(info ------------------------------)
	wget -O dbtools.tgz https://fastdl.mongodb.org/tools/db/mongodb-database-tools-ubuntu2204-x86_64-100.10.0.tgz
	wget -O mshell.tgz https://downloads.mongodb.com/compass/mongosh-2.3.3-linux-x64.tgz
	mkdir -p mongodb
	tar xzf dbtools.tgz -C mongodb --strip-components 1
	tar xzf mshell.tgz -C mongodb --strip-components 1
	rm mshell.tgz
	rm dbtools.tgz
	touch tasks/install-dbtools

update-mongodb:
	wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-$(MONGO_VERSION).tgz
	tar xzf mongodb-linux-x86_64-$(MONGO_VERSION).tgz
	mv mongodb/mongod.conf mongodb-linux-x86_64-$(MONGO_VERSION)
	mv mongodb/data mongodb-linux-x86_64-$(MONGO_VERSION)/data	
	ln -sfT mongodb-linux-x86_64-$(MONGO_VERSION) mongodb	
	rm mongodb-linux-x86_64-$(MONGO_VERSION).tgz
	
platform/conf/secret.conf.gz.nc:		
	$(info ------------------------------)
	$(info Generating new encrypted config file.... )
	$(info ------------------------------)
	$(info You will need a strong mantra to encrypt and decrypt the configuration file. )
	rm -f /dev/shm/secret.conf*
	cp platform/conf/secret.conf.template /dev/shm/secret.conf
	sed -i '/play.http.secret.key/d' /dev/shm/secret.conf
	cd platform;./sbt compile;
	NEWSECRET=`cd platform;./sbt playGenerateSecret | grep 'new secret:' | sed 's/^.*: //' | sed 's/[^[:print:]]//'` ; echo "play.http.secret.key=\"$$NEWSECRET\"" >> /dev/shm/secret.conf
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Mantra for config file:" pw;stty echo;printf "\n";printf $$pw;)))
	@/usr/bin/mcrypt /dev/shm/secret.conf -z -a rijndael-128 -m cbc -k "$(DECRYPT_PW)"
	cp /dev/shm/secret.conf.gz.nc platform/conf/secret.conf.gz.nc
	/usr/bin/shred -zun 0 /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc

tasks/remove-secret:
	rm -f /dev/shm/secret.conf*
	rm -f /dev/shm/db.conf

tasks/reencrypt-secret:
	$(info ------------------------------)
	$(info Encrypting configuration file.... )
	$(info ------------------------------)
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Mantra for config file:" pw;stty echo;printf "\n";printf $$pw;)))
	mv /dev/shm/db.conf /dev/shm/secret.conf
	@/usr/bin/mcrypt /dev/shm/secret.conf -z -a rijndael-128 -m cbc -k "$(DECRYPT_PW)"
	cp /dev/shm/secret.conf.gz.nc platform/conf/secret.conf.gz.nc
	/usr/bin/shred -zun 0 /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc

tasks/edit-secret:
	nano /dev/shm/db.conf
	
configure-connection: tasks/remove-secret /dev/shm/db.conf tasks/edit-secret tasks/reencrypt-secret clear-secrets  
	
platform/conf/application.conf: platform/conf/application.conf.template conf/setup.conf conf/pathes.conf conf/cluster.conf
	$(info ------------------------------)
	$(info Configuring Play Application.... )
	$(info ------------------------------)
	cp platform/conf/application.conf.template platform/conf/application.conf
	$(eval PORTAL_ORIGIN:=$(shell if [ -e switches/use-run ]; then echo "https://$(DOMAIN):9002";else echo "https://$(DOMAIN)";fi;))
	$(eval CLUSTERSERVERS:=$(foreach a,$(CLUSTER),"akka://midata@$(a):9006"))
	$(eval CLUSTERX:=$(call join-with,$(komma),$(CLUSTERSERVERS))) 
	sed -i'' -e 's|PORTAL_ORIGIN|$(PORTAL_ORIGIN)|' platform/conf/application.conf
	sed -i'' -e 's|PLUGINS_SERVER|$(DOMAIN)/plugin|' platform/conf/application.conf
	sed -i'' -e 's|PLUGINS_DIR|$(PLUGINS_DIR)|' platform/conf/application.conf
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' platform/conf/application.conf	
	sed -i'' -e 's|PLATFORM_HOSTNAME|$(HOSTNAME)|' platform/conf/application.conf
	sed -i'' -e 's|CLUSTER_SERVER|$(CLUSTERX)|' platform/conf/application.conf
	sed -i'' -e 's|INSTANCETYPE|$(INSTANCE_TYPE)|' platform/conf/application.conf
	sed -i'' -e 's|USER_MAIL_PASSWORD|$(USER_MAIL_PASSWORD)|' platform/conf/application.conf
	sed -i'' -e 's|STATUS_MAIL_PASSWORD|$(STATUS_MAIL_PASSWORD)|' platform/conf/application.conf
	sed -i'' -e 's|BULK_MAIL_PASSWORD|$(BULK_MAIL_PASSWORD)|' platform/conf/application.conf
	sed -i'' -e 's|USER_MAIL_SENDER|$(USER_MAIL_SENDER)|' platform/conf/application.conf
	sed -i'' -e 's|STATUS_MAIL_SENDER|$(STATUS_MAIL_SENDER)|' platform/conf/application.conf
	sed -i'' -e 's|BULK_MAIL_SENDER|$(BULK_MAIL_SENDER)|' platform/conf/application.conf
	sed -i'' -e 's|MAIL_SMTP_SERVER|$(MAIL_SMTP_SERVER)|' platform/conf/application.conf
	sed -i'' -e 's|MAIL_SECURITY_TARGET|$(MAIL_SECURITY_TARGET)|' platform/conf/application.conf
	sed -i'' -e 's|MAIL_ADMIN|$(MAIL_ADMIN)|' platform/conf/application.conf
	sed -i'' -e 's|DEFAULT_LANGUAGE|$(DEFAULT_LANGUAGE)|' platform/conf/application.conf
	sed -i'' -e 's|ROOTDIR|$(abspath .)|' platform/conf/application.conf
	sed -i'' -e 's|SMS_PROVIDER|$(SMS_PROVIDER)|' platform/conf/application.conf
	sed -i'' -e 's|SMS_OAUTH_TOKEN|$(SMS_OAUTH_TOKEN)|' platform/conf/application.conf	
	
conf/config.js: config/config-template.js conf/pathes.conf conf/setup.conf
	$(info ------------------------------)
	$(info Configuring Portal.... )
	$(info ------------------------------)
	$(eval PORTAL_ORIGIN:=$(shell if [ -e switches/use-run ]; then echo "https://$(DOMAIN):9002";else echo "https://$(DOMAIN)";fi;))
	cp config/config-template.js conf/config.js
	sed -i'' -e 's|PORTAL_ORIGIN|$(PORTAL_ORIGIN)|' conf/config.js
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' conf/config.js
	sed -i'' -e 's|PLATFORM_NAME|$(PLATFORM_NAME)|' conf/config.js
	sed -i'' -e 's|OPERATOR_NAME|$(OPERATOR_NAME)|' conf/config.js
	sed -i'' -e 's|PRODUCT_NAME|$(PRODUCT_NAME)|' conf/config.js
	sed -i'' -e 's|OFFICIAL_SUPPORT_MAIL|$(OFFICIAL_SUPPORT_MAIL)|' conf/config.js
	sed -i'' -e 's|OFFICIAL_HOMEPAGE|$(OFFICIAL_HOMEPAGE)|' conf/config.js
	sed -i'' -e 's|INSTANCE_TYPE|$(INSTANCE_TYPE)|' conf/config.js
	sed -i'' -e 's|INSTANCE|$(INSTANCE)|' conf/config.js
	sed -i'' -e 's|LANGUAGES|$(LANGUAGES)|' conf/config.js
	sed -i'' -e 's|DEFAULT_LANGUAGE|$(DEFAULT_LANGUAGE)|' conf/config.js
	sed -i'' -e 's|COUNTRIES|$(COUNTRIES)|' conf/config.js
	sed -i'' -e 's|BETA_FEATURES|$(BETA_FEATURES)|' conf/config.js		
	
tasks/reimport-mongodb: trigger/reimport-mongodb $(wildcard json/*.json)
	$(info ------------------------------)
	$(info Importing META-DATA into mongoDB.... )
	$(info ------------------------------)
	cd json;make reimport
	touch tasks/reimport-mongodb

tasks/reimport-plugins: trigger/reimport-plugins
	$(info ------------------------------)
	$(info Importing Plugins into mongoDB.... )
	$(info ------------------------------)
	cd json;make reimportplugins
	touch tasks/reimport-plugins
	
tasks/changelog: CHANGELOG.tsv
	$(info ------------------------------)
	$(info Updating Changelog...)
	$(info ------------------------------)
	cd json;make changelog
	touch tasks/changelog
		
tasks/build-mongodb: trigger/build-mongodb tasks/changelog tasks/reimport-mongodb tasks/reimport-plugins $(wildcard json/*.js)
	$(info ------------------------------)
	$(info (Re-)creating database indexes)
	$(info ------------------------------)
	cd json;make build	
	touch tasks/build-mongodb
	
tasks/build-portal: trigger/build-portal conf/recoverykeys.json $(shell find portal -type f | sed 's/ /\\ /g')
	$(info ------------------------------)
	$(info Building Portal... )
	$(info ------------------------------)
	cd portal;npm ci;npm run prod:build;
	touch tasks/build-portal
	
tasks/build-platform: $(shell find platform -name "*.java" | sed 's/ /\\ /g')
	$(info ------------------------------)
	$(info Creating Server Play Application... )
	$(info ------------------------------)
	cd platform;./sbt dist
	touch tasks/build-platform
	
tasks/reimport-build-mongodb: tasks/reimport-mongodb tasks/build-mongodb
	touch tasks/reimport-build-mongodb

nginx/sites-available/%: nginx/templates/% conf/setup.conf conf/pathes.conf conf/certificate.conf	
	mkdir -p nginx/sites-available
	cp nginx/templates/$* nginx/sites-available/$*
	if [ $(INSTANCE_TYPE) = PROD ]; then sed -i 's|https://localhost:9004 ||' nginx/sites-available/$*; sed -i 's|https://localhost:9004 ||' nginx/sites-available/$* ; sed -i 's|https://localhost:9004 ||' nginx/sites-available/$* ; fi 
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i'' -e 's|CERTIFICATE_PEM|$(CERTIFICATE_PEM)|' nginx/sites-available/$*
	sed -i'' -e 's|CERTIFICATE_KEY|$(CERTIFICATE_KEY)|' nginx/sites-available/$*
	sed -i'' -e 's|CERTIFICATE_CLIENT|$(CERTIFICATE_CLIENT)|' nginx/sites-available/$*
	sed -i'' -e 's|DHPARAMS|$(CERTIFICATE_DIR)/dhparams.pem|' nginx/sites-available/$*
	sed -i'' -e 's|PLATFORM_INTERNAL_PORT|9001|' nginx/sites-available/$*
	sed -i'' -e 's|ROOTDIR|$(abspath .)|' nginx/sites-available/$*
	sed -i'' -e 's|PLUGINS_DIR|$(PLUGINS_DIR)/web|' nginx/sites-available/$*
	sed -i'' -e 's|RUNDIR|$(abspath running)|' nginx/sites-available/$* 
	
tasks/setup-nginx: nginx/sites-available/sslredirect nginx/sites-available/webpages nginx/conf.d/noversion.conf $(CERTIFICATE_PEM) $(CERTIFICATE_DIR)/dhparams.pem
	$(info ------------------------------)
	$(info Configuring NGINX... )
	$(info ------------------------------)
	chmod 755 conf/config.js
	chmod 755 conf/recoverykeys.json
	chmod 755 conf	
	sudo cp nginx/sites-available/* /etc/nginx/sites-available
	sudo cp nginx/conf.d/* /etc/nginx/conf.d
	sudo rm -f /etc/nginx/sites-enabled/*
	sudo ln -s /etc/nginx/sites-available/sslredirect /etc/nginx/sites-enabled/sslredirect || true
	sudo ln -s /etc/nginx/sites-available/webpages /etc/nginx/sites-enabled/ || true	
	sudo nginx -t && sudo service nginx reload
	touch tasks/setup-nginx
	
$(CERTIFICATE_DIR)/selfsign.crt:
	$(info ------------------------------)
	$(info Create Self signed Certificate... )
	$(info ------------------------------)
	mkdir -p $(CERTIFICATE_DIR)
	openssl req -x509 -nodes -newkey rsa:2048 -keyout $(CERTIFICATE_DIR)/selfsign.key -out $(CERTIFICATE_DIR)/selfsign.crt -days 365

$(CERTIFICATE_DIR)/clientca.pem:
	$(info ------------------------------)
	$(info Create Certificate Authority... )
	$(info ------------------------------)
	mkdir -p $(CERTIFICATE_DIR)
	openssl req -new -nodes -x509 -newkey rsa:4096 -days 3650 -keyout $(CERTIFICATE_DIR)/clientca.key -out $(CERTIFICATE_DIR)/clientca.pem

order-certificate:
	$(info ---------------------------------------------)
	$(info Generate Certificate Request )
	$(info ---------------------------------------------) 		
	$(eval CSRFILE:=$(abspath $(CERTIFICATE_DIR)/$(DOMAIN)_$(YEAR).csr))
	$(eval KEYFILE:=$(abspath $(CERTIFICATE_DIR)/$(DOMAIN)_$(YEAR).key))
	openssl req -new -newkey rsa:2048 -nodes -keyout $(KEYFILE) -out $(CSRFILE)
	chmod ugo-rwx $(KEYFILE)
	@echo Here is your CSR:
	@echo
	@cat $(CSRFILE)
	@echo
	
install-certificate:
	$(info ---------------------------------------------)
	$(info Install new certificate )
	$(info ---------------------------------------------) 		
	$(eval PEMFILE:=$(abspath $(CERTIFICATE_DIR)/$(DOMAIN)_$(YEAR).pem))
	$(eval KEYFILE:=$(abspath $(CERTIFICATE_DIR)/$(DOMAIN)_$(YEAR).key))	
	@read -p "Hit enter and copy certificate into editor (with certificate chain):" dummy
	nano $(PEMFILE)		
	echo "CERTIFICATE_PEM=$(PEMFILE)\nCERTIFICATE_KEY=$(KEYFILE)\n" > conf/certificate.conf	
		
use-loadbalancer: /etc/ssl/certs/ssl-cert-snakeoil.pem
	echo "CERTIFICATE_PEM=/etc/ssl/certs/ssl-cert-snakeoil.pem\nCERTIFICATE_KEY=/etc/ssl/private/ssl-cert-snakeoil.key\n" >conf/certificate.conf
	
/etc/ssl/certs/ssl-cert-snakeoil.pem:
	$(info ------------------------------)
	$(info Install Certificate for use with Load Balancer... )
	$(info ------------------------------)
	sudo apt-get install ssl-cert	
	
tasks/bugfixes:
	mkdir -p ~/.config
	mkdir -p ~/.npm
	sudo chown -R $$USER:$$GROUP ~/.config	
	sudo chown -R $$USER:$$GROUP ~/.npm
	touch tasks/bugfixes

conf/recoverykeys.json:
	echo '[ { "neededKeys" : 1, "availableKeys" : 0 } ]' >conf/recoverykeys.json

/dev/shm/secret.conf: platform/conf/application.conf platform/conf/secret.conf.gz.nc 
	@echo "Decrypting configfile..."
	rm -f /dev/shm/secret.conf*
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Mantra for config file:" pw;stty echo;printf "\n";printf $$pw;)))	
	cp platform/conf/secret.conf.gz.nc /dev/shm/secret.conf.gz.nc
	@cd /dev/shm;/usr/bin/mcrypt /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d -k "$(DECRYPT_PW)"
	cat platform/conf/application.conf >> /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc 

/dev/shm/db.conf: platform/conf/application.conf platform/conf/secret.conf.gz.nc 
	@echo "Decrypting configfile..."
	rm -f /dev/shm/secret.conf*
	rm -f /dev/shm/db.conf
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Mantra for config file:" pw;stty echo;printf "\n";printf $$pw;)))	
	cp platform/conf/secret.conf.gz.nc /dev/shm/secret.conf.gz.nc
	@cd /dev/shm;/usr/bin/mcrypt /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d -k "$(DECRYPT_PW)"	
	mv /dev/shm/secret.conf /dev/shm/db.conf
	rm -f /dev/shm/secret.conf.gz.nc 

/var/log/midata/application.log:
	$(info ------------------------------)
	$(info Setting up application log )
	$(info ------------------------------)
	sudo mkdir -p /var/log/midata
	sudo chown $$USER:$$GROUP /var/log/midata
	touch /var/log/midata/application.log
	touch /var/log/midata/indexes.log
	touch /var/log/midata/jobs.log
	touch /var/log/midata/actions.log

clear-secrets:
	@echo "Removing decrypted config files..."
	@rm -f /dev/shm/secret.conf.gz.nc
	@rm -f /dev/shm/secret.conf
	@rm -f /dev/shm/db.conf

clean:	
	@rm -f tasks/bugfixes
	@rm -f tasks/build-platform
	@rm -f tasks/build-mongodb
	@rm -f tasks/build-portal
	@rm -f tasks/setup-nginx
	@rm -f conf/instance.js
	@rm -f platform/conf/application.conf
	@rm -f nginx/sites-available/*
	@rm -f platform/target
	@rm -f portal/dest
	@echo "Run make update to rebuild."
	
tasks/password:
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Mantra for config file:" pw;stty echo;printf "\n";printf $$pw;)))
