'''
Configuration of Activator for the Play Framework.

@author amarfurt
'''

import os, getpass
from product import Product
from command import Command
from sslcert import SSLCertificate
from shutil import copyfile

class Activator(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'activator')
		self.bin = os.path.join(self.base, 'activator')
		self.code = os.path.join(self.parent, 'platform')
		self.stage = os.path.join(self.code, 'target', 'universal', 'stage')
		self.app = os.path.join(self.stage, 'bin', 'hdc')
		self.keystore = os.path.join(SSLCertificate(self.parent).base, 'server.keystore')
		self.mcrypt = '/usr/bin/mcrypt'
		self.shred = '/usr/bin/shred'
		self.conf = os.path.join(self.code, 'conf')

	def setup(self, version):
		print 'Setting up Activator...'
		print 'Downloading binaries...'
		Command.execute('wget http://downloads.typesafe.com/typesafe-activator/{0}/typesafe-activator-{0}-minimal.zip'
			.format(version), self.parent)
		print 'Extracting...'
		Command.execute('unzip typesafe-activator-{0}-minimal.zip'.format(version), self.parent)
		print 'Setting symlink...'
		Command.execute('ln -s activator-{0}-minimal activator'.format(version), self.parent)
		print 'Cleaning up...'
		Command.execute('rm typesafe-activator-{0}-minimal.zip'.format(version), self.parent)

	def start(self):
		print 'Starting Activator...'
		password = getpass.getpass("Please enter the password for the Java KeyStore: ")
		# workaround: use the stage task as the start command doesn't work with HTTPS for now...		
		copyfile(os.path.join(self.conf, 'secret.conf.gz.nc'), '/dev/shm/secret.conf.gz.nc')
		Command.execute('{0} /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d'.format(self.mcrypt), self.conf)
		Command.execute('{0} stage'.format(self.bin), self.code)
		Command.execute('{0} -Dhttp.port=9001 -Dhttps.keyStore={1} -Dhttps.keyStorePassword={2} &'
			.format(self.app, self.keystore, password), redirect=os.path.join(self.parent, 'logs', 'activator.log'))
		Command.execute('{0} -zun 0 /dev/shm/secret.conf'.format(self.shred), self.conf)

	def run(self):
		print 'Running Activator...'
		password = getpass.getpass("Please enter the password for the Java KeyStore: ")
		copyfile(os.path.join(self.conf, 'secret.conf.gz.nc'), '/dev/shm/secret.conf.gz.nc')
		Command.execute('{0} /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d'.format(self.mcrypt), self.conf)		
		Command.execute('{0} run -Dhttp.port=9001 -Dhttps.port=9000 -Dhttps.keyStore={1} -Dhttps.keyStorePassword={2}'
			.format(self.bin, self.keystore, password), self.code)
		Command.execute('{0} -zun 0 /dev/shm/secret.conf'.format(self.shred), self.conf)

	def stop(self):
		print 'Shutting down Activator...'
		Command.execute('pkill -f typesafe')

	def build(self):
		pass

	def configure(self):
		if (os.path.isfile('/dev/shm/secret.conf')):
			print('Reencrypting configuration file')
			Command.execute('{0} /dev/shm/secret.conf -z -a rijndael-128 -m cbc'.format(self.mcrypt), self.conf)
			copyfile('/dev/shm/secret.conf.gz.nc', os.path.join(self.conf, 'secret.conf.gz.nc'))
			Command.execute('{0} -zun 0 /dev/shm/secret.conf'.format(self.shred), self.conf)												
		elif (os.path.isfile(os.path.join(self.conf, 'secret.conf.gz.nc'))):
			print('Decrypting existing configuration file')
			copyfile(os.path.join(self.conf, 'secret.conf.gz.nc'), '/dev/shm/secret.conf.gz.nc')
			Command.execute('{0} /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d'.format(self.mcrypt), self.conf)			
			print('Now edit your configuration in /dev/shm/secret.conf and call python main.py configure activator again')
			print('')
		else:
			Command.execute('{0} play-generate-secret'.format(os.path.join('..', 'activator', 'activator')), self.code)
			print('Starting new configuration. Edit configuration at /dev/shm/secret.conf then call	python main.py configure activator again')
			copyfile(os.path.join(self.conf, 'secret.conf.template'), '/dev/shm/secret.conf')			
			print('')
			print('Please include the above displayed application secret into the configuration file')		

	def reset(self):
		pass
