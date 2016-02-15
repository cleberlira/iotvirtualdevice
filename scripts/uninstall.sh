#!/bin/bash

printf "Uninstalling IoT Virtual Devices of boot of system...\n\n\n"

if [ $(id -u) -ne 0 ]; then
  printf "Script must be run as root.\n"
  exit 1
fi

rm -rf /usr/share/iot_virtual_devices

update-rc.d -f iot_virtual_devices remove

rm /etc/init.d/iot_virtual_devices
