# Lighty toaster device example
This netconf device example shows how to customize base `lighty-netconf-device`
to support functionality described in your custom yang model. In this netconf
device the toaster yang model `toaster@2009-11-20.yang` is used. Therefore the
name: toaster device.

### Brief description
Device registers toaster rpc processors to handle incoming rpc requests. Each
of the processor calls method of `ToasterServiceImpl` which implements
`ToasterService` generated from yang model.

### Build and run
Build root project - for more details check: [README](../../../README.md)

**Run device**
* extract binary distribution `lighty-toaster-device-13.1.0-bin.zip`
from target directory
* run jar file from zip with default parameter
```
java -jar lighty-toaster-device-13.1.0.jar
```
To run device on specific port, add port number as an argument
* run device on specific port `12345` (any available port)
```
java -jar lighty-toaster-device-13.1.0.jar 12345
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
sending client's model capabilities in hello-message:

```
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
    </capabilities>
</hello>
]]>]]>
```
which informs the device which capabilities the client supports.
The capabilities tag contains list of capabilities the client supports, e.g.
```
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>capability1</capabaility>
        <capability>capability2</capabaility>
        <capability>capability3</capabaility>
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
ok message will be replied and then closed the session.
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="106">
    <ok/>
</rpc-reply>
```

### Basic NETCONF commands

As a NETCONF device, it provides basic NETCONF defined commands (RPCs):

**get**

returns data from the device operational datastore

```
<rpc message-id="101" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <get/>
</rpc>
]]>]]>
```

Reply contains all data that are in the device's operational datastore.
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="101" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <data>
        <toaster xmlns="http://netconfcentral.org/ns/toaster">
            <toasterManufacturer>Pantheon</toasterManufacturer>
            <toasterModelNumber>SuperToaster9000</toasterModelNumber>
            <toasterStatus>up</toasterStatus>
        </toaster>
    </data>
</rpc-reply>
]]>]]>
```

**get-config**

Returns configuration data from the device datastore, specified in body of
request in source parameter.
```
<rpc message-id="101" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
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
<rpc-reply message-id="101" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <data>
        <toaster xmlns="http://netconfcentral.org/ns/toaster">
            <darknessFactor>500</darknessFactor>
        </toaster>
    </data>
</rpc-reply>
]]>]]>
```

**edit-config**

RPC to edit configuration data in the device.
```
<rpc message-id="m-9" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
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

Example of `replace` `edit-config` RPC message, to change `darknessFactor` of
toaster in the device's `running` datastore. The `config` tag contains actual
data to edit.
```
<rpc message-id="m-9" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <edit-config>
        <target>
            <running/>
        </target>
        <config xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">
            <toaster xmlns="http://netconfcentral.org/ns/toaster">
                <darknessFactor nc:operation="replace">750</darknessFactor>
            </toaster>
        </config>
    </edit-config>
</rpc>
]]>]]>
```
When operation `create` is used to edit existing data, device reply by error
response:
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="m-9" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <rpc-error>
        <error-type>rpc</error-type>
        <error-tag>data-exists</error-tag>
        <error-severity>error</error-severity>
        <error-message>data-exists</error-message>
    </rpc-error>
</rpc-reply>
]]>]]>
```

### Toaster Model
YANG model used by the device is toaster@2009-11-20.yang.

Toaster device has defined 3 RPCs in YANG model:

**make-toast**

- takes optional `toasterDoneness` and `toasterToastType` values as an input
into RPC, if not specified the default values from YANG models will be used.

```
<rpc message-id="mt1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <make-toast xmlns="http://netconfcentral.org/ns/toaster">
        <toasterDoneness>2</toasterDoneness>
        <toasterToastType>frozen-waffle</toasterToastType>
     </make-toast>
</rpc>
]]>]]>
```

**cancel-toast**

(has no inputs)
```
<rpc message-id="ct1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <cancel-toast xmlns="http://netconfcentral.org/ns/toaster">
    </cancel-toast>
</rpc>
]]>]]>
```

**restock-toast**

- takes optional `amountOfBreadToStock` value as an input, if not specified
default value from YANG models will be used.

```
<rpc message-id="rt1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <restock-toaster xmlns="http://netconfcentral.org/ns/toaster">
        <amountOfBreadToStock>10</amountOfBreadToStock>
    </restock-toaster>
</rpc>
]]>]]>
```

After each of the upper RPC calls, appropriate info message can be seen in
device logs.

Example of log info for make-toast RPC:
```
INFO [nioEventLoopGroup-2-3] (ToasterServiceMakeToastProcessor.java:30) - execute RPC: make-toast
INFO [nioEventLoopGroup-2-3] (ToasterServiceImpl.java:35) - makeToast 2 interface org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.FrozenWaffle
```
contains information with what parameters was RPC called:
`toasterDoneness: 2` and `toasterToastType: frozen-waffle`.

Example of successful reply message:
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <rpc-reply message-id="id" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0"/>
]]>]]>
```
where message-id corresponds with message-id which RPC was called with, so for
request with message-id `mt1` the response will contain same message-id `mt1`.

Example of unsuccessful reply contains appropriate error message (e.g. unknown
toast type: pink-bread)

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="mt1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <rpc-error>
        <error-type>application</error-type>
        <error-tag>operation-failed</error-tag>
        <error-severity>error</error-severity>
        <error-message>Unexpected error</error-message>
        <error-info>
            <ERROR>java.lang.UnsupportedOperationException:
                java.lang.IllegalArgumentException:
                    Parsed QName (http://netconfcentral.org/ns/toaster?revision=2009-11-20)pink-bread
                    does not refer to a valid identity
            </ERROR>
        </error-info>
    </rpc-error>
</rpc-reply>
]]>]]>
```
