'''
Create a self-signed SSL certificate.

@author amarfurt
'''

import os
from product import Product
from command import Command

class SSLCertificate(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'ssl-certificate')

	def setup(self):
		print 'Creating self-signed SSL certificate...'
		if os.path.exists(self.base):
			print "The directory '" + os.path.basename(self.base) + "' already exists. Exiting..."
			return
		os.mkdir(self.base)
		print 'Generating the Java KeyStore together with the key pair...' # for Play Framework (Activator)
		Command.execute('keytool -genkeypair -alias hdc -validity 365 -keyalg RSA -keystore server.keystore', self.base)
		print 'Creating PKCS#12-formatted keystore...' # intermediate step
		Command.execute('keytool -importkeystore -deststoretype PKCS12 -srckeystore server.keystore -destkeystore server.p12', self.base)
		print 'Creating PEM-formatted version of private key and certificate...' # for Lighttpd and Node.js
		Command.execute('openssl pkcs12 -nodes -in server.p12 -out server.pem', self.base)

	def build(self):
		pass

	def start(self):
		pass

	def stop(self):
		pass

	def reset(self):
		pass
		
	def run(self):
		pass
	