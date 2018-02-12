'''
Installation of portal

@author kreutz
'''

import os, json
from product import Product
from command import Command

class Portal(Product):

	def __init__(self, parentDir, buildDir, runDir):
		self.parent = parentDir	
		self.locdir = buildDir	
		self.portal = os.path.join(self.parent, 'portal')
		self.appconf = os.path.join(self.parent, 'platform', 'conf')					

	def build(self):
		print 'Installing Portal...'		
		Command.execute('npm install', self.portal)
		Command.execute('bower install', self.portal)			
		Command.execute('grunt deploy', self.portal)	
		
	def setup(self):
		print 'Setting up Plattform...'
		
		print 'Reading instance config...'
		with open(os.path.join(self.parent, 'config', 'instance.json'), 'r') as reader:
			instance = json.load(reader, 'utf8')		
						
		print 'Setting paths in config file...'
		clusters = []		
		for ip in instance['cluster']:
			clusters.append('"akka.tcp://midata@' + ip + ':9006"')
		clusterstr = ",".join(clusters)  
		with open(os.path.join(self.appconf, 'application.conf.template'), 'r') as configFile:				
			config = configFile.read()			
			config = config.replace('PORTAL_ORIGIN', instance['portal']['origin'])
			config = config.replace('PLUGINS_SERVER', instance['portal']['plugins'])
			config = config.replace('DOMAIN', instance['domain'])
			config = config.replace('CERTIFICATE_PEM', instance['certificate']['pem'])
			config = config.replace('CERTIFICATE_KEY', instance['certificate']['key'])			
			config = config.replace('NODE_INTERNAL_PORT', instance['node']['port'])
			config = config.replace('PLATFORM_INTERNAL_PORT', instance['platform']['port'])
			config = config.replace('PLATFORM_HOSTNAME', instance['platform']['hostname'])
			config = config.replace('CLUSTER_SERVER', clusterstr)
			config = config.replace('INSTANCETYPE', instance['instanceType'])
			config = config.replace('DEFAULT_LANGUAGE', instance['portal']['defaultLanguage'])
			config = config.replace('ROOTDIR', self.parent)															
		with open(os.path.join(self.appconf, 'application.conf'), 'w') as configFile:
			configFile.write(config)
	
	def start(self):
		pass

	def stop(self):
		pass

	def reset(self):
		pass
		
	def run(self):
		pass
	
