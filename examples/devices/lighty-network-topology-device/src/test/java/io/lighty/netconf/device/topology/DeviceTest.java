/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.netconf.device.topology;

import static io.lighty.netconf.device.topology.TestUtils.xmlFileToInputStream;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.xmlunit.assertj.XmlAssert.assertThat;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.ByteArrayInputStream;
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
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.client.NetconfClientDispatcher;
import org.opendaylight.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.NetconfClientSessionListener;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration.NetconfClientProtocol;
import org.opendaylight.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.netconf.nettyutil.NeverReconnectStrategy;
import org.opendaylight.netconf.nettyutil.handler.ssh.authentication.LoginPasswordHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DeviceTest {

    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;
    private static final String USER = "admin";
    private static final String PASS = "admin";
    private static final int DEVICE_SIMULATOR_PORT = 9090;
    private static final String CREATE_TOPOLOGY_REQUEST_XML = "create_topology_request.xml";
    private static final String DELETE_TOPOLOGY_REQUEST_XML = "delete_topology_request.xml";
    private static final String ADD_NODE_REQUEST_XML = "add_node_request.xml";
    private static final String EXPECTED_VALUES_XML = "expected_values.xml";

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
                .withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE,
                        NetconfClientConfigurationBuilder.DEFAULT_CONNECTION_TIMEOUT_MILLIS))
                .withProtocol(NetconfClientProtocol.SSH)
                .withAuthHandler(new LoginPasswordHandler(USER, PASS))
                .build();
    }

    @Test
    public void getSchemaTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();
        final NetconfMessage getSchemaMessage =
                new NetconfMessage(XmlUtil.readXmlToDocument(xmlFileToInputStream("get_schemas_request.xml")));

        try (NetconfClientSession session = dispatcher.createClient(createSHHConfig(sessionListener)).get()) {
            final NetconfMessage getSchemaResponse = sessionListener
                    .sendRequest(getSchemaMessage)
                    .get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            final NodeList schema = getSchemaResponse.getDocument().getDocumentElement().getElementsByTagName("schema");
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
                    }
                }
            }
            assertThat(networkTopologySchemaContained);
        }
    }

    @Test
    public void topologyRpcsTest() throws ExecutionException, InterruptedException, IOException, URISyntaxException,
            SAXException, TimeoutException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();
        try (NetconfClientSession session = dispatcher.createClient(createSHHConfig(sessionListener)).get()) {
            final Document expectedValuesDoc = XmlUtil.readXmlToDocument(xmlFileToInputStream(EXPECTED_VALUES_XML));

            final NetconfMessage createTopoResponse = performNetconfRequest(CREATE_TOPOLOGY_REQUEST_XML,
                    sessionListener);
            validateNetconfResponse(createTopoResponse, expectedValuesDoc, "create_topo");

            final NetconfMessage addNodeResponse = performNetconfRequest(ADD_NODE_REQUEST_XML, sessionListener);
            validateNetconfResponse(addNodeResponse, expectedValuesDoc, "add_node");

            final NetconfMessage deleteTopoResponse =
                    performNetconfRequest(DELETE_TOPOLOGY_REQUEST_XML, sessionListener);
            validateNetconfResponse(deleteTopoResponse, expectedValuesDoc, "delete_topo");
        }
    }

    private void validateNetconfResponse(NetconfMessage netconfResponse, Document expectedValuesDoc, String rpcAction) {
        final String expectedResponse = expectedValuesDoc.getDocumentElement()
                .getElementsByTagName(rpcAction).item(0).getAttributes().item(0).getNodeValue();

        assertResponseIsIdentical(netconfResponse, new ByteArrayInputStream(expectedResponse.getBytes()));
    }

    private NetconfMessage performNetconfRequest(String fileName, SimpleNetconfClientSessionListener sessionListener)
            throws SAXException, IOException, URISyntaxException,
            InterruptedException, ExecutionException, TimeoutException {

        final NetconfMessage getSchemaMessage =
                new NetconfMessage(XmlUtil.readXmlToDocument(xmlFileToInputStream(fileName)));

        return sessionListener.sendRequest(getSchemaMessage)
                .get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void assertResponseIsIdentical(final NetconfMessage response, final InputStream comparedResponse) {
        assertNotNull(response);
        assertThat(response.getDocument())
                .and(comparedResponse)
                .ignoreWhitespace()
                .areIdentical();
    }
}
