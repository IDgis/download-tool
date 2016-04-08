#!/bin/bash

echo starting container...

if [ -z ${ZIP_CACHEPATH} ]; then
	echo ZIP_CACHEPATH environment variable is missing
	exit 1
fi

echo using ZIP_CACHEPATH: ${ZIP_CACHEPATH}

mkfifo /opt/fifo
# tigger 'tail -f' to open fifo
echo logging started... > /opt/fifo &

echo "00 5 * * * root /opt/gc.sh > /opt/fifo 2>&1" > /etc/crontab

echo starting cron...

cron
tail -f /opt/fifo
