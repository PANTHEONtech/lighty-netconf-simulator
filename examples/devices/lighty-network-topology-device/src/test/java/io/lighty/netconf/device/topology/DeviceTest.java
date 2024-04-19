/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology;

import static io.lighty.netconf.device.topology.TestUtils.xmlFileToInputStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import io.lighty.netconf.device.utils.TimeoutUtil;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.netconf.api.messages.NetconfMessage;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.client.NetconfClientDispatcher;
import org.opendaylight.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.NetconfClientSessionListener;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration.NetconfClientProtocol;
import org.opendaylight.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.netconf.nettyutil.handler.ssh.authentication.LoginPasswordHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

public class DeviceTest {

    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;
    private static final String USER = "admin";
    private static final String PASS = "admin";
    private static final int DEVICE_SIMULATOR_PORT = 9090;
    private static final String CREATE_TOPOLOGY_RPC_REQUEST_XML = "create_topology_rpc_request.xml";
    private static final String CREATE_TOPOLOGY_RPC_RESPONSE_XML = "create_topology_rpc_response.xml";
    private static final String DELETE_TOPOLOGY_RPC_REQUEST_XML = "delete_topology_rpc_request.xml";
    private static final String DELETE_TOPOLOGY_RPC_RESPONSE_XML = "delete_topology_rpc_response.xml";
    private static final String ADD_NODE_RPC_REQUEST_XML = "add_node_rpc_request.xml";
    private static final String EXCEPTED_ADD_NODE_RESPONSE = "add_node_rpc_response.xml";
    private static final String CREATE_TOPOLOGY_CONFIG_REQUEST_XML = "create_topology_config_request.xml";
    private static final String MERGE_TOPOLOGY_CONFIG_REQUEST_XML = "merge_topology_config_request.xml";
    private static final String GET_CONFIG_REQUEST_XML = "get_config_request.xml";
    private static final String DELETE_TOPOLOGY_CONFIG_REQUEST_XML = "delete_topology_config_request.xml";
    private static final String GET_SCHEMAS_REQUEST_XML = "get_schemas_request.xml";
    private static Main deviceSimulator;
    private static NioEventLoopGroup nettyGroup;
    private static NetconfClientDispatcherImpl dispatcher;


    @BeforeAll
    public static void setUpClass() {
        deviceSimulator = new Main();
        deviceSimulator.start(new String[]{DEVICE_SIMULATOR_PORT + ""}, false, false);

        nettyGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(NetconfClientDispatcher.class));
        dispatcher = new NetconfClientDispatcherImpl(nettyGroup, nettyGroup, new HashedWheelTimer());
    }

    @AfterAll
    public static void cleanUpClass() throws InterruptedException {
        deviceSimulator.shutdown();
        nettyGroup.shutdownGracefully().sync();
    }

    private static NetconfClientConfiguration createSHHConfig(final NetconfClientSessionListener sessionListener) {
        return NetconfClientConfigurationBuilder.create()
                .withAddress(new InetSocketAddress("localhost", DEVICE_SIMULATOR_PORT))
                .withSessionListener(sessionListener)
                .withConnectionTimeoutMillis(NetconfClientConfigurationBuilder.DEFAULT_CONNECTION_TIMEOUT_MILLIS)
                .withProtocol(NetconfClientProtocol.SSH)
                .withAuthHandler(new LoginPasswordHandler(USER, PASS))
                .build();
    }

    @Test
    public void getSchemaTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();

        try (NetconfClientSession session =
                dispatcher.createClient(createSHHConfig(sessionListener))
                        .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            final NetconfMessage schemaResponse = sendRequestToDevice(GET_SCHEMAS_REQUEST_XML, sessionListener);

            final NodeList schema = schemaResponse.getDocument().getDocumentElement().getElementsByTagName("schema");
            assertTrue(schema.getLength() > 0);

            boolean networkTopologySchemaContained = false;
            for (int i = 0; i < schema.getLength(); i++) {
                if (schema.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    final Element item = (Element) schema.item(i);
                    final String schemaName = item.getElementsByTagName("identifier").item(0).getTextContent();
                    final String schemaNameSpace = item.getElementsByTagName("namespace").item(0).getTextContent();
                    if ("network-topology".equals(schemaName)
                            && "urn:TBD:params:xml:ns:yang:network-topology".equals(schemaNameSpace)) {
                        networkTopologySchemaContained = true;
                        break;
                    }
                }
            }
            assertTrue(networkTopologySchemaContained);
        }
    }

    @Test
    public void deviceConfigOperationsTest() throws InterruptedException, ExecutionException,
            IOException, TimeoutException, URISyntaxException, SAXException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();
        try (NetconfClientSession session =
                dispatcher.createClient(createSHHConfig(sessionListener))
                        .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            final NetconfMessage createTopoResponse =
                    sendRequestToDevice(CREATE_TOPOLOGY_CONFIG_REQUEST_XML, sessionListener);
            assertTrue(containsOkElement(createTopoResponse));

            final NetconfMessage mergeTopoResponse =
                    sendRequestToDevice(MERGE_TOPOLOGY_CONFIG_REQUEST_XML, sessionListener);
            assertTrue(containsOkElement(mergeTopoResponse));

            final NetconfMessage getConfigDataResponse = sendRequestToDevice(GET_CONFIG_REQUEST_XML, sessionListener);

            final NodeList topologies = getConfigDataResponse.getDocument().getElementsByTagName("topology");
            assertEquals(topologies.getLength(), 2);
            if (getTopologyID(topologies.item(0)).equals("test-config-topology")) {
                assertEquals(getTopologyID(topologies.item(0)), "test-config-topology");
                assertEquals(getTopologyID(topologies.item(1)), "test-config-topology-merge");
            } else {
                assertEquals(getTopologyID(topologies.item(1)), "test-config-topology");
                assertEquals(getTopologyID(topologies.item(0)), "test-config-topology-merge");
            }

            final NodeList nodes = getConfigDataResponse.getDocument().getElementsByTagName("node");
            assertEquals(nodes.getLength(), 1);

            final NetconfMessage deleteTopologyResponse =
                    sendRequestToDevice(DELETE_TOPOLOGY_CONFIG_REQUEST_XML, sessionListener);
            assertTrue(containsOkElement(deleteTopologyResponse));

            final NetconfMessage getConfigDataResponseAfterDelete =
                    sendRequestToDevice(GET_CONFIG_REQUEST_XML, sessionListener);
            final NodeList topologiesAfterDelete =
                    getConfigDataResponseAfterDelete.getDocument().getElementsByTagName("topology");
            assertEquals(topologiesAfterDelete.getLength(), 1);
        }
    }

    private boolean containsOkElement(final NetconfMessage responseMessage) {
        return responseMessage.getDocument().getElementsByTagName("ok").getLength() > 0;
    }

    private String getTopologyID(final Node topologyItem) {
        return topologyItem.getChildNodes().item(1).getChildNodes().item(0).getNodeValue();
    }

    @Test
    public void deviceRpcTest() throws ExecutionException, InterruptedException, IOException, URISyntaxException,
            SAXException, TimeoutException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();
        try (NetconfClientSession session =
                dispatcher.createClient(createSHHConfig(sessionListener))
                        .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {

            final NetconfMessage createTopoResponse =
                    sendRequestToDevice(CREATE_TOPOLOGY_RPC_REQUEST_XML, sessionListener);
            assertResponseIsIdentical(createTopoResponse, xmlFileToInputStream(CREATE_TOPOLOGY_RPC_RESPONSE_XML));

            final NetconfMessage addNodeResponse = sendRequestToDevice(ADD_NODE_RPC_REQUEST_XML, sessionListener);
            assertResponseIsIdentical(addNodeResponse, xmlFileToInputStream(EXCEPTED_ADD_NODE_RESPONSE));

            final NetconfMessage deleteTopoResponse =
                    sendRequestToDevice(DELETE_TOPOLOGY_RPC_REQUEST_XML, sessionListener);
            assertResponseIsIdentical(deleteTopoResponse, xmlFileToInputStream(DELETE_TOPOLOGY_RPC_RESPONSE_XML));
        }
    }

    @Test
    public void testCapabilitiesFormat() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();

        try (NetconfClientSession session =
                dispatcher.createClient(createSHHConfig(sessionListener))
                        .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            final NetconfMessage schemaResponse = sendRequestToDevice(GET_SCHEMAS_REQUEST_XML, sessionListener);

            final NodeList schema = schemaResponse.getDocument().getDocumentElement().getElementsByTagName("schema");
            for (int i = 0; i < schema.getLength(); i++) {
                if (schema.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    final Element element = (Element) schema.item(i);
                    final String schemaFormat = element.getElementsByTagName("format")
                            .item(0).getFirstChild().getTextContent();
                    final String location = element.getElementsByTagName("location")
                            .item(0).getFirstChild().getTextContent();
                    final String namespace = element.getElementsByTagName("namespace")
                            .item(0).getFirstChild().getTextContent();
                    assertEquals(schemaFormat, "yang");
                    assertEquals(location, "NETCONF");
                    assertNotNull(namespace);
                }
            }
        }
    }

    private NetconfMessage sendRequestToDevice(String requestFileName,
                                               SimpleNetconfClientSessionListener sessionListener)
            throws SAXException, IOException, URISyntaxException,
            InterruptedException, ExecutionException, TimeoutException {

        final NetconfMessage requestMessage =
                new NetconfMessage(XmlUtil.readXmlToDocument(xmlFileToInputStream(requestFileName)));

        return sessionListener.sendRequest(requestMessage)
                .get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void assertResponseIsIdentical(final NetconfMessage response, final InputStream comparedResponse) {
        assertNotNull(response);
        final var actual = Input.fromString(response.toString()).build();
        final var expected = Input.fromStream(comparedResponse).build();

        final var diff = DiffBuilder.compare(actual).withTest(expected)
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
            .checkForIdentical()
            .ignoreWhitespace()
            .build();
        assertFalse(diff.hasDifferences(), "XML identical " + diff);
    }
}
