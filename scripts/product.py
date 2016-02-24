'''
Superclass for products.

@author amarfurt
'''

from abc import ABCMeta, abstractmethod

class Product:
	__metaclass__ = ABCMeta

	@abstractmethod
	def setup(self, version):
		pass

	@abstractmethod
	def build(self):
		pass

	@abstractmethod
	def start(self):
		pass

	@abstractmethod
	def stop(self):
		pass

	@abstractmethod
	def reset(self):
		pass

	@abstractmethod
	def run(self):
		pass