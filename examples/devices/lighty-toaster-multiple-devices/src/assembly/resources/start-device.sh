#!/bin/bash

# Copyright (c) 2021 PANTHEON.tech s.r.o. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# The script accepts parameters specifying the listening port, the number of devices and the number of threads for the NETCONF server.
# Parameters are optional. If they are not used, the default value is used.
# --device-count DEVICES-COUNT (Default 1) Number of simulated netconf devices to spin. This is the number of actual ports which will be used for the devices.
# --starting-port STARTING-PORT (Default 17380) First port for simulated device. Each other device will use incremented port number.
# --thread-pool-size THREAD-POOL-SIZE (Default 8) The number of threads to keep in the pool, when creating a device simulator, even if they are idle.
# ./start-device --starting-port <STARTING_PORT> --device-count <DEVICE_COUNT> --thread-pool-size <THREAD_POOL_SIZE>
# ./start-device --starting-port 20000 --device-count 200 --thread-pool-size 200
#

CLASSPATH=lighty-toaster-multiple-devices-15.4.0-SNAPSHOT.jar

ARGUMENT_LIST=(
    "device-count"
    "starting-port"
    "thread-pool-size"
)

deviceCount=1
startingPort=17830
threadPoolSize=8

# read arguments
opts=$(getopt \
    --longoptions "$(printf "%s:," "${ARGUMENT_LIST[@]}")" \
    --name "$(basename "$0")" \
    --options "" \
    -- "$@"
)

eval set --$opts

while [[ $# -gt 0 ]]; do
    case "$1" in
        --device-count)
            deviceCount=$2
            shift 2
            ;;

        --starting-port)
            startingPort=$2
            shift 2
            ;;

        --thread-pool-size)
            threadPoolSize=$2
            shift 2
            ;;

        *)
            break
            ;;
    esac
done

for jar in `ls -1 lib/`;
do
   CLASSPATH=$CLASSPATH:lib/$jar
done

java -server -Xms16M -Xmx40M -XX:MaxMetaspaceSize=40m -classpath $CLASSPATH io.lighty.netconf.device.toaster.Main --starting-port $startingPort --thread-pool-size $threadPoolSize --device-count $deviceCount
