/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster;

import static org.testng.Assert.assertTrue;

import io.lighty.netconf.device.utils.TimeoutUtil;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.netconf.api.messages.NetconfMessage;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.client.NetconfClientFactory;
import org.opendaylight.netconf.client.NetconfClientFactoryImpl;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.NetconfClientSessionListener;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration.NetconfClientProtocol;
import org.opendaylight.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.netconf.common.impl.DefaultNetconfTimer;
import org.opendaylight.netconf.nettyutil.AbstractNetconfSession;
import org.opendaylight.netconf.nettyutil.handler.ssh.authentication.LoginPasswordHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DeviceTest {

    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;
    private static final String USER = "admin";
    private static final String PASS = "admin";
    private static final int DEVICE_STARTING_PORT = 20000;
    private static final int DEVICE_COUNT = 5;
    private static final int THREAD_POOL_SIZE = 5;
    private static final String EXPECTED_DARKNESS_FACTOR = "750";
    private static final String CREATE_TOASTER_REQUEST_XML = "create_toaster_request.xml";
    private static final String GET_TOASTER_DATA_REQUEST_XML = "get_toaster_data_request.xml";
    private static final String MAKE_TOAST_REQUEST_XML = "make_toast_request.xml";
    public static final String GET_SCHEMAS_REQUEST_XML = "get_schemas_request.xml";
    private static final List<SimpleNetconfClientSessionListener> SESSION_LISTENERS = new ArrayList<>();
    private static final List<NetconfClientSession> NETCONF_CLIENT_SESSIONS = new ArrayList<>();
    private static Main deviceSimulator;
    private static NioEventLoopGroup nettyGroup;

    @BeforeAll
    public static void setUpClass() throws InterruptedException, ExecutionException,
        TimeoutException, UnsupportedConfigurationException {
        deviceSimulator = new Main();
        deviceSimulator.start(new String[]{"--starting-port",
                        String.valueOf(DEVICE_STARTING_PORT), "--thread-pool-size",
                        String.valueOf(THREAD_POOL_SIZE), "--device-count", String.valueOf(DEVICE_COUNT)},
                true, false);
        nettyGroup = new NioEventLoopGroup(THREAD_POOL_SIZE, new DefaultThreadFactory(NetconfClientFactory.class));
        NetconfClientFactory dispatcher =
                new NetconfClientFactoryImpl(new DefaultNetconfTimer());
        for (int port = DEVICE_STARTING_PORT; port < DEVICE_STARTING_PORT + DEVICE_COUNT; port++) {
            final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();
            NetconfClientSession session = dispatcher.createClient(createSHHConfig(sessionListener, port))
                    .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            NETCONF_CLIENT_SESSIONS.add(session);
            SESSION_LISTENERS.add(sessionListener);
        }
    }

    @AfterAll
    public static void cleanUpClass() throws InterruptedException {
        NETCONF_CLIENT_SESSIONS.forEach(AbstractNetconfSession::close);
        deviceSimulator.shutdown();
        nettyGroup.shutdownGracefully().sync();
    }

    @Test
    public void sharedDatastoreTest() throws InterruptedException, ExecutionException,
            TimeoutException, SAXException, IOException, URISyntaxException {
        final SimpleNetconfClientSessionListener sessionListener = SESSION_LISTENERS.get(0);
        final NetconfMessage createToasterResponse = sendRequestToDevice(CREATE_TOASTER_REQUEST_XML, sessionListener);
        assertTrue(containsOkElement(createToasterResponse));
        for (SimpleNetconfClientSessionListener listener : SESSION_LISTENERS) {
            final NetconfMessage toasterData = sendRequestToDevice(GET_TOASTER_DATA_REQUEST_XML, listener);
            final String toasterDarknessFactor = toasterData.getDocument()
                    .getDocumentElement().getElementsByTagName("darknessFactor").item(0).getTextContent();
            Assertions.assertEquals(EXPECTED_DARKNESS_FACTOR, toasterDarknessFactor);
        }
    }

    @Test
    public void getSchemaTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException {
        for (SimpleNetconfClientSessionListener listener : SESSION_LISTENERS) {
            final NetconfMessage schemaResponse = sendRequestToDevice(GET_SCHEMAS_REQUEST_XML,
                    listener);
            final NodeList schema = schemaResponse.getDocument().getDocumentElement().getElementsByTagName("schema");
            Assertions.assertTrue(schema.getLength() > 0);
            boolean toasterSchemaContained = false;
            for (int i = 0; i < schema.getLength(); i++) {
                if (schema.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    final Element item = (Element) schema.item(i);
                    final String schemaName = item.getElementsByTagName("identifier").item(0).getTextContent();
                    final String schemaNameSpace = item.getElementsByTagName("namespace").item(0).getTextContent();
                    if ("toaster".equals(schemaName)
                            && "http://netconfcentral.org/ns/toaster".equals(schemaNameSpace)) {
                        toasterSchemaContained = true;
                        break;
                    }
                }
            }
            Assertions.assertTrue(toasterSchemaContained);
        }
    }

    @Test
    public void toasterRPCsTest() throws ExecutionException, InterruptedException, URISyntaxException, SAXException,
            TimeoutException, IOException {
        for (SimpleNetconfClientSessionListener listener : SESSION_LISTENERS) {
            final NetconfMessage makeToastResponse =
                    sendRequestToDevice(MAKE_TOAST_REQUEST_XML, listener);
            Assertions.assertTrue(containsOkElement(makeToastResponse));
        }
    }

    private static NetconfClientConfiguration createSHHConfig(final NetconfClientSessionListener sessionListener,
                                                              Integer port) {
        return NetconfClientConfigurationBuilder.create()
                .withAddress(new InetSocketAddress("localhost", port))
                .withSessionListener(sessionListener)
                .withConnectionTimeoutMillis(NetconfClientConfigurationBuilder.DEFAULT_CONNECTION_TIMEOUT_MILLIS)
                .withProtocol(NetconfClientProtocol.SSH)
                .withAuthHandler(new LoginPasswordHandler(USER, PASS))
                .build();
    }

    private boolean containsOkElement(final NetconfMessage responseMessage) {
        return responseMessage.getDocument().getElementsByTagName("ok").getLength() > 0;
    }

    private NetconfMessage sendRequestToDevice(String requestFileName,
                                               SimpleNetconfClientSessionListener sessionListener)
            throws SAXException, IOException, URISyntaxException,
            InterruptedException, ExecutionException, TimeoutException {

        final NetconfMessage requestMessage =
                new NetconfMessage(XmlUtil.readXmlToDocument(xmlFileToInputStream(requestFileName)));

        return sessionListener.sendRequest(requestMessage).get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private InputStream xmlFileToInputStream(final String fileName) throws URISyntaxException, IOException {
        final URL getRequest = DeviceTest.class.getClassLoader().getResource(fileName);
        return new FileInputStream(new File(Objects.requireNonNull(getRequest).toURI()));
    }
}
