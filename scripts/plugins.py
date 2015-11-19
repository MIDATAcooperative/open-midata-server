'''
Installation of plugins

@author kreutz
'''

import os
from product import Product
from command import Command

class Plugins(Product):

	def __init__(self, parentDir):
		self.parent = parentDir		
		self.apps = os.path.join(self.parent, 'apps')
		self.plugins = os.path.join(self.parent, 'visualizations')		

	def setup(self):
		print 'Installing Apps...'		
		Command.execute('for f in *; do cd $f; [ -e "package.json" ] && npm install; cd ..; done', self.apps)
		Command.execute('for f in *; do cd $f; [ -e "bower.json" ] && bower install; cd ..; done', self.apps)
		print 'Installing Plugins...'		
		Command.execute('for f in *; do cd $f; [ -e "package.json" ] && npm install; cd ..; done', self.plugins)
		Command.execute('for f in *; do cd $f; [ -e "bower.json" ] && bower install; cd ..; done', self.plugins)						
		print 'Building Apps...'		
		Command.execute('for f in *; do cd $f; [ -e "Gruntfile.js" ] && grunt build; cd ..; done', self.apps)
		print 'Building Plugins...'		
		Command.execute('for f in *; do cd $f; [ -e "Gruntfile.js" ] && grunt build; cd ..; done', self.plugins)
		
	def start(self):
		pass

	def stop(self):
		pass

	def reset(self):
		pass
		
	def run(self):
		pass
	