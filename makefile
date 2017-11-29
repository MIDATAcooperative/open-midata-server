.PHONY: info
info:
	$(info Welcome to MIDATA)
	$(info install-fullserver : Install a server with frontend and backend on one system)
	$(info install-webserver : Install a frontend server that usess a separate database servers)
	$(info install-dbserver : Install a backend server)
	$(info install-local : Install a localhost instance)
	$(info update : Update current instance)

install-fullserver: tasks/install-packages tasks/install-node tasks/prepare-webserver tasks/check-config tasks/install-localmongo tasks/install-activator tasks/setup-nginx tasks/configure-connection

install-webserver: tasks/install-packages tasks/install-node tasks/prepare-webserver tasks/check-config tasks/install-localmongo tasks/install-activator tasks/setup-nginx tasks/configure-connection

install-dbserver: tasks/install-dbserver-mongo

install-local: tasks/install-packages tasks/install-node tasks/prepare-local tasks/check-config tasks/install-dummycert tasks/install-localmongo tasks/install-lighttpd tasks/install-activator tasks/configure-connection

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
	NEWSECRET=`python main.py newsecret activator | grep 'new secret:' | sed 's/^.*: //'` ; echo "application.secret=\"$$NEWSECRET\"" >> /dev/shm/secret.conf
	nano /dev/shm/secret.conf
	python main.py configure activator
	touch tasks/configure-connection
	
tasks/setup-portal: trigger/setup-portal tasks/check-config
	python main.py setup portal
	touch tasks/setup-portal
	
tasks/reimport-mongodb: trigger/reimport-mongodb
	python main.py reimport mongodb
	touch tasks/reimport-mongodb
	
tasks/build-mongodb: trigger/build-mongodb tasks/reimport-mongodb
	python main.py start mongodb
	python main.py build mongodb
	touch tasks/build-mongodb

tasks/build-plugins: trigger/build-plugins
	python main.py build plugins
	touch tasks/build-plugins
	
tasks/build-portal: trigger/build-portal
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
tasks/order-ssl:		    
	echo $(DOMAIN)
	read -p "Enter Current Year (4digits): " year;openssl req -new -nodes -keyout ../ssl/$(DOMAIN)_$$year.key -out ../ssl/$(DOMAIN)_$$year.csr -newkey rsa:2048;
		
tasks/install-ssl: reconfig tasks/check-config
	python main.py setup nginx
	sudo cp nginx/sites-available/* /etc/nginx/sites-available
	sudo nginx -t && sudo service nginx reload	
	

tasks/install-dbserver-mongo:
	sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927
	echo "deb http://repo.mongodb.org/apt/ubuntu trusty/mongodb-org/3.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list
	sudo apt-get update
	sudo apt-get install -y mongodb-org=3.2.3 mongodb-org-server=3.2.3 mongodb-org-shell=3.2.3 mongodb-org-mongos=3.2.3 mongodb-org-tools=3.2.3
	echo "mongodb-org hold" | sudo dpkg --set-selections
	echo "mongodb-org-server hold" | sudo dpkg --set-selections
	echo "mongodb-org-shell hold" | sudo dpkg --set-selections
	echo "mongodb-org-mongos hold" | sudo dpkg --set-selections
	echo "mongodb-org-tools hold" | sudo dpkg --set-selections	
	touch tasks/install-dbserver-mongo
