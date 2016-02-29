MIDATA COOP 
=============================

Manage, understand, and leverage your health data.
[![Build Status](https://drone.io/github.com/amarfurt/hdc/status.png)](https://drone.io/github.com/amarfurt/hdc/latest)


This repository consists of three parts:
- The MIDATA platform : Provides an API with all the functionality used by the portal, plugins and external apps
- The MIDATA portal : HTML frontend for using the platform
- Plugins : Various plugins for the MIDATA platform

The HTML portal is separate from the platform and may be installed and run independent from the platform by connecting to a 
external instance of the platform. 

MIDATA platform and plugins
===========================


Installation
-------
 
To install all required software on ubuntu run:
```
sudo apt-get install git curl openssl python openjdk-7-jdk nginx mcrypt sqlite3
curl -sL https://deb.nodesource.com/setup_4.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g bower grunt-cli
```

Then clone this repository into a directory of your choice. 
```
git clone https://github.com/amarfurt/hdc.git
cd hdc
```

Then check the config/instance.json file if all settings are correct for your system:
```
nano config/instance.json
```

To download dependend software packages and generate configuration files depending on your instance.json file run once:
```
python main.py setup
```
This will ask you to select a keystore password which you need to repeat several times.

Next configure the database connections for the instance:
```
python main.py configure activator
nano /dev/shm/secret.conf
python main.py configure activator
```
The first call to 'configure activator' will place an example configuration file at /dev/shm/secret.conf. Edit the database
configuration in that file. When you run 'configure activator' for the second time you will be asked a 'Mantra' that will be
used to encrypt the configuration. You will need to reenter that Mantra whenever access to the database configuration is required.
If you repeat the 3 commands you will need to enter the (previously chosen) mantra also for decryption of the existing configuration.
You can then choose a new mantra for reencryption. 

To populate the database with initial data run:
```
python main.py start mongodb
python main.py reimport mongodb
```
The first command will start mongodb only (without the rest of the server). The second command will load non-user related data into the database.

To prepare all components for startup run (while mongodb is still running)
```
python main.py build
```
This will download software for all plugins, build all plugins and create required database indexes.

Finally stop mongodb before starting the platform
```
python main.py start mongodb
```

Startup in development mode:
You need 2 terminals as the calls do not return.
```
# Terminal 1; end with Ctrl-D
python main.py run

# Terminal 2; end with Ctrl-C
cd portal
grunt server
```

To stop the system:
```
python main.py stop
```

Script
------
The main.py python script covers the basic tasks: setup, start, stop and resetting the database to an example state. It's been tested under Ubuntu.

The script is called with
```
python main.py COMMAND [PRODUCT]
```
where ```COMMAND``` is one of
- ```setup```: Downloads the products' binaries, unpacks them and writes the paths in the config files. The only exception is Lighttpd, which is installed via the package manager APT.
- ```start```: Starts the products in production mode. 
- ```run```: Starts the products in development mode. Note: ElasticSearch has an initialization phase before it's completely ready. Activator starts the web application and leaves the user in interactive mode, where log output can be observed and the server is terminated with CTRL+D.
- ```stop```: Stops all products.
- ```reset```: Populates the database and the search server with sample data.
- ```dump```: Dumps the database and search server contents.

Giving a product is optional and will invoke the task only for the specified product, instead of for all. ```PRODUCT``` can be one of ```mongodb```, ```elasticsearch```, ```lighttpd```, ```node```, or ```activator```.

Folder structure
----------------

- apps: Plugins that create/import health records. May connect to external sites but may not query data from the platform.
- config: Template config files for products used by the platform.
- dump: Example database and search server state.
- logs (created by script): Logs of all products except web application (whose log is in platform -> logs).
- platform: Code for the MIDATA platform.
- scripts: Modules used by the main.py script.
- serverjs: Code for the Node.js server.
- ssl-certificate (created by script): Self-signed SSL certificate and Java Keystore.
- visualizations: Plugins that may read and/or create data but may not connect to external sites.


MIDATA portal
==============
The MIDATA portal is located in the portal directory. To install execute:
```
cd portal
npm install
bower install
```

To run the portal locally execute:
```
grunt server
```

To deploy the portal as production instance:
```
grunt deploy
```