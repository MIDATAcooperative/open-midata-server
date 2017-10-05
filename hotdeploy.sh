#!/bin/bash
mkdir -p instance1
mkdir -p instance2

if [ -f ./instance1/run ]
  then 
    instance="instance2"
  else 
    instance="instance1"
fi
echo "This build will go into: ${instance}"
rm -rf $instance
mkdir $instance
touch $instance/builddir
mkdir $instance/portal
mkdir $instance/visualizations
mkdir $instance/platform
mkdir -p $instance/nginx/sites-available
cp -r platform/conf $instance/platform
cp -r platform/project $instance/platform
cp -r scripts $instance
cd $instance
ln -s ../../platform/app platform/app
ln -s ../activator activator
cd .. 

#python main.py build portal
#python main.py build plugins
#mv portal/dest $instance/portal/dest

find visualizations/* -maxdepth 0 -print0 | xargs -0 -I hello mkdir $instance/hello
find visualizations -maxdepth 2 -name dist -print0 | xargs -0 -Ihello cp -r hello $instance/hello
cp -r portal/dest $instance/portal

python main.py setup nginx
python main.py hotprepare activator
sudo cp $instance/nginx/sites-available/* /etc/nginx/sites-available
rm -f instance1/run
rm -f instance2/run
touch $instance/run
echo "Instance LOCKED for start..."
touch lock apilock
python main.py hotswap activator
sudo service nginx restart
rm lock apilock
rm $instance/builddir
echo "Instance UNLOCKED..."
