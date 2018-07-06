include conf/*

.PHONY: info
info:
	$(info ------------------------------)
	$(info Welcome to MIDATA)
	$(info ------------------------------)
	$(info   )
	$(info install-webserver : Install a productive frontend server that may use separate database servers)
	$(info install-local : Install a localhost instance)
	$(info   )
	$(info create-mongo-passwords : Run once after your mongoDB setup is ready and running. For webserver)
	$(info configure-connection : Reconfigure database connection)
	$(info   )
	$(info order-ssl : Generate new CSR to order a new certificate)
	$(info install-ssl : Activate new certificate)
	$(info use-loadbalancer : Use loadbalancer)
	$(info   )
	$(info update : Update and start current instance)
	$(info start : Start current instance)
	$(info stop : Stop current instance)

install-webserver: tasks/install-packages tasks/install-node tasks/bugfixes tasks/prepare-webserver tasks/install-localmongo tasks/install-activator $(CERTIFICATE_DIR)/dhparams.pem tasks/configure-connection
	$(info Please run "make order-ssl" to order SSL certificate)
	$(info Please run "make install-ssl" to install SSL certificate that has been ordered before)
	$(info Please run "make skip-ssl" to continue with a fake SSL certificate that will trigger browser warnings)
	$(info Please run "make use-loadbalancer" to continue with a default certificate for the load balancer)	
	$(info Please run "make configure-connection" to setup database connection)
	$(info Please run "make update" to build after everything has been configured correctly)

install-from-servertools: tasks/install-packages tasks/install-node tasks/bugfixes tasks/install-localmongo tasks/install-activator $(CERTIFICATE_DIR)/dhparams.pem tasks/configure-connection
	touch switches/use-hotdeploy

install-local: tasks/install-packages tasks/install-node tasks/bugfixes tasks/prepare-local tasks/check-config tasks/install-dummycert tasks/install-localmongo tasks/install-lighttpd tasks/install-activator tasks/configure-connection
	touch switches/local-mongo
	$(info Please run "make update" to build)
	touch switches/local-mongo

.PHONY: pull
pull:
	git pull

.PHONY: start
start: /dev/shm/secret.conf
	if [ -e switches/use-hotdeploy ]; then sh ./hotdeploy.sh; fi;
	if [ -e switches/use-run ]; then cd platform;./sbt -Dhttps.port=9000 -Dhttp.port=9001; fi;

update: tasks/check-config start-mongo tasks/setup-portal tasks/build-mongodb tasks/build-portal tasks/build-plugins tasks/build-platform start

.PHONY: stop
stop:
	$(info ------------------------------)
	$(info Locking and stopping platform)
	$(info ------------------------------)
	touch locks/lock
	python main.py stop

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
	test=`pgrep mongo`; if [ -e switches/local-mongo -a -z "$$test" ]; then python main.py start mongodb; fi 

$(CERTIFICATE_DIR)/dhparams.pem:
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
	sudo apt-get install git curl openssl python openjdk-8-jdk nginx mcrypt unzip ruby-sass	
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
	rm mongodb-linux-x86_64-{MONGO_VERSION}.tgz			
	touch tasks/install-localmongo

	
conf/secret.conf.gz.nc:		
	$(info ------------------------------)
	$(info Generating new encrypted config file.... )
	$(info ------------------------------)
	$(info You will need a strong mantra to encrypt and decrypt the configuration file. )
	rm -f /dev/shm/secret.conf*
	cp conf/secret.conf.template /dev/shm/secret.conf
	sed -i '/application.secret/d' /dev/shm/secret.conf
	NEWSECRET=`cd platform;./sbt playGenerateSecret | grep 'new secret:' | sed 's/^.*: //' | sed 's/[^[:print:]]//'` ; echo "application.secret=\"$$NEWSECRET\"" >> /dev/shm/secret.conf
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Password:" pw;stty echo;printf "\n";printf $$pw;)))
	@/usr/bin/mcrypt /dev/shm/secret.conf -z -a rijndael-128 -m cbc -k "$(DECRYPT_PW)"
	cp /dev/shm/secret.conf.gz.nc platform/conf/secret.conf.gz.nc
	/usr/bin/shred -zun 0 /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc

tasks/remove-secret:
	rm -f /dev/shm/secret.conf*

tasks/reencrypt-secret:
	$(info ------------------------------)
	$(info Encrypting configuration file.... )
	$(info ------------------------------)
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Password:" pw;stty echo;printf "\n";printf $$pw;)))
	@/usr/bin/mcrypt /dev/shm/secret.conf -z -a rijndael-128 -m cbc -k "$(DECRYPT_PW)"
	cp /dev/shm/secret.conf.gz.nc platform/conf/secret.conf.gz.nc
	/usr/bin/shred -zun 0 /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc

tasks/edit-secret:
	nano /dev/shm/secret.conf
	
configure-connection: tasks/remove-secret /dev/shm/secret.conf tasks/edit-secret tasks/reencrypt-secret  
	
platform/conf/application.conf: platform/conf/application.conf.template conf/setup.conf conf/pathes.conf 
	@echo 'Setting up Plattform...'
	cp platform/conf/application.conf.template platform/conf/application.conf
	sed -i 's|PORTAL_ORIGIN|https://$(DOMAIN)|' platform/conf/application.conf
	sed -i 's|PLUGINS_SERVER|$(DOMAIN)/plugin|' platform/conf/application.conf
	sed -i 's|DOMAIN|$(DOMAIN)|' platform/conf/application.conf	
	sed -i 's|PLATFORM_HOSTNAME|$(HOSTNAME)|' platform/conf/application.conf
	sed -i 's|CLUSTER_SERVER|$(CLUSTER)|' platform/conf/application.conf
	sed -i 's|INSTANCETYPE|$(INSTANCE_TYPE)|' platform/conf/application.conf
	sed -i 's|MAIL_PASSWORD|$(MAIL_PASSWORD)|' platform/conf/application.conf
	sed -i 's|MAIL_SENDER|$(MAIL_SENDER)|' platform/conf/application.conf
	sed -i 's|MAIL_SMTP_SERVER|$(MAIL_SMTP_SERVER)|' platform/conf/application.conf
	sed -i 's|MAIL_SECURITY_TARGET|$(MAIL_SECURITY_TARGET)|' platform/conf/application.conf
	sed -i 's|DEFAULT_LANGUAGE|$(DEFAULT_LANGUAGE)|' platform/conf/application.conf
	sed -i 's|ROOTDIR|$(abspath .)|' platform/conf/application.conf	
	
tasks/reimport-mongodb: trigger/reimport-mongodb $(wildcard json/*.json)
	$(info ------------------------------)
	$(info Importing META-DATA into mongoDB.... )
	$(info ------------------------------)
	python main.py reimport mongodb
	touch tasks/reimport-mongodb

tasks/reimport-plugins: trigger/reimport-plugins
	$(info ------------------------------)
	$(info Importing Plugins into mongoDB.... )
	$(info ------------------------------)
	python main.py reimportplugins mongodb
	touch tasks/reimport-plugins
	
tasks/build-mongodb: trigger/build-mongodb tasks/reimport-mongodb tasks/reimport-plugins
	python main.py start mongodb
	python main.py build mongodb
	touch tasks/build-mongodb

tasks/build-plugins: trigger/build-plugins $(shell find visualizations/*/src -type f | sed 's/ /\\ /g')
	$(info ------------------------------)
	$(info Building Plugins... )
	$(info ------------------------------)
	python main.py build plugins
	touch tasks/build-plugins
	
tasks/build-portal: trigger/build-portal $(shell find portal -type f | sed 's/ /\\ /g')
	$(info ------------------------------)
	$(info Building Portal... )
	$(info ------------------------------)
	cd portal;npm install;bower update;grunt deploy;
	touch tasks/build-portal
	
tasks/build-platform: $(shell find platform -name "*.java" | sed 's/ /\\ /g')
	cd platform;./sbt dist
	touch tasks/build-platform
	
tasks/reimport-build-mongodb: tasks/reimport-mongodb tasks/build-mongodb
	touch tasks/reimport-build-mongodb

nginx/sites-available/%: nginx/templates/% conf/setup.conf conf/pathes.conf
	cp nginx/templates/$* nginx/sites-available/$*
	sed -i 's|DOMAIN|https://$(DOMAIN)|' nginx/sites-available/$*
	sed -i 's|CERTIFICATE_PEM|$(CERTIFICATE_PEM)|' nginx/sites-available/$*
	sed -i 's|CERTIFICATE_KEY|$(CERTIFICATE_KEY)|' nginx/sites-available/$*
	sed -i 's|DHPARAMS|https://$(CERTIFICATE_DIR)/dhparams.pem|' nginx/sites-available/$*
	sed -i 's|PLATFORM_INTERNAL_PORT|9001|' nginx/sites-available/$*
	sed -i 's|ROOTDIR|$(abspath .)|' nginx/sites-available/$*
	sed -i 's|RUNDIR|$(abspath running)|' nginx/sites-available/$*
	
tasks/setup-nginx: nginx/sites-available/sslredirect nginx/sites-available/webpages		
	sudo cp nginx/sites-available/* /etc/nginx/sites-available
	sudo ln -s /etc/nginx/sites-available/sslredirect /etc/nginx/sites-enabled/sslredirect || true
	sudo ln -s /etc/nginx/sites-available/plugins /etc/nginx/sites-enabled/ || true
	sudo ln -s /etc/nginx/sites-available/portal_api /etc/nginx/sites-enabled/ || true
	sudo ln -s /etc/nginx/sites-available/webpages /etc/nginx/sites-enabled/ || true
	sudo rm -f /etc/nginx/sites-enabled/default
	sudo nginx -t && sudo service nginx reload
	touch tasks/setup-nginx
	
order-ssl:		    
	$(info ------------------------------)
	$(info Order SSL Certificate... )
	$(info ------------------------------)
	$(eval DOMAIN := $(shell cat config/instance.json | python -c "import sys, json; print json.load(sys.stdin)['domain']"))
	$(eval YEAR := $(shell read -p "Enter Current Year (4digits): " pw ;printf $$pw;))
	mkdir -p ../ssl
	openssl req -new -nodes -keyout ../ssl/$(DOMAIN)_$(YEAR).key -out ../ssl/$(DOMAIN)_$(YEAR).csr -newkey rsa:2048;
	@echo "----------------------"
	@echo "Your certificate request CSR is here:"
	@echo "$(abspath ../ssl/$(DOMAIN)_$(YEAR).csr)"
	@echo
	@echo "Please put the certificate once you have it here:"
	@echo "$(abspath ../ssl/$(DOMAIN)_$(YEAR).crt)"
	@echo
	@echo "Append the CA-Chain to the certificate so that it is only one file."
	@echo "Run make install-ssl when ready"
	@echo "----------------------"

$(CERTIFICATE_DIR)/selfsign.crt:
	mkdir -p $(CERTIFICATE_DIR)
	openssl req -x509 -nodes -newkey rsa:2048 -keyout $(CERTIFICATE_DIR)/selfsign.key -out $(CERTIFICATE_DIR)/selfsign.crt -days 365
		
install-ssl: reconfig tasks/set-ssl-path tasks/check-config tasks/setup-nginx

skip-ssl: self-sign-ssl install-ssl

use-loadbalancer: reconfig tasks/install-ssl-lb tasks/set-ssl-lb tasks/check-config tasks/setup-nginx 

	
tasks/install-ssl-lb:
	sudo apt-get install ssl-cert
	touch tasks/install-ssl-lb
	
tasks/set-ssl-lb:
	node scripts/replace.js certificate pem /etc/ssl/certs/ssl-cert-snakeoil.pem;
	node scripts/replace.js certificate key /etc/ssl/private/ssl-cert-snakeoil.key;

tasks/bugfixes:
	sudo chown -R $$USER:$$GROUP ~/.config	
	sudo chown -R $$USER:$$GROUP ~/.npm
	touch tasks/bugfixes

/dev/shm/secret.conf: 
	@echo "Decrypting configfile..."
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Password:" pw;stty echo;printf "\n";printf $$pw;)))	
	cp platform/conf/secret.conf.gz.nc /dev/shm/secret.conf.gz.nc
	@cd /dev/shm;/usr/bin/mcrypt /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d -k "$(DECRYPT_PW)"
	cat platform/conf/application.conf >> /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc 

tasks/password:
	$(eval DECRYPT_PW := $(if $(DECRYPT_PW),$(DECRYPT_PW),$(shell stty -echo;read -p "Password:" pw;stty echo;printf "\n";printf $$pw;)))
