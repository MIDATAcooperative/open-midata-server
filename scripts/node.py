'''
Configuration of Node.

@author amarfurt
'''

import os
from product import Product
from command import Command
from sslcert import SSLCertificate

class Node(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'node')
		self.bin = os.path.join(self.base, 'bin', 'node')
		self.code = os.path.join(self.parent, 'serverjs')
		self.ssl = SSLCertificate(self.parent).base

	def setup(self, version):
		print 'Setting up Node.js...'
		print 'Downloading binaries...'
		Command.execute('wget http://nodejs.org/dist/v{0}/node-v{0}-linux-x64.tar.gz'.format(version), self.parent)
		print 'Extracting...'
		Command.execute('tar xzf node-v{0}-linux-x64.tar.gz'.format(version), self.parent)
		print 'Setting symlink...'
		Command.execute('ln -s node-v{0}-linux-x64 node'.format(version), self.parent)
		print 'Setting paths in settings file...'
		with open(os.path.join(self.code, 'settings.js'), 'r') as configFile:
			config = configFile.read()
			config = config.replace('NODE_SSL_SERVER_KEY', os.path.join(self.ssl, 'server.pem'))
			config = config.replace('NODE_SSL_SERVER_CERT', os.path.join(self.ssl, 'server.pem'))
		with open(os.path.join(self.code, 'settings.js'), 'w') as configFile:
			configFile.write(config)
		print 'Cleaning up...'
		Command.execute('rm node-v{0}-linux-x64.tar.gz'.format(version), self.parent)

	def start(self):
		print 'Starting Node...'
		Command.execute('{0} {1} &'.format(self.bin, os.path.join(self.code, 'server.js')), self.parent, 
			redirect=os.path.join(self.parent, 'logs', 'node.log'))

	def stop(self):
		print 'Shutting down Node...'
		Command.execute('pkill node')

	def reset(self):
		pass

	def run(self):
		Node.start(self)