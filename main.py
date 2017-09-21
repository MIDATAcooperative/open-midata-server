'''
Created on Jun 25, 2014

Main file from where the different scripts are called.

@author amarfurt
'''

import os, sys, json, importlib

def instantiate(modules, name, *args):
	''' Imports the module and instantiates the class '''
	mod = importlib.import_module(modules[name]['module'])
	cls = getattr(mod, modules[name]['class'])
	return cls(*args)

def main(command, product=None):
	# base directory is project home (current working directory)
	baseDir = os.getcwd()
	runDir = baseDir
	buildDir = baseDir
		
	if os.path.isfile(os.path.join(baseDir, 'instance1', 'run')):
		runDir = os.path.join(baseDir, 'instance1')
	if os.path.isfile(os.path.join(baseDir, 'instance2', 'run')):
		runDir = os.path.join(baseDir, 'instance2')
	if os.path.isfile(os.path.join(baseDir, 'instance1', 'builddir')):
		buildDir = os.path.join(baseDir, 'instance1')
	if os.path.isfile(os.path.join(baseDir, 'instance2', 'builddir')):
		buildDir = os.path.join(baseDir, 'instance2')
	print "base="+baseDir
	print "build="+buildDir
	print "run="+runDir

	# get used modules
	with open('scripts/modules.json', 'r') as reader:
		modules = json.load(reader, 'utf8')

	# load specified (otherwise all) modules and instantiate respective classes
	instances = []
	if product in modules:
		instances.append((product, instantiate(modules, product, baseDir, buildDir, runDir)))
	else:
		# append instances in correct starting order
		for productName in sorted(modules, key=lambda name: modules[name]['startOrder']):
			instances.append((productName, instantiate(modules, productName, baseDir, buildDir, runDir)))

	# possibly execute general operations
	args = []
	if command == 'setup':
		# create logs directory
		logDir = os.path.join(baseDir, 'logs')
		if not os.path.exists(logDir):
			os.mkdir(logDir)

		# get product versions
		with open('scripts/versions.json', 'r') as reader:
			versions = json.load(reader, 'utf8')
			args.append(versions)

	# execute given method for each instance
	for productName, instance in instances:
		try:
			method = getattr(instance, command)
		except AttributeError:
			print "[ERROR]: Class '" + instance.__class__.__name__ + "'' has no method '" + command + "'."
			continue
		else:
			curArgs = [arg[productName] for arg in args if productName in arg]
			try:
				method(*curArgs)
			except:
				print "[ERROR]: '" + command + "' failed for '" + productName + "'."
				continue

main(*sys.argv[1:])