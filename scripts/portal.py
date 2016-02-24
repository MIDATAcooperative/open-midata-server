'''
Installation of portal

@author kreutz
'''

import os
from product import Product
from command import Command

class Portal(Product):

	def __init__(self, parentDir):
		self.parent = parentDir		
		self.portal = os.path.join(self.parent, 'portal')			

	def build(self):
		print 'Installing Portal...'		
		Command.execute('npm install', self.portal)
		Command.execute('bower install', self.portal)			
		Command.execute('grunt deploy', self.portal)	
		
	def setup(self):
		pass
	
	def start(self):
		pass

	def stop(self):
		pass

	def reset(self):
		pass
		
	def run(self):
		pass
	