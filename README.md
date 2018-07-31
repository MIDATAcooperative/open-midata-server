MIDATA COOP 
=============================

Manage, understand, and leverage your health data.

This repository contains the MIDATA application server.
For a full installation you will need to checkout the "server-tools" repository instead.


Development Installation (localhost)
===========================

Install git and make:
```
sudo apt-get install git make
```

Clone the MIDATA plugins repository into a directory of your choice

Then clone this repository into a directory of your choice. 
```
git clone https://github.com/MIDATAcooperative/midata-plugins
git clone https://github.com/MIDATAcooperative/platform-private
cd platform-private
```

Run the installation:
```
make install-local
```
An editor will open where you can configure the instance.

Afterwards a second editor will open where you have to configure two pathes:
- PLUGINS_DIR : Where is the MIDATA plugins directory checked out?
- CERTIFICATE_DIR : Where should certificates be placed?

Starting the instance in development mode:
------
Open two terminals.
Terminal 1:
```
cd platform-private
make update
```

Terminal 2: (after Terminal 1 is ready)
```
cd platform-private/portal
grunt server
```

The local server will be at address: https://localhost:9002
On the developer login page you can log in as "admin@midata.coop" with initial password "secret".


Folder structure
----------------

- conf: Config files used by the platform.
- logs (created by script): Logs of all products except web application (whose log is in platform -> logs).
- platform: Code for the MIDATA platform.
- json: JSON data and scripts for the database.
