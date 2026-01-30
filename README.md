# lighty.io NETCONF Device Simulator (& Libraries)

A lightweight project that provides:
- **NETCONF Device Libraries** for building custom NETCONF devices
- **Examples of NETCONF Device Simulators** to demonstrate, how these libraries can be used to create custom NETCONF devices

## NETCONF Device Libraries
`lighty-netconf-device` is a NETCONF device library for creating
custom NETCONF devices. With the provided NETCONF device builder,
you can create your own device. This device can be built with the use of builder switches for
adding custom YANG models, custom request processors & more.

## Build & Run
* Build the project with Java 21:
```
mvn clean install
```
* The NETCONF Device Library build is located at:

`lighty-netconf-device\target\lighty-netconf-device-23.0.0.jar`

* The build & run procedures for the example devices are described in each device's README.

## Example NETCONF Device Simulators
This tool contains 5 device examples, to demonstrate the usage of the NETCONF Device Library for creating custom devices:
- [**lighty Actions Device**](./examples/devices/lighty-actions-device/README.md)
- [**lighty Network Topology Device**](./examples/devices/lighty-network-topology-device/README.md)
- [**lighty Notifications Device**](./examples/devices/lighty-notifications-device/README.md)
- [**lighty Toaster Device**](./examples/devices/lighty-toaster-device/README.md)
- [**lighty Toaster Multiple Devices**](./examples/devices/lighty-toaster-multiple-devices/README.md)

[Read about the background of this project here.](https://pantheon.tech/netconf-monitoring-get-schema/)

## Running the application using docker
1. Build the application dockerfile using `docker build -t lighty-netconf-simulator .`
2. Run the dockerfile using `docker run --name lighty-netconf-simulator -p 17830:17830 lighty-netconf-simulator -i resources/ -o resources/`
3. Get ip address using `docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' lighty-netconf-simulator`
4. Start lighty.io or other controller and connect to the device
5. Make changes to the device. example:
```
curl --location 'http://localhost:8888/restconf/data/network-topology:network-topology/topology=topology-netconf/node=new-netconf-device/yang-ext:mount/network-topology:network-topology/topology=default-topology' \
--header 'Content-Type: application/json' \
--data '{
    "node": [
        {
            "node-id": "test-12345"
            }
    ]
}'
```
6. Stop the docker container
7. Restart the docker container using `docker start lighty-netconf-simulator`

## Known Issues

**Problem:** Creating multiple simulators takes a long time.  
 Delay can be caused by Random Number Generation `/dev/random`.   
**Solution:** Use /dev/urandom instead of /dev/random by passing it as system property  
`-Djava.security.egd=file:/dev/./urandom` or modify file `$JAVA_HOME/jre/lib/security/java.security`  
by changing property `securerandom.source=file:/dev/random`
to `securerandom.source=file:/dev/urandom`.