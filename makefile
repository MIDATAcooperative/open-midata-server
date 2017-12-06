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
	$(info   )
	$(info update : Update current instance)

install-webserver: tasks/install-packages tasks/install-node tasks/bugfixes tasks/prepare-webserver tasks/install-localmongo tasks/install-activator tasks/dhparams tasks/configure-connection
	$(info Please run "make order-ssl" to order SSL certificate)
	$(info Please run "make install-ssl" to install SSL certificate that has been ordered before)
	$(info Please run "make skip-ssl" to continue with a fake SSL certificate that will trigger browser warnings)
	$(info Please run "make configure-connection" to setup database connection)
	$(info Please run "make update" to build after everything has been configured correctly)

install-local: tasks/install-packages tasks/install-node tasks/bugfixes tasks/prepare-local tasks/check-config tasks/install-dummycert tasks/install-localmongo tasks/install-lighttpd tasks/install-activator tasks/configure-connection
	$(info Please run "make update" to build)

.PHONY: pull
pull:
	git pull

.PHONY: restart
restart:
	if [ -e switches/use-hotdeploy ]; then sh ./hotdeploy.sh; fi;
	if [ -e switches/use-run ]; then python main.py run; fi;

update: tasks/check-config start-mongo tasks/setup-portal tasks/build-mongodb tasks/build-portal tasks/build-plugins restart

.PHONY: stop
stop:
	python main.py stop

.PHONY: reconfig
reconfig:
	rm -f tasks/check-config

.PHONY: start-mongo
start-mongo:
	test=`pgrep mongo`; if [ -e switches/local-mongo -a -z "$$test" ]; then python main.py start mongodb; fi 

tasks/prepare-webserver:
	touch switches/use-hotdeploy
	cp config/instance-template.json config/instance.json
	$(info ------------------------------)
	$(info Basic configuration of Frontend)
	$(info ------------------------------)
	read -p "Enter domain name: " newdomain ; node scripts/replace.js domain $$newdomain ; node scripts/replace.js portal origin https://$$newdomain ; node scripts/replace.js portal backend https://$$newdomain ; node scripts/replace.js portal plugins $$newdomain/plugins ;
	node scripts/replace.js instanceType prod
	touch tasks/prepare-webserver

tasks/dhparams:
	mkdir -p ../ssl
	openssl dhparam -out ../ssl/dhparams.pem 2048
	node scripts/replace.js certificate dhparams $(abspath ../ssl/dhparams.pem)
	touch tasks/dhparams

tasks/prepare-local:
	touch switches/use-run
	touch switches/local-mongo
	cp config/instance-template.json config/instance.json	
	touch tasks/prepare-local
		
tasks/install-packages: trigger/install-packages
	$(info ------------------------------)
	$(info Installing Packages... )
	$(info ------------------------------)
	sudo apt-get install git curl openssl python openjdk-8-jdk nginx mcrypt sqlite3 unzip
	touch tasks/install-packages
	
tasks/install-node: tasks/install-packages trigger/install-node
	$(info ------------------------------)
	$(info Installing Node JS... )
	$(info ------------------------------)
	curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
	sudo apt-get install -y nodejs
	sudo npm install -g bower grunt-cli
	touch tasks/install-node

tasks/check-config: trigger/check-config
	nano config/instance.json
	touch tasks/check-config
	
tasks/install-activator: trigger/install-activator
	$(info ------------------------------)
	$(info Installing Play Framework... )
	$(info ------------------------------)
	python main.py setup activator
	python main.py newsecret activator
	touch tasks/install-activator

tasks/install-localmongo: trigger/install-localmongo
	$(info ------------------------------)
	$(info Installing Local Version of MongoDB... )
	$(info ------------------------------)
	python main.py setup mongodb
	touch switches/local-mongo
	touch tasks/install-localmongo

tasks/install-dummycert: trigger/install-dummycert
	$(info ------------------------------)
	$(info Generating Dummy Certificate for Development Instance... )
	$(info ------------------------------)
	python main.py setup sslcert
	touch tasks/install-dummycert

tasks/install-lighttpd: trigger/install-lighttpd
	$(info ------------------------------)
	$(info Installing lighttpd Server for Development Instance.... )
	$(info ------------------------------)
	python main.py setup lighttpd
	touch tasks/install-lighttpd
	
tasks/configure-connection: trigger/configure-connection
	$(info ------------------------------)
	$(info Preparing encrypted configuration file.... )
	$(info ------------------------------)
	$(info You will need a strong mantra to encrypt and decrypt the configuration file. )
	rm -f /dev/shm/secret.conf*
	python main.py configure activator
	sed -i '/application.secret/d' /dev/shm/secret.conf
	NEWSECRET=`python main.py newsecret activator | grep 'new secret:' | sed 's/^.*: //' | sed 's/[^[:print:]]//'` ; echo "application.secret=\"$$NEWSECRET\"" >> /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc
	python main.py configure activator
	touch tasks/configure-connection

configure-connection: 
	rm -f /dev/shm/secret.conf*
	python main.py configure activator
	nano /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc
	python main.py configure activator

create-mongo-passwords:
	rm -f /dev/shm/secret.conf*
	python main.py configure activator	
	$(eval PORT=$(shell read -p "Port of Mongo Instance [27017 for sharded instance]:" pw ; echo $$pw))	
	$(eval MASTERPW=$(shell read -p "Choose Admin Password:" pw ; echo $$pw))
	$(eval MAPPINGPW=$(shell read -p "Password for 'mapping' database:" pw ; echo $$pw))
	$(eval USERPW=$(shell read -p "Password for 'user' database:" pw ; echo $$pw))
	$(eval ACCESSPW=$(shell read -p "Password for 'access' database:" pw ; echo $$pw))
	$(eval RECORDPW=$(shell read -p "Password for 'record' database:" pw ; echo $$pw))
	mongodb/bin/mongo --port $(PORT) --eval "db=db.getSiblingDB('admin');db.createUser({ user: 'midataAdmin', pwd: '$(MASTERPW)', roles: [ { role: 'userAdminAnyDatabase', db: 'admin' } ] } );"
	mongodb/bin/mongo --port $(PORT) --eval "db=db.getSiblingDB('admin');db.auth('midataAdmin', '$(MASTERPW)');db=db.getSiblingDB('mapping');db.createUser({user: 'mapping',pwd:'$(MAPPINGPW)',roles: [ { role: 'dbAdmin', db: 'mapping' }, { role: 'readWrite', db: 'mapping' } ] });"
	mongodb/bin/mongo --port $(PORT) --eval "db=db.getSiblingDB('admin');db.auth('midataAdmin', '$(MASTERPW)');db=db.getSiblingDB('user');db.createUser({user: 'user',pwd:'$(USERPW)',roles: [ { role: 'dbAdmin', db: 'user' }, { role: 'readWrite', db: 'user' } ] });"
	mongodb/bin/mongo --port $(PORT) --eval "db=db.getSiblingDB('admin');db.auth('midataAdmin', '$(MASTERPW)');db=db.getSiblingDB('access');db.createUser({user: 'access',pwd:'$(ACCESSPW)',roles: [ { role: 'dbAdmin', db: 'access' }, { role: 'readWrite', db: 'access' } ] });"
	mongodb/bin/mongo --port $(PORT) --eval "db=db.getSiblingDB('admin');db.auth('midataAdmin', '$(MASTERPW)');db=db.getSiblingDB('record');db.createUser({user: 'record',pwd:'$(RECORDPW)',roles: [ { role: 'dbAdmin', db: 'record' }, { role: 'readWrite', db: 'record' } ] });"
	sed -i 's|PASSWORD_USER|$(USERPW)|' /dev/shm/secret.conf
	sed -i 's|PASSWORD_ACCESS|$(ACCESSPW)|' /dev/shm/secret.conf
	sed -i 's|PASSWORD_RECORD|$(RECORDPW)|' /dev/shm/secret.conf
	sed -i 's|PASSWORD_MAPPING|$(MAPPINGPW)|' /dev/shm/secret.conf
	sed -i 's|\# mongo\.|mongo\.|g' /dev/shm/secret.conf
	rm -f /dev/shm/secret.conf.gz.nc
	python main.py configure activator	
	
tasks/setup-portal: trigger/setup-portal tasks/check-config config/instance.json
	python main.py setup portal
	touch tasks/setup-portal
	
tasks/reimport-mongodb: trigger/reimport-mongodb $(wildcard json/*.json)
	$(info ------------------------------)
	$(info Importing META-DATA into mongoDB.... )
	$(info ------------------------------)
	python main.py reimport mongodb
	touch tasks/reimport-mongodb
	
tasks/build-mongodb: trigger/build-mongodb tasks/reimport-mongodb
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
	python main.py build portal
	touch tasks/build-portal
	
tasks/reimport-build-mongodb: tasks/reimport-mongodb tasks/build-mongodb
	touch tasks/reimport-build-mongodb
	
tasks/setup-nginx: tasks/check-config
	python main.py setup nginx
	sudo cp nginx/sites-available/* /etc/nginx/sites-available
	sudo ln -s /etc/nginx/sites-available/sslredirect /etc/nginx/sites-enabled/sslredirect || true
	sudo ln -s /etc/nginx/sites-available/*_plugins /etc/nginx/sites-enabled/ || true
	sudo ln -s /etc/nginx/sites-available/*_portal_api /etc/nginx/sites-enabled/ || true
	sudo ln -s /etc/nginx/sites-available/*_webpages /etc/nginx/sites-enabled/ || true
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

self-sign-ssl:
	$(eval DOMAIN := $(shell cat config/instance.json | python -c "import sys, json; print json.load(sys.stdin)['domain']"))
	$(eval YEAR := $(shell read -p "Enter Current Year (4digits): " pw ;printf $$pw;))
	mkdir -p ../ssl
	openssl req -x509 -nodes -newkey rsa:2048 -keyout ../ssl/$(DOMAIN)_$(YEAR).key -out ../ssl/$(DOMAIN)_$(YEAR).crt -days 365
		
install-ssl: reconfig tasks/set-ssl-path tasks/check-config tasks/setup-nginx

skip-ssl: self-sign-ssl install-ssl

sharding:
	python main.py sharding mongodb

tasks/set-ssl-path:
	$(eval DOMAIN := $(shell cat config/instance.json | python -c "import sys, json; print json.load(sys.stdin)['domain']"))
	$(eval YEAR := $(shell read -p "Enter Current Year (4digits): " pw ; printf $$pw))
	$(eval CERTPATH := $(abspath ../ssl/$(DOMAIN)_$(YEAR)))
	node scripts/replace.js certificate pem $(CERTPATH).crt
	node scripts/replace.js certificate key $(CERTPATH).key

tasks/bugfixes:
	sudo chown -R $$USER:$$GROUP ~/.config	
	sudo chown -R $$USER:$$GROUP ~/.npm
	touch tasks/bugfixes

