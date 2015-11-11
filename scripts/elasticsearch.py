'''
Configuration of ElasticSearch.

@author amarfurt
'''

import os, shutil
from product import Product
from command import Command

class ElasticSearch(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'elasticsearch')
		self.data = os.path.join(self.base, 'data')
		self.bin = os.path.join(self.base, 'bin')

	def setup(self, version):
		print 'Setting up ElasticSearch...'
		print 'Downloading binaries...'
		Command.execute('wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-{0}.tar.gz'
			.format(version), self.parent)
		print 'Extracting...'
		Command.execute('tar xzf elasticsearch-{0}.tar.gz'.format(version), self.parent)
		print 'Setting symlink...'
		Command.execute('ln -s elasticsearch-{0} elasticsearch'.format(version), self.parent)
		print 'Creating required folders...'
		if not os.path.exists(self.data):
			os.mkdir(self.data)
		print 'Setting paths in config file...'
		with open(os.path.join(self.parent, 'config', 'elasticsearch.yml'), 'r') as configFile:
			config = configFile.read()
			config = config.replace('ELASTICSEARCH_DATA_PATH', self.data)
			config = config.replace('ELASTICSEARCH_LOG_PATH', os.path.join(self.parent, 'logs'))
		with open(os.path.join(self.base, 'config', 'elasticsearch.yml'), 'w') as configFile:
			configFile.write(config)
		print 'Copying file with default mapping...'
		shutil.copy(os.path.join(self.parent, 'config', 'default-mapping.json'), os.path.join(self.base, 'config', 'default-mapping.json'))
		print 'Cleaning up...'
		Command.execute('rm elasticsearch-{0}.tar.gz'.format(version), self.parent)

	def start(self):
		print 'Starting ElasticSearch...'
		Command.execute('{0} -d; sleep 1; {0} -d'.format(os.path.join(self.bin, 'elasticsearch')), self.parent)

	def run(self):
		ElasticSearch.start(self)

	def stop(self):
		print 'Shutting down ElasticSearch...'
		Command.execute('pkill -f elasticsearch')

	def reset(self):
		print 'Reimporting data from dump...'
		# Setting the snapshot repository
		Command.execute('curl -XPUT \'localhost:9200/_snapshot/dump\'' + 
			' -d \'{{ "type": "fs", "settings": {{ "location": "{0}" }} }}\''.format(os.path.join(self.parent, 'dump', 'elasticsearch')))
		# Closing all indices
		Command.execute('curl -XPOST localhost:9200/_all/_close')
		# Restoring the snapshot with the name 'snapshot'
		Command.execute('curl -XPOST localhost:9200/_snapshot/dump/snapshot/_restore')

	def dump(self):
		print 'Issuing dump command (async)...'
		Command.execute('curl -XPUT localhost:9200/_snapshot/dump/snapshot')
