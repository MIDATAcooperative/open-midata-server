'''
Configuration of NGinx.

@author kreutz
'''

import os, json, platform
from product import Product
from command import Command
from sslcert import SSLCertificate

class Nginx(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.source = os.path.join(self.parent, 'nginx', 'templates')
		self.target = os.path.join(self.parent, 'nginx', 'sites-available')	


	def setup(self):
		print 'Setting up Nginx...'
		
		print 'Reading instance config...'
		with open(os.path.join(self.parent, 'config', 'instance.json'), 'r') as reader:
			instance = json.load(reader, 'utf8')		
		print 'Creating folder...'
		if not os.path.exists(self.target):
			os.mkdir(self.target)						
		print 'Setting paths in config file...'
		for f in ['plugins', 'portal_api', 'webpages']:			
			with open(os.path.join(self.source, f), 'r') as configFile:				
				config = configFile.read()			
				config = config.replace('DOMAIN', instance['domain'])
				config = config.replace('CERTIFICATE_PEM', instance['certificate']['pem'])
				config = config.replace('CERTIFICATE_KEY', instance['certificate']['key'])
				config = config.replace('DHPARAMS', instance['certificate']['dhparams'])				
				config = config.replace('NODE_INTERNAL_PORT', instance['node']['port'])
				config = config.replace('PLATFORM_INTERNAL_PORT', instance['platform']['port'])
				config = config.replace('ROOTDIR', self.parent)												
			with open(os.path.join(self.target, instance['name']+'_'+f), 'w') as configFile:
				configFile.write(config)
		with open(os.path.join(self.source, 'sslredirect'), 'r') as configFile:
			config = configFile.read()
		with open(os.path.join(self.target, 'sslredirect'), 'w') as configFile:
			configFile.write(config)		

	def build(self):
		pass

	def start(self):
		pass

	def run(self):
		pass

	def stop(self):
		pass

	def reset(self):
		pass
