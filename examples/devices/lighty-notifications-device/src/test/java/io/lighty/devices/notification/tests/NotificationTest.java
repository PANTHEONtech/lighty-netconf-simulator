/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.devices.notification.tests;

import static org.testng.Assert.assertTrue;

import io.lighty.netconf.device.notification.Main;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
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
import org.opendaylight.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.netconf.common.impl.DefaultNetconfTimer;
import org.opendaylight.netconf.transport.api.UnsupportedConfigurationException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.crypto.types.rev241010.password.grouping.password.type.CleartextPasswordBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.client.rev240814.netconf.client.initiate.stack.grouping.transport.ssh.ssh.SshClientParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.client.rev240814.netconf.client.initiate.stack.grouping.transport.ssh.ssh.TcpClientParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.client.rev241010.ssh.client.grouping.ClientIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.client.rev241010.ssh.client.grouping.client.identity.PasswordBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class NotificationTest {

    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;
    private static final String USER = "admin";
    private static final String PASS = "admin";
    private static final int DEVICE_SIMULATOR_PORT = 9090;
    private static final String SUBCRIBE_TO_NOTIFICATIONS_REQUEST_XML = "subcribe_to_notifications_request.xml";
    private static final String TRIGGER_DATA_NOTIFICATION_REQUEST_XML = "trigger_data_notification_request.xml";
    private static final String SUBSCRIBE_MSG_TAG = "m-2";
    private static final String EXPECTED_NOTIFICATION_PAYLOAD = "Test Notification";
    private static final String GET_SCHEMAS_REQUEST_XML = "get_schemas_request.xml";
    private static Main deviceSimulator;
    private static NetconfClientFactory dispatcher;

    @BeforeAll
    public static void setupClass() {
        deviceSimulator = new Main();
        deviceSimulator.start(new String[]{DEVICE_SIMULATOR_PORT + ""}, false);
        dispatcher = new NetconfClientFactoryImpl(new DefaultNetconfTimer());
    }

    @AfterAll
    public static void cleanUpClass() throws InterruptedException {
        deviceSimulator.shutdown();
    }

    private static NetconfClientConfiguration createSHHConfig(final NetconfClientSessionListener sessionListener) {
        return NetconfClientConfigurationBuilder.create()
            .withTcpParameters(new TcpClientParametersBuilder()
                .setRemoteAddress(new Host(new IpAddress(Ipv4Address.getDefaultInstance("127.0.0.1"))))
                .setRemotePort(new PortNumber(Uint16.valueOf(DEVICE_SIMULATOR_PORT))).build())
                .withSessionListener(sessionListener)
            .withConnectionTimeoutMillis(NetconfClientConfigurationBuilder.DEFAULT_CONNECTION_TIMEOUT_MILLIS)
            .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH)
            .withSshParameters(new SshClientParametersBuilder().setClientIdentity(new ClientIdentityBuilder()
                    .setUsername(USER)
                    .setPassword(new PasswordBuilder()
                        .setPasswordType(new CleartextPasswordBuilder()
                            .setCleartextPassword(PASS)
                            .build())
                        .build())
                    .build())
                .build())
            .build();
    }

    public static InputStream xmlFileToInputStream(final String fileName) throws URISyntaxException, IOException {
        final URL getRequest = NotificationTest.class.getClassLoader().getResource(fileName);
        return new FileInputStream(new File(Objects.requireNonNull(getRequest).toURI()));
    }

    @Test
    public void getNotificationSchemaTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException, UnsupportedConfigurationException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();

        try (NetconfClientSession session =
                dispatcher.createClient(createSHHConfig(sessionListener))
                        .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            final NetconfMessage schemaResponse = sendRequesttoDevice(sessionListener, GET_SCHEMAS_REQUEST_XML);

            final NodeList schema = schemaResponse.getDocument().getDocumentElement().getElementsByTagName("schema");
            assertTrue(schema.getLength() > 0);

            boolean notificationSchemaContained = false;
            for (int i = 0; i < schema.getLength(); i++) {
                if (schema.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    final Element item = (Element) schema.item(i);
                    final String schemaName = item.getElementsByTagName("identifier").item(0).getTextContent();
                    final String schemaNameSpace = item.getElementsByTagName("namespace").item(0).getTextContent();
                    if ("lighty-test-notifications".equals(schemaName)
                            && "yang:lighty:test:notifications".equals(schemaNameSpace)) {
                        notificationSchemaContained = true;
                        break;
                    }
                }
            }
            assertTrue(notificationSchemaContained);
        }
    }

    @Test
    public void triggerNotificationRpcTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException, UnsupportedConfigurationException {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final NotificationNetconfSessionListener sessionListener =
                new NotificationNetconfSessionListener(countDownLatch, EXPECTED_NOTIFICATION_PAYLOAD);

        try (NetconfClientSession session =
                dispatcher.createClient(createSHHConfig(sessionListener))
                        .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            final NetconfMessage subscribeResponse =
                    sendRequesttoDevice(sessionListener, SUBCRIBE_TO_NOTIFICATIONS_REQUEST_XML);

            final boolean okPresent =
                    subscribeResponse.getDocument().getDocumentElement().getElementsByTagName("ok").getLength() > 0;
            assertTrue(okPresent);

            final boolean msgIdMatches = subscribeResponse.getDocument()
                    .getDocumentElement().getAttribute("message-id").equals(SUBSCRIBE_MSG_TAG);

            assertTrue(msgIdMatches);

            sendRequesttoDevice(sessionListener, TRIGGER_DATA_NOTIFICATION_REQUEST_XML);

            final boolean isNotificationPublished = countDownLatch.await(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            assertTrue(isNotificationPublished);
        }
    }

    private NetconfMessage sendRequesttoDevice(SimpleNetconfClientSessionListener sessionListener,
                                               String requestFileName)
            throws SAXException, IOException, URISyntaxException,
            InterruptedException, ExecutionException, TimeoutException {

        final NetconfMessage netconfMessage =
                new NetconfMessage(XmlUtil.readXmlToDocument(xmlFileToInputStream(requestFileName)));

        return sessionListener
                .sendRequest(netconfMessage)
                .get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }
}
