#!/bin/bash
mkdir -p instance1
mkdir -p instance2
mkdir -p locks
chmod ugo+rx locks
chmod ugo+X .
chmod go-r . 

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
unzip platform/target/universal/midata-server-1.0-SNAPSHOT.zip -d $instance
sudo cp $instance/nginx/sites-available/* /etc/nginx/sites-available
sed -i 's|RUNDIR|$instance1|' /dev/shm/secret.conf
rm -f instance1/run
rm -f instance2/run
touch $instance/run
sudo chmod -R ugo+r $instance/portal
sudo chmod -R ugo+r $instance/visualizations
sudo chmod -R ugo+X $instance
echo "Instance LOCKED for start..."
touch locks/lock locks/apilock

echo print 'Swapping Midata Server...'
pkill -f java
echo 'Starting new instance...'				
/usr/bin/nohup $instance/bin/midata-server -Dpidfile.path=/dev/shm/play.pid -Dconfig.file=/dev/shm/secret.conf -Dhttp.port=9001 &
echo 'Waiting for startup...'		
sleep 30s
echo 'Fetching Page'
curl 'http://localhost:9001/api/test'
/usr/bin/shred -zun 0 /dev/shm/secret.conf
echo 'Done swapping activator'		
sudo service nginx restart
rm locks/lock locks/apilock
rm $instance/builddir
echo "Instance UNLOCKED..."
