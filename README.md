Open MIDATA Server
==================

The Open MIDATA Server was developed by ETH Zurich and Bern University of Applied Sciences BFH. 

The platform allows citizens to collect their health data and to freely decide upon data use in 
research and data projects.

Please refer to the instructions provided for setting up a development instance.

Disclaimer
==========

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT 
OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Installation (localhost)
===========================

Install git and make:
```
sudo apt-get install git make
```

Then clone this repository into a directory of your choice. 
```
git clone https://github.com/MIDATAcooperative/open-midata-server
cd open-midata-server
```

Prepare an empty directory where you want to have the plugins
```
mkdir plugins
```

Prepare an empty directory where you want to have SSL certificates installed
```
mkdir ssl
```

Configure the installer
```
make config
```
An editor will open where you can configure the instance.

Afterwards a second editor will open where you have to configure two pathes:
- PLUGINS_DIR : Where will MIDATA plugins be located?
- CERTIFICATE_DIR : Where should certificates be placed?
You will have to change the two pathes to the directories you have created before.

Repeat the configuration step until you are satisfied with the settings.
The pathes have to be correct before starting the next step.

Run the installation:
```
make install
```
Please answer all questions during installation.

As part of the installation two certificates will be generated:
The first is a self signed certificate for operating the server.

The second one is for issuing client certificates.

After the installation has successfully completed you can start the server in development mode.
 

Starting the instance in development mode:
------
Open two terminals.
Terminal 1:
```
make update
```

Terminal 2: (after Terminal 1 is ready)
```
cd portal
npm run dev:server
```

The local server will be at address: https://localhost:9002
On the developer login page you can log in as "admin@example.com" with initial password "secret".
Please use this account to register new administrator accounts. 
When done set the status of the "admin@example.com" user to "BLOCKED" and never use it again.

Folder structure
----------------

- conf: Config files used by the platform.
- config: Config files for external components
- platform: Code for the open MIDATA server.
- portal: Code for the portal of the open MIDATA server
- json: JSON data and scripts for the database.
- nginx: Templates for nginx configuration
- apps: some additional tools for the server

- switches: Used by build process
- tasks: Used by build process

