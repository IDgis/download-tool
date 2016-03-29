#!/bin/bash

set -e

# Configure the docker daemon:
service docker stop

cp /vagrant/scripts/docker-settings /etc/default/docker
rm -fr /var/lib/docker/devicemapper

adduser vagrant docker

service docker start
