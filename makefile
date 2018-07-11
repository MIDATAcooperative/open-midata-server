include conf/*

.PHONY: info
info:
	$(info ------------------------------)
	$(info Welcome to MIDATA)
	$(info ------------------------------)
	$(info   )	
	$(info install-local : Install a localhost instance)
	$(info   )
	$(info configure-connection : Reconfigure database connection)
	$(info   )	
	$(info update : Update and start current instance)
	$(info start : Start current instance)
	$(info stop : Stop current instance)
	$(info   )	
	$(info lock : Lock current instance (exclude from load balancer) )
	$(info unlock : Unlock current instance)

space :=
space +=	
komma :=,
join-with = $(subst $(space),$1,$(strip $2))

install-from-servertools: tasks/install-packages tasks/install-node tasks/bugfixes tasks/install-localmongo $(CERTIFICATE_DIR)/dhparams.pem /etc/ssl/certs/ssl-cert-snakeoil.pem tasks/precompile
	touch switches/use-hotdeploy
	touch tasks/check-config

install-local: tasks/install-packages tasks/install-node tasks/bugfixes tasks/prepare-local tasks/check-config $(CERTIFICATE_DIR)/selfsign.crt $(CERTIFICATE_DIR)/dhparams.pem tasks/install-localmongo conf/secret.conf.gz.nc tasks/precompile 
	touch switches/local-mongo
	$(info Please run "make update" to build)
	touch switches/local-mongo

.PHONY: pull
pull:
	git pull

.PHONY: start
start: /dev/shm/secret.conf start1 clear-secrets

start1: 
	if [ -e switches/use-hotdeploy ]; then sh ./hotdeploy.sh; fi;
	if [ -e switches/use-run ]; then sudo service nginx start; fi;
	if [ -e switches/use-run ]; then mkdir -p locks;cd platform;./sbt run -Dpidfile.path=/dev/shm/play.pid -J-Xverify:none -Dconfig.file=/dev/shm/secret.conf -Dhttps.port=9000 -Dhttp.port=9001; fi;

stop-mongo:
	@echo 'Shutting down MongoDB...'
	if [ -e switches/local-mongo ]; then pkill mongod; fi

update: tasks/check-config start-mongo tasks/build-mongodb tasks/build-portal tasks/build-platform tasks/setup-nginx start

.PHONY: stop
stop: stop-platform stop-mongo

stop-platform:
	$(info ------------------------------)
	$(info Locking and stopping platform)
	$(info ------------------------------)
	touch locks/lock
	pkill -f sbt
	

.PHONY: lock
lock:
	$(info ------------------------------)
	$(info Locking platform)
	$(info ------------------------------)
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

tasks/prepare-local:
	touch switches/use-run
	touch switches/local-mongo		
	touch tasks/prepare-local
		
tasks/install-packages: trigger/install-packages
	$(info ------------------------------)
	$(info Installing Packages... )
	$(info ------------------------------)
	sudo apt-get install git curl openssl openjdk-8-jdk nginx mcrypt unzip ruby-sass	
	touch tasks/install-packages
	
tasks/install-node: tasks/install-packages trigger/install-node
	$(info ------------------------------)
	$(info Installing Node JS... )
	$(info ------------------------------)
	curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
	sudo apt-get install -y nodejs
	sudo npm install -g bower grunt-cli
	sudo chmod -R ugo+rx /usr/lib/node_modules
	touch tasks/install-node

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
	cp config/mongod.conf mongodb/mongod.conf
	sed -i 's|MONGODB_DATA_PATH|$(abspath monodb/data)|' mongodb/mongod.conf
	sed -i 's|MONGODB_LOG_PATH|$(abspath logs/mongod.log)|' mongodb/mongod.conf
	rm mongodb-linux-x86_64-$(MONGO_VERSION).tgz			
	touch tasks/install-localmongo

	
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
	$(eval CLUSTERSERVERS:=$(foreach a,$(CLUSTER),"akka.tcp://midata@$(a):9006"))
	$(eval CLUSTERX:=$(call join-with,$(komma),$(CLUSTERSERVERS))) 
	sed -i 's|PORTAL_ORIGIN|$(PORTAL_ORIGIN)|' platform/conf/application.conf
	sed -i 's|PLUGINS_SERVER|$(DOMAIN)/plugin|' platform/conf/application.conf
	sed -i 's|DOMAIN|$(DOMAIN)|' platform/conf/application.conf	
	sed -i 's|PLATFORM_HOSTNAME|$(HOSTNAME)|' platform/conf/application.conf
	sed -i 's|CLUSTER_SERVER|$(CLUSTERX)|' platform/conf/application.conf
	sed -i 's|INSTANCETYPE|$(INSTANCE_TYPE)|' platform/conf/application.conf
	sed -i 's|MAIL_PASSWORD|$(MAIL_PASSWORD)|' platform/conf/application.conf
	sed -i 's|MAIL_SENDER|$(MAIL_SENDER)|' platform/conf/application.conf
	sed -i 's|MAIL_SMTP_SERVER|$(MAIL_SMTP_SERVER)|' platform/conf/application.conf
	sed -i 's|MAIL_SECURITY_TARGET|$(MAIL_SECURITY_TARGET)|' platform/conf/application.conf
	sed -i 's|MAIL_ADMIN|$(MAIL_ADMIN)|' platform/conf/application.conf
	sed -i 's|DEFAULT_LANGUAGE|$(DEFAULT_LANGUAGE)|' platform/conf/application.conf
	sed -i 's|ROOTDIR|$(abspath .)|' platform/conf/application.conf	
	
config/instance.json: config/instance-template.json conf/pathes.conf conf/setup.conf
	$(info ------------------------------)
	$(info Configuring Portal.... )
	$(info ------------------------------)
	$(eval PORTAL_ORIGIN:=$(shell if [ -e switches/use-run ]; then echo "https://$(DOMAIN):9002";else echo "https://$(DOMAIN)";fi;))
	cp config/instance-template.json config/instance.json
	sed -i 's|PORTAL_ORIGIN|$(PORTAL_ORIGIN)|' config/instance.json
	sed -i 's|DOMAIN|$(DOMAIN)|' config/instance.json
	sed -i 's|INSTANCE_TYPE|$(INSTANCE_TYPE)|' config/instance.json
	sed -i 's|INSTANCE|$(INSTANCE)|' config/instance.json
	sed -i 's|LANGUAGES|$(LANGUAGES)|' config/instance.json
	sed -i 's|DEFAULT_LANGUAGE|$(DEFAULT_LANGUAGE)|' config/instance.json
	sed -i 's|COUNTRIES|$(COUNTRIES)|' config/instance.json
	sed -i 's|BETA_FEATURES|$(BETA_FEATURES)|' config/instance.json		
	
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
	
tasks/build-mongodb: trigger/build-mongodb tasks/reimport-mongodb tasks/reimport-plugins
	$(info ------------------------------)
	$(info (Re-)creating database indexes)
	$(info ------------------------------)
	cd json;make build	
	touch tasks/build-mongodb
	
tasks/build-portal: trigger/build-portal $(shell find portal -type f | sed 's/ /\\ /g') config/instance.json
	$(info ------------------------------)
	$(info Building Portal... )
	$(info ------------------------------)
	cd portal;npm install;bower update;grunt deploy;
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
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|DOMAIN|$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|CERTIFICATE_PEM|$(CERTIFICATE_PEM)|' nginx/sites-available/$*
	sed -i 's|CERTIFICATE_KEY|$(CERTIFICATE_KEY)|' nginx/sites-available/$*
	sed -i 's|DHPARAMS|$(CERTIFICATE_DIR)/dhparams.pem|' nginx/sites-available/$*
	sed -i 's|PLATFORM_INTERNAL_PORT|9001|' nginx/sites-available/$*
	sed -i 's|ROOTDIR|$(abspath .)|' nginx/sites-available/$*
	sed -i 's|PLUGINS_DIR|$(PLUGINS_DIR)/plugin_active|' nginx/sites-available/$*
	sed -i 's|RUNDIR|$(abspath running)|' nginx/sites-available/$* 
	
tasks/setup-nginx: nginx/sites-available/sslredirect nginx/sites-available/webpages $(CERTIFICATE_PEM) $(CERTIFICATE_DIR)/dhparams.pem
	$(info ------------------------------)
	$(info Configuring NGINX... )
	$(info ------------------------------)	
	sudo cp nginx/sites-available/* /etc/nginx/sites-available
	sudo rm -f /etc/nginx/sites-enabled/*
	sudo ln -s /etc/nginx/sites-available/sslredirect /etc/nginx/sites-enabled/sslredirect || true
	sudo ln -s /etc/nginx/sites-available/plugins /etc/nginx/sites-enabled/ || true
	sudo ln -s /etc/nginx/sites-available/portal_api /etc/nginx/sites-enabled/ || true
	sudo ln -s /etc/nginx/sites-available/webpages /etc/nginx/sites-enabled/ || true	
	sudo nginx -t && sudo service nginx reload
	touch tasks/setup-nginx
	
$(CERTIFICATE_DIR)/selfsign.crt:
	$(info ------------------------------)
	$(info Create Self signed Certificate... )
	$(info ------------------------------)
	mkdir -p $(CERTIFICATE_DIR)
	openssl req -x509 -nodes -newkey rsa:2048 -keyout $(CERTIFICATE_DIR)/selfsign.key -out $(CERTIFICATE_DIR)/selfsign.crt -days 365
		
use-loadbalancer: /etc/ssl/certs/ssl-cert-snakeoil.pem
	echo "CERTIFICATE_PEM=/etc/ssl/certs/ssl-cert-snakeoil.pem\nCERTIFICATE_KEY=/etc/ssl/certs/ssl-cert-snakeoil.key\n" >conf/certificate.conf
	
/etc/ssl/certs/ssl-cert-snakeoil.pem:
	$(info ------------------------------)
	$(info Install Certificate for use with Load Balancer... )
	$(info ------------------------------)
	sudo apt-get install ssl-cert	
	
tasks/bugfixes:
	sudo chown -R $$USER:$$GROUP ~/.config	
	sudo chown -R $$USER:$$GROUP ~/.npm
	touch tasks/bugfixes

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
	@rm -f config/instance.json
	@rm -f platform/conf/application.conf
	@rm -f nginx/sites-available/*
	@rm -f platform/target
	@rm -f portal/dest
	@echo "Run make update to rebuild."
	
tasks/password:
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Mantra for config file:" pw;stty echo;printf "\n";printf $$pw;)))
