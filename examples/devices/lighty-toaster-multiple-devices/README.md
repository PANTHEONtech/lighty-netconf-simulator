# Toaster multiple devices example

The simulator has the ability to create multiple devices in a single JVM instance. **All devices share the same datastore.**  
The netconf device uses the toaster yang model `toaster@2009-11-20.yang`.

### Build and run
Build root project - for more details check: [README](../../../README.md)

**Run device**
* Extract binary distribution in target directory.
* Run jar file from zip with parameters. Parameters are optional. If they are not used, the default value is used.  
`--device-count DEVICES-COUNT` (Default 1) Number of simulated netconf devices to spin. This is the number of actual ports which will be used for the devices. If some ports are bound, these ports will be skipped. The log shows all open ports.    
`--starting-port STARTING-PORT` (Default 17380) First port for simulated device. Each other device will use incremented port number.    
`--thread-pool-size THREAD-POOL-SIZE` (Default 8) The number of threads to keep in the pool, when creating a device simulator, even if they are idle.    
```
java -jar lighty-toaster-multiple-devices-14.0.0.jar --starting-port 20000 --device-count 200 --thread-pool-size 200
```

### Connect to device via SSH
**Open session**

To connect to a devices via SSH, run command.  
All devices are accessible via a bound port.

```
ssh admin@127.0.0.1 -p 20000 -s netconf
ssh admin@127.0.0.1 -p 20001 -s netconf
...
ssh admin@127.0.0.1 -p 20199 -s netconf
```

where
- `admin` is default username of a device with password `admin`(no other users)
- `127.0.0.1` is the local IP address of the machine where device is running
- `-p 20000` is port number of running device
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

For more information on simulator basic NETCONF commands check: [Basic NETCONF commands](../lighty-toaster-device/README.md#basic-netconf-commands).

### Toaster Model
YANG model used by the device is toaster@2009-11-20.yang.

The simulator has defined 2 RPCs:

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
