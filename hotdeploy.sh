#!/bin/bash
#
# This file is part of the Open MIDATA Platform.
#
# The Open MIDATA platform is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# any later version.
#
# The Open MIDATA Platform is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
#

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
mkdir $instance/platform
mkdir -p $instance/nginx/sites-available
cp -r platform/conf $instance/platform
cp -r platform/project $instance/platform
cp -r portal/dest $instance/portal
unzip platform/target/universal/midata-server-1.0-SNAPSHOT.zip -d $instance
sed -i 's|RUNDIR|$instance1|' /dev/shm/secret.conf
rm -f instance1/run
rm -f instance2/run
touch $instance/run
sudo chmod -R ugo+r $instance/portal
sudo chmod -R ugo+X $instance
echo "Instance LOCKED for start..."
touch locks/lock locks/apilock
echo print 'Swapping Midata Server...'
pkill -f java
sleep 10s
echo 'Starting new instance...'
rm running
ln -s $instance running		
rm -f nohup.out		
/usr/bin/nohup $instance/midata-server-1.0-SNAPSHOT/bin/midata-server -Dpidfile.path=/dev/shm/play.pid -Dconfig.file=/dev/shm/secret.conf -Dhttp.port=9001 &
echo 'Waiting for startup...'		
sleep 30s
echo 'Fetching Page'
curl 'http://localhost:9001/api/ping'
cat nohup.out
/usr/bin/shred -zun 0 /dev/shm/secret.conf
echo 'Done swapping server'		
sudo service nginx restart
rm locks/lock locks/apilock
rm $instance/builddir
echo "Instance UNLOCKED..."
