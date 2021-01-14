# Network topology device example
This NETCONF device example shows how NETCONF device is customized to work with topology data inside NETCONF device. Device sends
notifications when topologies are created and deleted. Device is simulator for network topology, so its functionality is implemented
to serve as an example to show how RPCs are called, take input, return output, how to create topologies, edit topologies,
remove topologies, get topologies etc. Device contains topology with id `default-topology` by default.
Topology requests and notifications are described in `network-topology-rpcs` yang file which is located in
`examples/models/lighty-example-network-topology-device-model`. Therefore the name: network topology device.

### Brief description
Device registers rpc processors to handle incoming RPC requests. They each call method of `NetworkTopologyRpcsService`.
For every RPC in `network-topology-rpcs` there is created a single processor. Device also registers topology change
listener which listens to the state of the network topology. It sends notifications when changes to the network
topology are made.

### Build and run
Build root project - for more details check: [README](../../../README.md)

**Run device**
* extract binary distribution in target directory
* run jar file from zip with default parameter
```
java -jar lighty-network-topology-device-13.1.0.jar
```
* to run device on specific port it is necessary to add port number as an argument
* run device on specific port `12345` (any available port)
```
java -jar lighty-network-topology-device-13.1.0.jar 12345
```

### Connect to device via SSH
**Open session**

To connect to a device via SSH, run command

```
ssh admin@127.0.0.1 -p 12345 -s netconf
```

where
- `admin` is default username of a device with password `admin`(no other users)
- `127.0.0.1` is the local IP address of the machine where device is running
- `-p 12345` is port number of running device, default is `17830`
- `-s netconf` option establishes the NETCONF session as an SSH subsystem,
which means that NETCONF can be used in terminal through opened SSH session

To complete connection with a device, it is necessary to initiate handshake by
sending client's model capabilities in `hello-message`:

```
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:tech.pantheon.netconfdevice.network.topology.rpcs?module=network-topology-rpcs;revision=2018-03-20</capability>
    </capabilities>
</hello>
]]>]]>
```

which informs the device which capabilities the client supports.
The capabilities tag contains list of capabilities the client supports, e.g.

```
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>CAPABILITY_1</capabaility>
        <capability>CAPABILITY_2</capabaility>
        <capability>CAPABILITY_3</capabaility>
        ...
    </capabilities>
</hello>
]]>]]>
```

If handshake is not completed by sending hello message with client's capabilities
and any other message is sent, following error message is received (long text replaced with ...):
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <rpc-error>
        <error-type>rpc</error-type>
        <error-tag>malformed-message</error-tag>
        <error-severity>error</error-severity>
        <error-message>
            java.lang.IllegalStateException: Hello message not received, instead received: ...
        </error-message>
        <error-info>
            <cause>
                java.lang.IllegalStateException: Hello message not received, instead received: ...
            </cause>
        </error-info>
    </rpc-error>
</rpc-reply>
]]>]]>
```

**Close session**

To properly exit current NETCONF device SSH session, close-session message
needs to be send

```
<rpc message-id="106" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session/>
</rpc>
]]>]]>
```

`ok` message will be replied and then closed the session.

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="106">
    <ok/>
</rpc-reply>
```

### Basic NETCONF commands

As a NETCONF device, it provides basic NETCONF defined commands (RPCs):

**get**

returns data from the device's operational datastore

```
<rpc message-id="nc1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <get/>
</rpc>
]]>]]>
```

Reply contains data that are in the device's operational datastore
(to shorten reply, most of `available-capabilites` were replaced by ...).

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="nc1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <data>
        <network-topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
            <topology>
                <topology-id>test-topology</topology-id>
                <node>
                    <node-id>test-nettopo-node</node-id>
                    <connection-status xmlns="urn:opendaylight:netconf-node-topology">connected</connection-status>
                    <port xmlns="urn:opendaylight:netconf-node-topology">17835</port>
                    <available-capabilities xmlns="urn:opendaylight:netconf-node-topology">
                        <available-capability>
                            <capability>(urn:ietf:params:xml:ns:yang:ietf-inet-types?revision=2013-07-15)ietf-inet-types</capability>
                            <capability-origin>device-advertised</capability-origin>
                        </available-capability>
                        ...
                    </available-capabilities>
                    <unavailable-capabilities xmlns="urn:opendaylight:netconf-node-topology"/>
                    <host xmlns="urn:opendaylight:netconf-node-topology">127.0.0.1</host>
                </node>
                <node>
                    <node-id>test-nettopo-node-2</node-id>
                    ...
                </node>
            </topology>
            <topology>
                <topology-id>default-topology</topology-id>
            </topology>
        </network-topology>
    </data>
</rpc-reply>
]]>]]>
```

**get-config**

Returns only configuration data from the device datastore, specified in body of
request in source parameter.

```
<rpc message-id="nc2" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <get-config>
        <source>
            <running/>
        </source>
    </get-config>
</rpc>
]]>]]>
```

The returned reply contains data from running type of configuration datastore

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="nc2" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <data>
        <network-topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
            <topology>
                <topology-id>test-topology</topology-id>
                <node>
                    <node-id>test-nettopo-node</node-id>
                    <port xmlns="urn:opendaylight:netconf-node-topology">17835</port>
                    <schemaless xmlns="urn:opendaylight:netconf-node-topology">false</schemaless>
                    <host xmlns="urn:opendaylight:netconf-node-topology">127.0.0.1</host>
                    <tcp-only xmlns="urn:opendaylight:netconf-node-topology">false</tcp-only>
                    <keepalive-delay xmlns="urn:opendaylight:netconf-node-topology">0</keepalive-delay>
                    <username xmlns="urn:opendaylight:netconf-node-topology">admin</username>
                    <password xmlns="urn:opendaylight:netconf-node-topology">admin</password>
                </node>
                <node>
                    <node-id>test-nettopo-node-2</node-id>
                    ...
                </node>
            </topology>
            <topology>
                <topology-id>default-topology</topology-id>
            </topology>
        </network-topology>
    </data>
</rpc-reply>
]]>]]>
```

**edit-config**

RPC to edit configuration data in the device.

```
<rpc message-id="nc3" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <edit-config>
        <target>
            <DATASTORE_TYPE/>
        </target>
        <default-operation>DEFAULT_OPERATION</default-operation>
        <config>
            <DATA_TO_EDIT...>
                <SOME_DATA operation="OPERATION">
            ...
        </config>
    </edit-config>
</rpc>
]]>]]>
```

NETCONF defines several types of operations, which can be used within
edit-config RPC:
- `merge, replace, create, delete, remove`

It can also contain `default-operation`, for requests without operation
specified:
- `merge, replace, none`

Default behaviour for operations is merge.

Example of `edit-config` RPC message, to change `port` value of `test-nettopo-node2`
in topology `test-topology` in the device's `running` datastore.
The `config` tag contains actual data to edit.

```
<rpc message-id="nc3" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <edit-config>
        <target>
            <running/>
        </target>
        <config xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">
            <network-topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
                <topology>
                    <topology-id>test-topology</topology-id>
                    <node>
                        <node-id>test-nettopo-node</node-id>
                        <port xmlns="urn:opendaylight:netconf-node-topology" nc:operation="replace">17837</port>
                    </node>
                </topology>
            </network-topology>
        </config>
    </edit-config>
</rpc>
]]>]]>
```
simple `ok` message is returned

When operation `create` is used to edit existing data, device replies by
appropriate error response:
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="nc3" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <rpc-error>
        <error-type>rpc</error-type>
        <error-tag>data-exists</error-tag>
        <error-severity>error</error-severity>
        <error-message>data-exists</error-message>
    </rpc-error>
</rpc-reply>
]]>]]>
```

### Network Topology Model

YANG model used by the device is `network-topology-rpcs@2018-03-20.yang`, which also uses
`netconf-node-topology@2015-01-14.yang` and `network-topology@2013-10-21.yang`.
In YANG model, there are defined groupings for `node-data` and `topology-data`,
which will be referenced further.

- `node-data` contains list of nodes with `node-id` as key
and every node has a lot of optional fields that are described in YANG model.

- `topology-data` contains list of topologies with `topology-id` as key
and every topology has a lot of optional fields that are described in YANG model.

Network topology device has defined following RPCs in YANG model and implemented
as mentioned in *Brief description*:

**get-node-from-topology-by-id**

- takes mandatory `is-config`, `topology-id` and `node-id` values as an input to RPC
- `is-config` is boolean value, which tells if RPC returns configuration or operational data
- `topology-id` is id of topology where to look for node with `node-id`

```
<rpc message-id="rpc1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <get-node-from-topology-by-id xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <is-config>true</is-config>
        <topology-id>test-topology</topology-id>
        <node-id>test-nettopo-node</node-id>
    </get-node-from-topology-by-id>
</rpc>
]]>]]>
```

- if node is found, returns node-data of searched node

Output example for `is-config: true`
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="rpc1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <node xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <node-id>test-nettopo-node</node-id>
        <password>admin</password>
        <username>admin</username>
        <port>17835</port>
        <tcp-only>false</tcp-only>
        <host>127.0.0.1</host>
        <schemaless>false</schemaless>
    </node>
</rpc-reply>
]]>]]>
```

Output example for `is-config: false` (with node added before calling RPC)
(to shorten reply, most of `available-capabilites` were replaced by ...)
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="rpc1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <node xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <node-id>test-nettopo-node</node-id>
        <available-capabilities>
            <available-capability>
                <capability>(urn:ietf:params:xml:ns:yang:ietf-inet-types?revision=2013-07-15)ietf-inet-types</capability>
                <capability-origin>device-advertised</capability-origin>
            </available-capability>
            ...
        </available-capabilities>
        <port>17835</port>
        <tcp-only>false</tcp-only>
        <host>127.0.0.1</host>
        <unavailable-capabilities/>
        <connection-status>connected</connection-status>
    </node>
</rpc-reply>
]]>]]>
```

- if node is not found, simple `ok` message is returned

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="rpc1">
    <ok/>
</rpc-reply>
```

**get-topology-ids**

(has no inputs)
```
<rpc message-id="rpc2" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <get-topology-ids xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
    </get-topology-ids>
</rpc>
]]>]]>
```

- returns list of ids of all topologies, which device contains

**get-topology-by-id**

- takes mandatory `topology-id` value as an input to RPC
- `topology-id` is id of topology to return

```
<rpc message-id="rpc3" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <get-topology-by-id xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <topology-id>default-topology</topology-id>
    </get-topology-by-id>
</rpc>
]]>]]>
```

- if topology is found, returns topology-data for searched topology

**get-topologies**

(has no inputs)
```
<rpc message-id="rpc4" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <get-topologies xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
    </get-topologies>
</rpc>
]]>]]>
```

- returns list of all topologies, which device contains

**create-topology**

- takes mandatory `topology-id` as input value
- creates new topology with `topology-id` value
```
<rpc message-id="rpc5" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <create-topology xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <topology-id>test-topology</topology-id>
    </create-topology>
</rpc>
]]>]]>
```
- returns simple `ok` message

**add-node-into-topology**

- takes `topology-id`(mandatory) and `node-data` as input values
- creates new node in topology with `topology-id` and provided `node-data` values
- only mandatory value in `node-data` is `node-id`, because it is a unique key identifier of node in topology
- all parameters are described in YANG models mentioned at the beginning of section
- in case of this simulator device, it is not needed to have real device running to connect it
as a node into topology

```
<rpc message-id="rpc6" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <add-node-into-topology xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <topology-id>test-topology</topology-id>
        <node>
            <node-id>test-nettopo-node</node-id>
            <host>127.0.0.1</host>
            <port>17835</port>
            <username>admin</username>
            <password>admin</password>
            <tcp-only>false</tcp-only>
            <keepalive-delay>0</keepalive-delay>
            <schemaless>false</schemaless>
        </node>
  </add-node-into-topology>
</rpc>
]]>]]>
```

**remove-node-from-topology**

- takes mandatory `topology-id` and `node-id` as input values
- removes node with `node-id` value from topology with `topology-id` value
```
<rpc message-id="rpc7" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <remove-node-from-topology xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <topology-id>test-topology</topology-id>
        <node-id>test-nettopo-node</node-id>
    </remove-node-from-topology>
</rpc>
]]>]]>
```

- returns simple `ok` message

**remove-topology**

- takes mandatory `topology-id` as input value
- removes topology with `topology-id` value
```
<rpc message-id="rpc8" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <remove-topology xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <topology-id>test-topology</topology-id>
    </remove-topology>
</rpc>
]]>]]>
```

- returns simple `ok` message

**remove-all-topologies**

(has no inputs)
```
<rpc message-id="rpc9" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <remove-all-topologies xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
    </remove-all-topologies>
</rpc>
]]>]]>
```

- returns simple `ok` message

After each of the upper RPC calls, appropriate info message can be seen in
device logs.


Example of log info for create-topology RPC:
```
INFO [nioEventLoopGroup-2-3] (NetworkTopologyServiceImpl.java:186) - Creating topology Uri{_value=test-topology}
INFO [pool-6-thread-1] (TopologyDataTreeChangeListener.java:65) - Data has been created
```
contains information with what parameters was RPC called:
`_value: test-topology` and notification from `TopologyDataTreeChangeListener`
that data has been created on device.

Example of successful reply message:
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <rpc-reply message-id="id" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0"/>
]]>]]>
```
where message-id corresponds with message-id which RPC was called with, so for
request with message-id `rpc1` the response will contain same message-id `rpc1`.

Example of unsuccessful reply contains appropriate error message
(e.g. illegal negative value on field with only positive values allowed)
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="rpc6" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <rpc-error>
        <error-type>application</error-type>
        <error-tag>operation-failed</error-tag>
        <error-severity>error</error-severity>
        <error-message>Unexpected error</error-message>
        <error-info>
            <ERROR>java.lang.UnsupportedOperationException:
                java.lang.NumberFormatException:
                    Illegal leading minus sign on unsigned string -1.
            </ERROR>
        </error-info>
    </rpc-error>
</rpc-reply>
]]>]]>
```

**Preconfigured data**
Before device starts, it reads initial data from XML configuration files
- `initial-network-topo-config-datastore.xml`
- `initial-network-topo-operational-datastore.xml`
each corresponding to `configuration` and `operational datastore`. If it is needed
to preconfigure device with initial data, XML files have to be modified to contain
those particular data.

Example of XML file for configuration datastore,
when device needs to be started with topology `init-topology-A`, which contains
node `init-node-A`, in configuration datastore:

```
<?xml version='1.0' encoding='UTF-8'?>
<data xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <network-topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
        <topology>
            <topology-id>default-topology</topology-id>
        </topology>
        <topology>
            <topology-id>init-topology-A</topology-id>
            <node>
                <node-id>init-node-A</node-id>
                <port xmlns="urn:opendaylight:netconf-node-topology">12345</port>
                <schemaless xmlns="urn:opendaylight:netconf-node-topology">false</schemaless>
                <host xmlns="urn:opendaylight:netconf-node-topology">127.0.0.1</host>
                <tcp-only xmlns="urn:opendaylight:netconf-node-topology">false</tcp-only>
                <keepalive-delay xmlns="urn:opendaylight:netconf-node-topology">0</keepalive-delay>
                <username xmlns="urn:opendaylight:netconf-node-topology">admin</username>
                <password xmlns="urn:opendaylight:netconf-node-topology">admin</password>
            </node>
        </topology>
    </network-topology>
</data>
```

### Notifications

This device also supports notifications.

Subscription to get notifications is done by
sending `create-subscription` message:

```
<rpc message-id="rpc10" xmlns ="urn:ietf:params:xml:ns:netconf:base:1.0">
    <create-subscription xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
    </create-subscription>
</rpc>
]]>]]>
```

when subscription is created, simple `ok` message is returned.

Notifications for this device are defined in `network-topology-rpcs` YANG model,
and there are two type of them:

- `new-topology-created`
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<notification xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
    <eventTime>2020-08-31T20:08:04Z</eventTime>
    <new-topology-created xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <topology-id>test-topology-for-notification</topology-id>
    </new-topology-created>
</notification>
]]>]]>
```

This notification is sent, when new topology is created on device.
It contains information about topology created, and the time event occurred.

- `topology-deleted`
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<notification xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
    <eventTime>2020-08-31T20:10:04Z</eventTime>
    <topology-deleted xmlns="urn:tech.pantheon.netconfdevice.network.topology.rpcs">
        <topology-ids>test-topology-for-notification</topology-ids>
    </topology-deleted>
</notification>
]]>]]>
```

This notification is sent, when topology was deleted from device.
It contains information about topology deleted, and the time event occurred.
