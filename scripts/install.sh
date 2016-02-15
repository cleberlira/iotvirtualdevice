#!/bin/bash

DIR=/usr/share/iot_virtual_devices

printf "Installing IoT Virtual Devices in boot of system...\n\n\n"

if [ $(id -u) -ne 0 ]; then
  printf "Script must be run as root.\n"
  exit 1
fi

mkdir -p $DIR
ln -s ../config.properties `echo $DIR`/config.properties
cp ../target/IoT_VirtualDevice-0.0.1-SNAPSHOT-jar-with-dependencies.jar `echo $DIR`/iot_virtual_devices.jar


cp iot_virtual_devices /etc/init.d
chmod +x /etc/init.d/iot_virtual_devices

update-rc.d iot_virtual_devices defaults



