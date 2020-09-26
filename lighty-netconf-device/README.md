# NETCONF device
This is NETCONF device base library enabling users to create own
netconf device implementations.

## NETCONF commands
```ssh -oStrictHostKeyChecking=no -oUserKnownHostsFile=/dev/null admin@127.0.0.1 -p 17830 -s netconf```

```
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
    </capabilities>
</hello>
]]>]]>
```
```
<rpc message-id="101"
     xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
     <get/>
</rpc>]]>]]>
```
```
<rpc message-id="101"
     xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
     <get-config/>
</rpc>]]>]]>
```



## Pyang
In order to generate simple XML data skeleton from yang model use this pyang command:

```pyang -f sample-xml-skeleton YANG_FILE -o OUTPUT_FILE```

## RPC edit-config
Current implementation supports only simple requests.

Current state supports:
- default operations
- only one operation per request
- only one top level element (in config element)

To be done:
- operations are not fully compliant with RFC(all operations support, notifications defined inside tree, ...)
- support multiple operations per request
- support multiple top level elements (in config element)
- refactor whole edit-config implementation
- implement getting schema in different formats with get-schema RPC
- configurable delay between request response - simulate device processing
