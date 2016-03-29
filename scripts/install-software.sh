#!/bin/bash

set -e

# Add docker repository
apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D	
echo deb https://apt.dockerproject.org/repo ubuntu-trusty main > /etc/apt/sources.list.d/docker.list

# Install software and update:
apt-get -qy update
apt-get -qy install docker-engine linux-generic-lts-vivid beanstalkd
apt-get -qy upgrade

if [[ ! -e /usr/local/bin/docker-compose ]]; then

	curl -L https://github.com/docker/compose/releases/download/1.6.2/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
	chmod +x /usr/local/bin/docker-compose
	
fi

cp /vagrant/scripts/beanstalkd-settings /etc/default/beanstalkd