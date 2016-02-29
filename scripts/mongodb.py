'''
Configuration of MongoDB.

@author amarfurt
'''

import os, StringIO, ConfigParser
from product import Product
from command import Command
from shutil import copyfile

class MongoDB(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'mongodb')
		self.data = os.path.join(self.base, 'data')
		self.bin = os.path.join(self.base, 'bin')
		self.code = os.path.join(self.parent, 'platform')		
		self.mcrypt = '/usr/bin/mcrypt'
		self.shred = '/usr/bin/shred'
		self.conf = os.path.join(self.code, 'conf')		
		
	def readconf(self):
		print 'Read database configuration'
		copyfile(os.path.join(self.conf, 'secret.conf.gz.nc'), '/dev/shm/secret.conf.gz.nc')
		Command.execute('{0} /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d'.format(self.mcrypt), self.conf)
		config = StringIO.StringIO()
		config.write('[dummysection]\n')
		config.write(open('/dev/shm/secret.conf').read())
		config.seek(0, os.SEEK_SET)
		cp = ConfigParser.ConfigParser()
		cp.readfp(config)
		self.user_host = cp.get('dummysection', 'mongo.user.host')
		self.user_port = cp.getint('dummysection', 'mongo.user.port')
		self.user_database = cp.get('dummysection', 'mongo.user.database')
		if (cp.has_option('dummysection', 'mongo.user.username')):
			self.user_username = ' -u '+cp.get('dummysection', 'mongo.user.username')
			self.user_password = ' -p '+cp.get('dummysection', 'mongo.user.password')
		else:
			self.user_username = ''
			self.user_password = ''
								
		self.access_host = cp.get('dummysection', 'mongo.access.host')		
		self.access_port = cp.getint('dummysection', 'mongo.access.port')
		self.access_database = cp.get('dummysection', 'mongo.access.database')		
		if (cp.has_option('dummysection', 'mongo.access.username')):
			self.access_username = ' -u '+cp.get('dummysection', 'mongo.access.username')
			self.access_password = ' -p '+cp.get('dummysection', 'mongo.access.password')
		else:
			self.access_username = ''
			self.access_password = ''
		
		self.record_host = cp.get('dummysection', 'mongo.record.host')		
		self.record_port = cp.getint('dummysection', 'mongo.record.port')
		self.record_database = cp.get('dummysection', 'mongo.record.database')		
		if (cp.has_option('dummysection', 'mongo.record.username')):
			self.record_username = ' -u '+cp.get('dummysection', 'mongo.record.username')
			self.record_password = ' -p '+cp.get('dummysection', 'mongo.record.password')
		else:
			self.record_username = ''
			self.record_password = ''
		
		self.mapping_host = cp.get('dummysection', 'mongo.mapping.host')		
		self.mapping_port = cp.getint('dummysection', 'mongo.mapping.port')
		self.mapping_database = cp.get('dummysection', 'mongo.mapping.database')		
		if (cp.has_option('dummysection', 'mongo.mapping.username')):
			self.mapping_username = ' -u '+cp.get('dummysection', 'mongo.mapping.username')
			self.mapping_password = ' -p '+cp.get('dummysection', 'mongo.mapping.password')
		else:
			self.mapping_username = ''
			self.mapping_password = ''
		Command.execute('{0} -zun 0 /dev/shm/secret.conf'.format(self.shred), self.conf)
		

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

	def build(self):
		MongoDB.readconf(self)
		Command.execute('{0} {5}{6} {2}:{3}/{4} {1}'.format(os.path.join(self.bin, 'mongo'),
			os.path.join(self.parent, 'json', 'mongo-setup-user.js'),
			self.user_host, 
			self.user_port, 
			self.user_database,
			self.user_username,
			self.user_password), self.parent)					 					
		Command.execute('{0} {5}{6} {2}:{3}/{4} {1}'.format(os.path.join(self.bin, 'mongo'),
			os.path.join(self.parent, 'json', 'mongo-setup-access.js'),
			self.access_host, 
			self.access_port, 
			self.access_database,
			self.access_username,
			self.access_password), self.parent)					 					
		Command.execute('{0} {5}{6} {2}:{3}/{4} {1}'.format(os.path.join(self.bin, 'mongo'),
			os.path.join(self.parent, 'json', 'mongo-setup-record.js'),
			self.record_host, 
			self.record_port, 
			self.record_database,
			self.record_username,
			self.record_password), self.parent)								 					

	def start(self):
		print 'Starting MongoDB...'
		Command.execute('{0} --config {1}'.format(os.path.join(self.bin, 'mongod'), 
			os.path.join(self.base, 'mongod.conf')), self.parent)

	def run(self):
		MongoDB.start(self)
		
	def stop(self):
		print 'Shutting down MongoDB...'
		Command.execute('pkill mongod')

	def reset(self):
		MongoDB.readconf(self)		
		print 'Reimporting data from dump...'
		Command.execute('{0} -h {2}:{3} --drop --db {4} {5}{6} {1}'.format(os.path.join(self.bin, 'mongorestore'), 
			os.path.join(self.parent, 'dump', 'mongodb', 'user'),
			self.user_host, 
			self.user_port, 
			self.user_database,
			self.user_username,
			self.user_password), self.parent)			
		Command.execute('{0} -h {2}:{3} --drop --db {4} {5}{6} {1}'.format(os.path.join(self.bin, 'mongorestore'), 
			os.path.join(self.parent, 'dump', 'mongodb', 'access'),
			self.access_host, 
			self.access_port, 
			self.access_database,
			self.access_username,
			self.access_password), self.parent)			
		Command.execute('{0} -h {2}:{3} --drop --db {4} {5}{6} {1}'.format(os.path.join(self.bin, 'mongorestore'), 
			os.path.join(self.parent, 'dump', 'mongodb', 'mapping'),
			self.mapping_host, 
			self.mapping_port, 
			self.mapping_database,
			self.mapping_username,
			self.mapping_password), self.parent)			
		Command.execute('{0} -h {2}:{3} --drop --db {4} {5}{6} {1}'.format(os.path.join(self.bin, 'mongorestore'), 
			os.path.join(self.parent, 'dump', 'mongodb', 'record'),
			self.record_host, 
			self.record_port, 
			self.record_database,
			self.record_username,
			self.record_password), self.parent)			

	def dump(self):
		MongoDB.readconf(self)	
		print 'Dumping database...'		
		Command.execute('{0} -h {2}:{3} --db {4} {5}{6} --out {1}'.format(os.path.join(self.bin, 'mongodump'), 
			os.path.join(self.parent, 'dump', 'mongodb'),
			self.user_host, 
			self.user_port, 
			self.user_database,
			self.user_username,
			self.user_password), self.parent)
		Command.execute('{0} -h {2}:{3} --db {4} {5}{6} --out {1}'.format(os.path.join(self.bin, 'mongodump'), 
			os.path.join(self.parent, 'dump', 'mongodb'),
			self.access_host, 
			self.access_port, 
			self.access_database,
			self.access_username,
			self.access_password), self.parent)
		Command.execute('{0} -h {2}:{3} --db {4} {5}{6} --out {1}'.format(os.path.join(self.bin, 'mongodump'), 
			os.path.join(self.parent, 'dump', 'mongodb'),
			self.mapping_host, 
			self.mapping_port, 
			self.mapping_database,
			self.mapping_username,
			self.mapping_password), self.parent)
		Command.execute('{0} -h {2}:{3} --db {4} {5}{6} --out {1}'.format(os.path.join(self.bin, 'mongodump'), 
			os.path.join(self.parent, 'dump', 'mongodb'),
			self.record_host, 
			self.record_port, 
			self.record_database,
			self.record_username,
			self.record_password), self.parent)
	
	def export(self):
		MongoDB.readconf(self)
		print 'Exporting metadata'
		for f in ['formatgroups','contentinfo','formatinfo','plugins','coding']:
			Command.execute('{0} -h {2}:{3} -d {4} {5}{6} -c {7} -o {1}'.format(os.path.join(self.bin, 'mongoexport'), 
				os.path.join(self.parent, 'json', f + '.json'), 
				self.user_host, 
				self.user_port, 
				self.user_database,
				self.user_username,
				self.user_password,
				f), self.parent)					  

	def reimport(self):
		MongoDB.readconf(self)	
		print 'Importing metadata'
		for f in ['formatgroups','contentinfo','formatinfo','coding']:
			Command.execute('{0} -h {2}:{3} -d {4} {5}{6} -c {7} --file {1} --drop --upsertFields _id'.format(os.path.join(self.bin, 'mongoimport'), 
				os.path.join(self.parent, 'json', f + '.json'),
				self.user_host, 
				self.user_port, 
				self.user_database,
				self.user_username,
				self.user_password,
				f), self.parent)		
		Command.execute('{0} -h {2}:{3} -d {4} {5}{6} -c {7} --file {1} --upsertFields _id'.format(os.path.join(self.bin, 'mongoimport'), 
			os.path.join(self.parent, 'json', 'plugins.json'),
			self.user_host, 
			self.user_port, 
			self.user_database,
			self.user_username,
			self.user_password,
			'plugins'), self.parent)		
		Command.execute('{0} -h {2}:{3} -d {4} {5}{6} -c loinc --type csv --drop --headerline --file {1}'.format(os.path.join(self.bin, 'mongoimport'), 
			os.path.join(self.parent, 'json', 'loinc.csv'),
			self.user_host, 
			self.user_port, 
			self.user_database,
			self.user_username,
			self.user_password), self.parent)		

						