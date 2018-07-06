'''
Configuration of Activator for the Play Framework.

@author amarfurt
'''

import os, getpass, urllib2, time
from product import Product
from command import Command
from sslcert import SSLCertificate
from shutil import copyfile

class Activator(Product):

	def __init__(self, parentDir, buildDir, runDir):
		self.parent = parentDir
		self.runDir = runDir
		self.base = os.path.join(self.parent, 'platform')
		self.bin = os.path.join(self.base, 'sbt')
		self.code = os.path.join(buildDir, 'platform')
		self.stage = os.path.join(self.code, 'target', 'universal', 'stage')
		self.app = os.path.join(self.stage, 'bin', 'hdc')
		self.coderun = os.path.join(runDir, 'platform')
		self.stagerun = os.path.join(self.coderun, 'target', 'universal', 'stage')
		self.apprun = os.path.join(self.stagerun, 'bin', 'hdc')
		self.keystore = os.path.join(SSLCertificate(self.parent, buildDir, runDir).base, 'server.keystore')
		self.mcrypt = '/usr/bin/mcrypt'
		self.shred = '/usr/bin/shred'
		self.conf = os.path.join(self.code, 'conf')

	def setup(self, version):
		pass

	def readconf(self):
		copyfile(os.path.join(self.conf, 'secret.conf.gz.nc'), '/dev/shm/secret.conf.gz.nc')
		Command.execute('{0} /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d'.format(self.mcrypt), self.conf)
		with open('/dev/shm/secret.conf', 'a') as fout:
			with open(os.path.join(self.conf, 'application.conf'),'r') as fi:
				fout.write(fi.read())
		with open('/dev/shm/secret.conf', 'r') as configFile:				
			config = configFile.read()																		
			config = config.replace('RUNDIR', self.runDir)
		with open('/dev/shm/secret.conf', 'w') as configFile:
			configFile.write(config)				
	

	def start(self):
		print 'Starting Play Framework...'		
		# workaround: use the stage task as the start command doesn't work with HTTPS for now...		
		Activator.readconf(self)
		Command.execute('{0} stage'.format(self.bin), self.coderun)
		Command.execute('{0} -Dpidfile.path=/dev/shm/play.pid -Dconfig.file=/dev/shm/secret.conf -Dhttp.port=9001 &'
			.format(self.apprun), redirect=os.path.join(self.parent, 'logs', 'activator.log'))
		print 'Waiting for startup...'		
		time.sleep(30)
		print 'Fetching Page'
		t = urllib2.urlopen('http://localhost:9001/api/test')		
		t.read()		
		t.close()		
		Command.execute('{0} -zun 0 /dev/shm/secret.conf'.format(self.shred), self.conf)
		print 'Done startup activator'

	def hotprepare(self):
		print 'Preparing Activator...'		
		Activator.readconf(self)
		Command.execute('{0} stage'.format(self.bin), self.code)
		print 'Done preparing activator'		

	def hotswap(self):
		print 'Swapping Activator...'
		Command.execute('pkill -f typesafe')
		print 'Starting new instance...'				
		Command.execute('/usr/bin/nohup {0} -Dpidfile.path=/dev/shm/play.pid -Dconfig.file=/dev/shm/secret.conf -Dhttp.port=9001 &'
			.format(self.app), redirect=os.path.join(self.parent, 'logs', 'activator.log'))
		print 'Waiting for startup...'		
		time.sleep(30)
		print 'Fetching Page'
		t = urllib2.urlopen('http://localhost:9001/api/test')		
		t.read()		
		t.close()		
		Command.execute('{0} -zun 0 /dev/shm/secret.conf'.format(self.shred), self.conf)
		print 'Done swapping activator'		

	def run(self):
		print 'Running Activator...'
		password = getpass.getpass("Please enter the password for the Java KeyStore: ")
		Activator.readconf(self)
		Command.execute('{0} run -Dpidfile.path=/dev/shm/play.pid -J-Xverify:none -Dconfig.file=/dev/shm/secret.conf -Dhttp.port=9001 -Dhttps.port=9000 -Dhttps.keyStore={1} -Dhttps.keyStorePassword={2}'
			.format(self.bin, self.keystore, password), self.coderun)
		Command.execute('{0} -zun 0 /dev/shm/secret.conf'.format(self.shred), self.conf)

	def stop(self):
		print 'Shutting down Activator...'
		Command.execute('pkill -f typesafe')

	def build(self):
		pass
	
	def newsecret(self):
		Command.execute('{0} play-generate-secret'.format(os.path.join('..', 'activator', 'activator')), self.code)	    

	def configure(self):
		if (os.path.isfile('/dev/shm/secret.conf')):
			print('Reencrypting configuration file')
			Command.execute('{0} /dev/shm/secret.conf -z -a rijndael-128 -m cbc'.format(self.mcrypt), self.conf)
			copyfile('/dev/shm/secret.conf.gz.nc', os.path.join(self.conf, 'secret.conf.gz.nc'))
			Command.execute('{0} -zun 0 /dev/shm/secret.conf'.format(self.shred), self.conf)												
		elif (os.path.isfile(os.path.join(self.conf, 'secret.conf.gz.nc'))):		
			print('Decrypting existing configuration file')
			copyfile(os.path.join(self.conf, 'secret.conf.gz.nc'), '/dev/shm/secret.conf.gz.nc')
			Command.execute('{0} /dev/shm/secret.conf.gz.nc -z -a rijndael-128 -m cbc -d'.format(self.mcrypt), self.conf)						
			print('Now edit your configuration in /dev/shm/secret.conf and call python main.py configure activator again')
			print('')
		else:						
			copyfile(os.path.join(self.conf, 'secret.conf.template'), '/dev/shm/secret.conf')
			print('Starting new configuration. Edit configuration at /dev/shm/secret.conf then call	python main.py configure activator again')									
			print('')
			print('Please include the above displayed application secret into the configuration file')		

	def reset(self):
		pass
