# Lighty Actions Device
This is example NETCONF device which can process yang actions. Device supports actions
described in `lighty-example-data-center-model` yang from `examples/models/lighty-example-data-center-model`.
YANG action is an operation which is tied to a specific YANG container or data
node in the datastore. Lighty-actions-netconf-app can be used to trigger
actions on this device.

### Brief introduction
Device can process yang actions via `ActionServiceDeviceProcessor` class. Every
action has its own implementation and its own action processor. Currently, it
has implemented behaviour for start action and reset action. When action
request is invoked, device decides which processor should handle the request.
Processor then calls specific action implementation and sends actions response.

### Processors
ActionServiceDeviceProcessor is a root request Processor. Processor decides on
which action processor apply to each request. Action processor then calls its
action specific execute method.

### Actions
Implementation of the particular action behaviour. Currently, all actions return
the value they get from input.

### Build and run
Build root project - for more details check: [README](../../../README.md)

**Run device**
* extract binary distribution in target directory
* run jar file from zip with default parameter
```
java -jar lighty-action-device-13.1.0.jar
```
To run device on specific port, add port number as an argument
* run device on specific port `12345` (any available port)
```
java -jar lighty-action-device-13.1.0.jar 12345
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

Device has no preinitialized data, so it is empty when started.

As a NETCONF device, it provides basic NETCONF defined commands (RPCs):

**get**

returns data from the device's operational datastore.

```
<rpc message-id="nc1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <get/>
</rpc>
]]>]]>
```

Empty data response example:
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="nc1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
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
(two server entries were added for example purposes)
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="nc2" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <data>
        <server xmlns="urn:example:data-center">
            <name>server-earth</name>
        </server>
        <server xmlns="urn:example:data-center">
            <name>server-mars</name>
        </server>
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

Example of `create` `edit-config` RPC message, to change `darknessFactor` of
toaster in the device's `running` datastore. The `config` tag contains actual
data to edit.
```
<rpc message-id="m-9" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <edit-config>
        <target>
            <running/>
        </target>
        <config xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">
            <server xmlns="urn:example:data-center" nc:operation="create">
                <name xmlns="urn:example:data-center">server-moon</name>
            </server>
        </config>
    </edit-config>
</rpc>
]]>]]>
```
When operation `create` is used to edit existing data, device reply by error
response:
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

### Trigger action

YANG model used by the device is `example-data-center@2018-08-07.yang`.

**reset**

- this action is connected with list `server` in YANG model, and is called
on a specific entry of list(in this example `server-earth`)
- input values are:
    - string value `name`, which represents key in list `server`
    - string value `reset-at`, which represents datetime

```
<rpc message-id="ac1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <action xmlns="urn:ietf:params:xml:ns:yang:1">
        <server xmlns="urn:example:data-center">
            <name>server-earth</name>
            <reset>
                <reset-at>2020-09-03T16:20:00Z</reset-at>
            </reset>
        </server>
    </action>
</rpc>
]]>]]>
```

Action is implemented in such a way, that it returns `reset-at` value
provided in input, as a `reset-finished-at` output value.

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="ac1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <reset-finished-at xmlns="urn:example:data-center">2020-09-03T16:20:00Z</reset-finished-at>
</rpc-reply>
]]>]]>
```

**start**

- this action is connected with empty container `device` in YANG model, and is called
on that particular container
- input value is string value `reset-at`, which represents datetime

```
<rpc message-id="ac2" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <action xmlns="urn:ietf:params:xml:ns:yang:1">
        <device xmlns="urn:example:data-center">
            <start>
                <start-at>2020-09-03T16:30:00Z</start-at>
            </start>
        </device>
    </action>
</rpc>
]]>]]>
```

Action is implemented in such a way, that it returns `start-at` value
provided in input, as a `start-finished-at` output value.

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply message-id="ac2" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <start-finished-at xmlns="urn:example:data-center">2020-09-03T16:30:00Z</start-finished-at>
</rpc-reply>
]]>]]>
```
