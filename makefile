.PHONY: info
info:
	$(info Welcome to MIDATA)
	$(info install-fullserver : Install a server with frontend and backend on one system)
	$(info install-webserver : Install a frontend server that uses separate database servers)
	$(info install-local : Install a localhost instance)
	$(info   )
	$(info order-ssl : Generate new CSR to order a new certificate)
	$(info install-ssl : Activate new certificate)
	$(info   )
	$(info update : Update current instance)

install-fullserver: tasks/install-packages tasks/install-node tasks/prepare-webserver tasks/check-config tasks/install-localmongo tasks/install-activator tasks/setup-nginx tasks/configure-connection
	$(info Please run "make update" to build)

install-webserver: tasks/install-packages tasks/install-node tasks/prepare-webserver tasks/check-config tasks/install-localmongo tasks/install-activator tasks/dhparams tasks/setup-nginx tasks/configure-connection
	$(info Please run "make update" to build)

install-local: tasks/install-packages tasks/install-node tasks/prepare-local tasks/check-config tasks/install-dummycert tasks/install-localmongo tasks/install-lighttpd tasks/install-activator tasks/configure-connection
	$(info Please run "make update" to build)

.PHONY: pull
pull:
	git pull

.PHONY: restart
restart:
	if [ -e switches/use-hotdeploy ]; then ./hotdeploy.sh; fi;
	if [ -e switches/use-run ]; then python main.py run; fi;

update: tasks/check-config start-mongo tasks/setup-portal tasks/build-mongodb tasks/build-portal tasks/build-plugins restart

.PHONY: reconfig
reconfig:
	rm tasks/check-config

.PHONY: start-mongo
start-mongo:
	test=`pgrep mongo`; if [ -e switches/local-mongo -a -z "$$test" ]; then python main.py start mongodb; fi 

tasks/prepare-webserver:
	touch switches/use-hotdeploy
	cp config/instance-template.json config/instance.json
	read -p "Enter domain name: " newdomain ; node scripts/replace.js domain $$newdomain ; node scripts/replace.js portal origin https://$$newdomain ; node scripts/replace.js portal backend https://$$newdomain ; 
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
	sudo apt-get install git curl openssl python openjdk-8-jdk nginx mcrypt sqlite3 unzip
	touch tasks/install-packages
	
tasks/install-node: tasks/install-packages trigger/install-node
	curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
	sudo apt-get install -y nodejs
	sudo npm install -g bower grunt-cli
	touch tasks/install-node

tasks/check-config: trigger/check-config
	nano config/instance.json
	touch tasks/check-config
	
tasks/install-activator: trigger/install-activator
	python main.py setup activator
	touch tasks/install-activator

tasks/install-localmongo: trigger/install-localmongo
	python main.py setup mongodb
	touch switches/local-mongo
	touch tasks/install-localmongo

tasks/install-dummycert: trigger/install-dummycert
	python main.py setup sslcert
	touch tasks/install-dummycert

tasks/install-lighttpd: trigger/install-lighttpd
	python main.py setup lighttpd
	touch tasks/install-lighttpd
	
tasks/configure-connection: trigger/configure-connection
	python main.py configure activator
	sed -i '/application.secret/d' /dev/shm/secret.conf
	NEWSECRET=`python main.py newsecret activator | grep 'new secret:' | sed 's/^.*: //' | sed 's/[^[:print:]]//'` ; echo "application.secret=\"$$NEWSECRET\"" >> /dev/shm/secret.conf
	nano /dev/shm/secret.conf
	python main.py configure activator
	touch tasks/configure-connection
	
tasks/setup-portal: trigger/setup-portal tasks/check-config config/instance.json
	python main.py setup portal
	touch tasks/setup-portal
	
tasks/reimport-mongodb: trigger/reimport-mongodb $(wildcard json/*.json)
	python main.py reimport mongodb
	touch tasks/reimport-mongodb
	
tasks/build-mongodb: trigger/build-mongodb tasks/reimport-mongodb
	python main.py start mongodb
	python main.py build mongodb
	touch tasks/build-mongodb

tasks/build-plugins: trigger/build-plugins $(shell find visualizations/*/src -type f | sed 's/ /\\ /g')
	python main.py build plugins
	touch tasks/build-plugins
	
tasks/build-portal: trigger/build-portal $(shell find portal -type f | sed 's/ /\\ /g')
	python main.py build portal
	touch tasks/build-portal
	
tasks/reimport-build-mongodb: tasks/reimport-mongodb tasks/build-mongodb
	touch tasks/reimport-build-mongodb
	
tasks/setup-nginx: tasks/check-config
	python main.py setup nginx
	sudo cp nginx/sites-available/* /etc/nginx/sites-available
	sudo ln -s /etc/nginx/sites-available/sslredirect /etc/nginx/sites-enabled/sslredirect
	sudo ln -s /etc/nginx/sites-available/*_plugins /etc/nginx/sites-enabled/
	sudo ln -s /etc/nginx/sites-available/*_portal_api /etc/nginx/sites-enabled/
	sudo ln -s /etc/nginx/sites-available/*_webpages /etc/nginx/sites-enabled/
	sudo nginx -t && sudo service nginx reload
	touch tasks/setup-nginx
	
DOMAIN := $(shell cat config/instance.json | python -c "import sys, json; print json.load(sys.stdin)['domain']")
order-ssl:		    
	echo $(DOMAIN)
	$(eval YEAR := $(shell read -p "Enter Current Year (4digits): " pw ;printf $$pw;))
	mkdir -p ../ssl
	openssl req -new -nodes -keyout ../ssl/$(DOMAIN)_$(YEAR).key -out ../ssl/$(DOMAIN)_$(YEAR).csr -newkey rsa:2048;
		
install-ssl: reconfig tasks/set-ssl-path tasks/check-config
	python main.py setup nginx
	sudo cp nginx/sites-available/* /etc/nginx/sites-available
	sudo nginx -t && sudo service nginx reload	

tasks/set-ssl-path:
	$(eval YEAR := $(shell read -p "Enter Current Year (4digits): " pw ; printf $$pw))
	$(eval CERTPATH := $(abspath ../ssl/$(DOMAIN)_$(YEAR)))
	node scripts/replace.js certificate pem $(CERTPATH).crt
	node scripts/replace.js certificate key $(CERTPATH).key

tasks/bugfixes:
	sudo chown -R $USER:$GROUP ~/.config	
	sudo chown -R $USER:$GROUP ~/.npm
	touch tasks/bugfixes

