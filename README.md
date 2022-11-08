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
* Build the project with Java 17:
```
mvn clean install
```
* The NETCONF Device Library build is located at:

`lighty-netconf-device\target\lighty-netconf-device-17.0.2-SNAPSHOT.jar`

* The build & run procedures for the example devices are described in each device's README.

## Example NETCONF Device Simulators
This tool contains 5 device examples, to demonstrate the usage of the NETCONF Device Library for creating custom devices:
- [**lighty Actions Device**](./examples/devices/lighty-actions-device/README.md)
- [**lighty Network Topology Device**](./examples/devices/lighty-network-topology-device/README.md)
- [**lighty Notifications Device**](./examples/devices/lighty-notifications-device/README.md)
- [**lighty Toaster Device**](./examples/devices/lighty-toaster-device/README.md)
- [**lighty Toaster Multiple Devices**](./examples/devices/lighty-toaster-multiple-devices/README.md)

[Read about the background of this project here.](https://pantheon.tech/netconf-monitoring-get-schema/)

## Known Issues

**Problem:** Creating multiple simulators takes a long time.  
 Delay can be caused by Random Number Generation `/dev/random`.   
**Solution:** Use /dev/urandom instead of /dev/random by passing it as system property  
`-Djava.security.egd=file:/dev/./urandom` or modify file `$JAVA_HOME/jre/lib/security/java.security`  
by changing property `securerandom.source=file:/dev/random`
to `securerandom.source=file:/dev/urandom`.