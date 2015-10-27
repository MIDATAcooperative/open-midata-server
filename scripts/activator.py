'''
Configuration of Activator for the Play Framework.

@author amarfurt
'''

import os, getpass
from product import Product
from command import Command
from sslcert import SSLCertificate

class Activator(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'activator')
		self.bin = os.path.join(self.base, 'activator')
		self.code = os.path.join(self.parent, 'platform')
		self.stage = os.path.join(self.code, 'target', 'universal', 'stage')
		self.app = os.path.join(self.stage, 'bin', 'hdc')
		self.keystore = os.path.join(SSLCertificate(self.parent).base, 'server.keystore')

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
		Command.execute('{0} stage'.format(self.bin), self.code)
		Command.execute('{0} -Dhttp.port=9001 -Dhttps.keyStore={1} -Dhttps.keyStorePassword={2} &'
			.format(self.app, self.keystore, password), redirect=os.path.join(self.parent, 'logs', 'activator.log'))

	def run(self):
		print 'Running Activator...'
		password = getpass.getpass("Please enter the password for the Java KeyStore: ")
		Command.execute('{0} run -Dhttp.port=9001 -Dhttps.port=9000 -Dhttps.keyStore={1} -Dhttps.keyStorePassword={2}'
			.format(self.bin, self.keystore, password), self.code)

	def stop(self):
		print 'Shutting down Activator...'
		Command.execute('pkill -f typesafe')

	def reset(self):
		pass
