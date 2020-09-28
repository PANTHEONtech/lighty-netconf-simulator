# lighty.io NETCONF Device simulators and libraries

Lightweight project that provides:
- **NETCONF Device libraries** for building custom NETCONF device
- **Example NETCONF Device simulators** to demonstrate how can be
 libraries used for creating custom NETCONF devices

## NETCONF Device libraries
`lighty-netconf-device` is NETCONF device library for creating
custom NETCONF devices. With provided NETCONF device builder,
own device can be built with use of builder switches for
adding custom YANG models, custom request processors etc.

## Build and run
* build the project with Java 11:
```
mvn clean install
```
* NETCONF Device library build is located at:

`lighty-netconf-device\target\lighty-netconf-device-12.2.1-SNAPSHOT.jar`

* example devices build and run procedures are described in each device's README

## Example NETCONF Device simulators
- contains 4 example devices to demonstrate usage of NETCONF device library
for creating custom devices
    - [**lighty-actions-device**](./examples/devices/lighty-actions-device/README.md)
    - [**lighty-network-topology-device**](./examples/devices/lighty-network-topology-device/README.md)
    - [**lighty-notifications-device**](./examples/devices/lighty-notifications-device/README.md)
    - [**lighty-toaster-device**](./examples/devices/lighty-toaster-device/README.md)
