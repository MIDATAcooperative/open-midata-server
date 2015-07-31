'''
Configuration of MongoDB.

@author amarfurt
'''

import os
from product import Product
from command import Command

class MongoDB(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'mongodb')
		self.data = os.path.join(self.base, 'data')
		self.bin = os.path.join(self.base, 'bin')

	def setup(self, version):
		print 'Setting up MongoDB...'
		print 'Downloading binaries...'
		Command.execute('wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-{0}.tgz'.format(version), self.parent)
		print 'Extracting...'
		Command.execute('tar xzf mongodb-linux-x86_64-{0}.tgz'.format(version), self.parent)
		print 'Setting symlink...'
		Command.execute('ln -s mongodb-linux-x86_64-{0} mongodb'.format(version), self.parent)
		print 'Creating required folders...'
		if not os.path.exists(self.data):
			os.mkdir(self.data)
		print 'Writing config file...'
		with open(os.path.join(self.parent, 'config', 'mongod.conf'), 'r') as configFile:
			config = configFile.read()
			config = config.replace('MONGODB_DATA_PATH', self.data)
			config = config.replace('MONGODB_LOG_PATH', os.path.join(self.parent, 'logs', 'mongod.log'))
		with open(os.path.join(self.base, 'mongod.conf'), 'w') as configFile:
			configFile.write(config)
		print 'Cleaning up...'
		Command.execute('rm mongodb-linux-x86_64-{0}.tgz'.format(version), self.parent)

	def start(self):
		print 'Starting MongoDB...'
		Command.execute('{0} --config {1}'.format(os.path.join(self.bin, 'mongod'), 
			os.path.join(self.base, 'mongod.conf')), self.parent)

	def stop(self):
		print 'Shutting down MongoDB...'
		Command.execute('pkill mongod')

	def reset(self):
		print 'Reimporting data from dump...'
		Command.execute('{0} --drop --db user {1}'.format(os.path.join(self.bin, 'mongorestore'), 
			os.path.join(self.parent, 'dump', 'mongodb', 'user')), self.parent)
		Command.execute('{0} --drop --db access {1}'.format(os.path.join(self.bin, 'mongorestore'), 
			os.path.join(self.parent, 'dump', 'mongodb', 'access')), self.parent)
		Command.execute('{0} --drop --db mapping {1}'.format(os.path.join(self.bin, 'mongorestore'), 
			os.path.join(self.parent, 'dump', 'mongodb', 'mapping')), self.parent)
		Command.execute('{0} --drop --db record {1}'.format(os.path.join(self.bin, 'mongorestore'), 
			os.path.join(self.parent, 'dump', 'mongodb', 'record')), self.parent)									

	def dump(self):
		print 'Dumping database...'
		Command.execute('{0} --db user --out {1}'.format(os.path.join(self.bin, 'mongodump'), 
			os.path.join(self.parent, 'dump', 'mongodb')), self.parent)
		Command.execute('{0} --db access --out {1}'.format(os.path.join(self.bin, 'mongodump'), 
			os.path.join(self.parent, 'dump', 'mongodb')), self.parent)
		Command.execute('{0} --db mapping --out {1}'.format(os.path.join(self.bin, 'mongodump'), 
			os.path.join(self.parent, 'dump', 'mongodb')), self.parent)
		Command.execute('{0} --db record --out {1}'.format(os.path.join(self.bin, 'mongodump'), 
			os.path.join(self.parent, 'dump', 'mongodb')), self.parent)
	
	def export(self):
	   print 'Exporting metadata'
	   Command.execute('{0} -d user -c formatgroups -o {1}'.format(os.path.join(self.bin, 'mongoexport'), 
			os.path.join(self.parent, 'json', 'formatgroups.json')), self.parent)		
	   Command.execute('{0} -d user -c contentinfo -o {1}'.format(os.path.join(self.bin, 'mongoexport'), 
			os.path.join(self.parent, 'json', 'contentinfo.json')), self.parent)
	   Command.execute('{0} -d user -c formatinfo -o {1}'.format(os.path.join(self.bin, 'mongoexport'), 
			os.path.join(self.parent, 'json', 'formatinfo.json')), self.parent)
	   Command.execute('{0} -d user -c visualizations -o {1}'.format(os.path.join(self.bin, 'mongoexport'), 
			os.path.join(self.parent, 'json', 'visualizations.json')), self.parent)
	   Command.execute('{0} -d user -c apps -o {1}'.format(os.path.join(self.bin, 'mongoexport'), 
			os.path.join(self.parent, 'json', 'apps.json')), self.parent)

	def reimport(self):
	   print 'Importing metadata'
	   Command.execute('{0} -d user -c formatgroups --file {1} --upsertFields _id'.format(os.path.join(self.bin, 'mongoimport'), 
			os.path.join(self.parent, 'json', 'formatgroups.json')), self.parent)		
	   Command.execute('{0} -d user -c contentinfo --file {1} --upsertFields _id'.format(os.path.join(self.bin, 'mongoimport'), 
			os.path.join(self.parent, 'json', 'contentinfo.json')), self.parent)
	   Command.execute('{0} -d user -c formatinfo --file {1} --upsertFields _id'.format(os.path.join(self.bin, 'mongoimport'), 
			os.path.join(self.parent, 'json', 'formatinfo.json')), self.parent)
	   Command.execute('{0} -d user -c visualizations --file {1} --upsertFields _id'.format(os.path.join(self.bin, 'mongoimport'), 
			os.path.join(self.parent, 'json', 'visualizations.json')), self.parent)
	   Command.execute('{0} -d user -c apps --file {1} --upsertFields _id'.format(os.path.join(self.bin, 'mongoimport'), 
			os.path.join(self.parent, 'json', 'apps.json')), self.parent)