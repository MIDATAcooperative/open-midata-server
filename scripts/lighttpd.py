'''
Configuration of Lighttpd.

@author amarfurt
'''

import os, platform
from product import Product
from command import Command
from sslcert import SSLCertificate

class Lighttpd(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'lighttpd')
		self.logs = os.path.join(self.parent, 'logs')
		self.ssl = SSLCertificate(self.parent).base

	def setup(self, version):
		print 'Setting up Lighttpd...'
		if 'Linux' != platform.system():
			print '[ERROR]: Different architecture from Linux detected.'
			print '[ERROR]: Please install Lighttpd version {0} manually.'.format(version)
		else:
			print 'Installing Lighttpd over package management system...'
			Command.execute('sudo apt-get install lighttpd')
		print 'Creating folder...'
		if not os.path.exists(self.base):
			os.mkdir(self.base)
		print 'Setting paths in config file...'
		with open(os.path.join(self.parent, 'config', 'lighttpd-apps.conf'), 'r') as configFile:
			config = configFile.read()
			config = config.replace('LIGHTTPD_DOCUMENT_PATH', os.path.join(self.parent, 'apps'))
			config = config.replace('LIGHTTPD_ERRORLOG_PATH', os.path.join(self.logs, 'lighttpd-apps.log'))
			config = config.replace('LIGHTTPD_PID_FILE_PATH', os.path.join(self.logs, 'lighttpd-apps.pid'))
			config = config.replace('LIGHTTPD_SSL_PEM_FILE_PATH', os.path.join(self.ssl, 'server.pem'))
		with open(os.path.join(self.base, 'lighttpd-apps.conf'), 'w') as configFile:
			configFile.write(config)
		with open(os.path.join(self.parent, 'config', 'lighttpd-visualizations.conf'), 'r') as configFile:
			config = configFile.read()
			config = config.replace('LIGHTTPD_DOCUMENT_PATH', os.path.join(self.parent, 'visualizations'))
			config = config.replace('LIGHTTPD_ERRORLOG_PATH', os.path.join(self.logs, 'lighttpd-visualizations.log'))
			config = config.replace('LIGHTTPD_PID_FILE_PATH', os.path.join(self.logs, 'lighttpd-visualizations.pid'))
			config = config.replace('LIGHTTPD_SSL_PEM_FILE_PATH', os.path.join(self.ssl, 'server.pem'))
		with open(os.path.join(self.base, 'lighttpd-visualizations.conf'), 'w') as configFile:
			configFile.write(config)

	def start(self):
		print 'Skipping Lighttpd for apps and visualizations...'

	def startdevelop(self):
		print 'Starting Lighttpd for apps...'
		Command.execute('lighttpd -f ' + os.path.join(self.base, 'lighttpd-apps.conf'), self.parent)
		print 'Starting Lighttpd for visualizations...'
		Command.execute('lighttpd -f ' + os.path.join(self.base, 'lighttpd-visualizations.conf'), self.parent)

	def stop(self):
		print 'Shutting down Lighttpd...'
		Command.execute('pkill lighttpd')

	def reset(self):
		pass
