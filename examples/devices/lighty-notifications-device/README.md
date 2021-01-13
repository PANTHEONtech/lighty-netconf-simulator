# Notification device example
This NETCONF device example shows how NETCONF device is customized to publish notifications from the device. Device uses `lighty-test-notifications@2018-08-20.yang` YANG model located in `examples/models/lighty-example-notifications-model`. Therefore, the name: notifications device.

### Usage
Device uses `TriggerNotificationProcessor` class to handle incoming `triggerDataNotification` RPC which implies publishing notification `DataNotification`.
`triggerDataNotification` RPC and `dataNotification` notification are described in YANG file.
Check commands in [Notifications device model](#notifications-device-model) on how to trigger publishing `dataNotification` notification.

### Build and run
Build root project - for more details check: [README](../../../README.md)

**Run device**
* extract binary distribution `lighty-notifications-device-13.1.0-bin.zip`
from target directory
* run jar file from zip with default parameter
```
java -jar lighty-notifications-device-13.1.0.jar
```
To run device on specific port, add port number as an argument
* run device on specific port `12345` (any available port)
```
java -jar lighty-notifications-device-13.1.0.jar 12345
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
<rpc message-id="cs1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session/>
</rpc>
]]>]]>
```
`ok` message will be replied and then closed the session.
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="cs1">
    <ok/>
</rpc-reply>
```

### Basic NETCONF commands

Notification device supports all basic NETCONF commands,
but as far as this device does not contain any other YANG model with root containers,
this device will not be able to store or modified data device manages.
This device tries to describe usage of NETCONF `notifications`, only.
For detailed description, find more details in topology-device or toaster-device projects.

### Trigger and receive notification
YANG model used by the device is `lighty-test-notifications@2018-08-20.yang`.

Notifications device has only one RPC defined in YANG model:

**triggerDataNotification**

- takes 4 mandatory parameters: `ClientId`, `Count`, `Delay` and `Payload`

To trigger RPC with
client `ClientId: 0`, `Count: 5`, `Delay: 500` and `Payload: just simple notification`,
call `triggerDataNotification` RPC

```
<rpc message-id="nt1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <triggerDataNotification xmlns="yang:lighty:test:notifications">
        <ClientId>0</ClientId>
        <Count>5</Count>
        <Delay>500</Delay>
        <Payload>just simple notification</Payload>
    </triggerDataNotification>
</rpc>
]]>]]>
```

Log info message after triggering `triggerDataNotification RPC`:
`INFO [nioEventLoopGroup-2-3] (TriggerNotificationProcessor.java:82) - triggering notifications: clientId=0 5 delay=500ms, payload=just simple notification`
`INFO [nioEventLoopGroup-2-3] (TriggerNotificationProcessor.java:71) - sending notification clientId=0 1/5`

Triggered `triggerDataNotification` RPC publishes notification `DataNotification`,
so any other listener device subscribed to notifications by `create-subscription` message,
will receive notification message, example:
```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<notification xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
    <eventTime>2020-09-04T14:56:15Z</eventTime>
    <dataNotification xmlns="yang:lighty:test:notifications">
        <Ordinal>5</Ordinal>
        <Payload>just simple notification</Payload>
        <ClientId>0</ClientId>
    </dataNotification>
</notification>
]]>]]>
```

which contains input parameters of called `triggerDataNotification` RPC,
in this case `Ordinal(Count): 5`, `ClientId: 0` and `Payload: just simple notification`.

**Subscribe on notification**

To receive notifications on NETCONF listener device, subscribe
for notification by sending `create-subscription` message:

```
<rpc message-id="101" xmlns ="urn:ietf:params:xml:ns:netconf:base:1.0">
    <create-subscription xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
    </create-subscription>
</rpc>
]]>]]>
```
