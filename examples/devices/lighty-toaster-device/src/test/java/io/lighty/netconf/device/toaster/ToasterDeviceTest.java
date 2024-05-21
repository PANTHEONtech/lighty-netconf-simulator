/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.opendaylight.netconf.client.NetconfClientFactoryImpl;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.NetconfClientSessionListener;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.netconf.common.impl.DefaultNetconfTimer;
import org.opendaylight.netconf.transport.api.UnsupportedConfigurationException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.crypto.types.rev240208.password.grouping.password.type.CleartextPasswordBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.client.rev240208.netconf.client.initiate.stack.grouping.transport.ssh.ssh.SshClientParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.client.rev240208.netconf.client.initiate.stack.grouping.transport.ssh.ssh.TcpClientParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.client.rev240208.ssh.client.grouping.ClientIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ssh.client.rev240208.ssh.client.grouping.client.identity.PasswordBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ToasterDeviceTest {
    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;
    private static final String USER = "admin";
    private static final String PASS = "admin";
    private static final int DEVICE_SIMULATOR_PORT = 9090;
    private static final String EXPECTED_DARKNESS_FACTOR = "750";
    private static final String MAKE_TOAST_REQUEST_XML = "make_toast_request.xml";
    private static final String RESTOCK_TOAST_REQUEST_XML = "restock_toast_request.xml";
    private static final String CREATE_TOASTER_REQUEST_XML = "create_toaster_request.xml";
    private static final String GET_TOASTER_DATA_REQUEST_XML = "get_toaster_data_request.xml";
    public static final String SUBSCRIBE_TO_NOTIFICATIONS_REQUEST_XML = "subscribe_to_notifications_request.xml";
    public static final String GET_SCHEMAS_REQUEST_XML = "get_schemas_request.xml";

    private static Main deviceSimulator;
    private static NetconfClientFactoryImpl dispatcher;

    @BeforeAll
    public static void setUpClass() {
        deviceSimulator = new Main();
        deviceSimulator.start(new String[]{DEVICE_SIMULATOR_PORT + ""}, false, false);
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

    @Test
    public void getSchemaTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException, UnsupportedConfigurationException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();

        try (NetconfClientSession session =
                dispatcher.createClient(createSHHConfig(sessionListener))
                        .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            final NetconfMessage schemaResponse = sentRequestToDevice(GET_SCHEMAS_REQUEST_XML,
                    sessionListener);

            final NodeList schema = schemaResponse.getDocument().getDocumentElement().getElementsByTagName("schema");
            assertTrue(schema.getLength() > 0);

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
            assertTrue(toasterSchemaContained);
        }
    }

    @Test
    public void toasterRpcsTest() throws ExecutionException, InterruptedException, URISyntaxException, SAXException,
            TimeoutException, IOException, UnsupportedConfigurationException {

        final SimpleNetconfClientSessionListener sessionListenerSimple =
            new SimpleNetconfClientSessionListener();

        try (NetconfClientSession sessionSimple =
                dispatcher.createClient(createSHHConfig(sessionListenerSimple))
                    .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {

            final NetconfMessage createToasterResponse =
                    sentRequestToDevice(CREATE_TOASTER_REQUEST_XML, sessionListenerSimple);
            assertTrue(containsOkElement(createToasterResponse));

            final NetconfMessage toasterData =
                    sentRequestToDevice(GET_TOASTER_DATA_REQUEST_XML, sessionListenerSimple);
            final String toasterDarknessFactor = toasterData.getDocument()
                    .getDocumentElement().getElementsByTagName("darknessFactor").item(0).getTextContent();
            assertEquals(EXPECTED_DARKNESS_FACTOR, toasterDarknessFactor);

            final NetconfMessage makeToastResponse =
                sentRequestToDevice(MAKE_TOAST_REQUEST_XML, sessionListenerSimple);
            assertTrue(containsOkElement(makeToastResponse));

            final NetconfMessage restockToastResponse =
                sentRequestToDevice(RESTOCK_TOAST_REQUEST_XML, sessionListenerSimple);
            assertTrue(containsOkElement(restockToastResponse));
        }
    }

    @Test
    public void toasterNotificationTest() throws ExecutionException, InterruptedException, URISyntaxException,
            SAXException, TimeoutException, IOException, UnsupportedConfigurationException {

        final CountDownLatch notificationReceivedLatch = new CountDownLatch(1);
        final NotificationNetconfSessionListener sessionListenerNotification =
                new NotificationNetconfSessionListener(notificationReceivedLatch);
        final SimpleNetconfClientSessionListener sessionListenerSimple =
                new SimpleNetconfClientSessionListener();

        try (NetconfClientSession sessionNotification =
                dispatcher.createClient(createSHHConfig(sessionListenerNotification))
                        .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                NetconfClientSession sessionSimple =
                        dispatcher.createClient(createSHHConfig(sessionListenerSimple))
                                .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {

            final NetconfMessage subscribeResponse =
                    sentRequestToDevice(SUBSCRIBE_TO_NOTIFICATIONS_REQUEST_XML, sessionListenerNotification);
            assertTrue(containsOkElement(subscribeResponse));

            final NetconfMessage restockToastResponse =
                    sentRequestToDevice(RESTOCK_TOAST_REQUEST_XML, sessionListenerSimple);
            assertTrue(containsOkElement(restockToastResponse));

            final boolean await = notificationReceivedLatch.await(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            assertTrue(await);

            NetconfMessage restockToastNotification = sessionListenerNotification.getReceivedNotificationMessage();
            assertNotNull(restockToastNotification);
            assertTrue(containsNotificationElement(restockToastNotification));
            final String amountOfBreadExpected = XmlUtil
                    .readXmlToDocument(xmlFileToInputStream(RESTOCK_TOAST_REQUEST_XML))
                    .getElementsByTagName("amountOfBreadToStock").item(0).getTextContent();
            final String amountOfBread = restockToastNotification.getDocument().getElementsByTagName("amountOfBread")
                    .item(0).getTextContent();
            assertEquals(amountOfBreadExpected, amountOfBread);
        }
    }

    private boolean containsOkElement(final NetconfMessage responseMessage) {
        return responseMessage.getDocument().getElementsByTagName("ok").getLength() > 0;
    }

    private boolean containsNotificationElement(final NetconfMessage responseMessage) {
        return responseMessage.getDocument().getElementsByTagName("notification").getLength() > 0;
    }

    public static NetconfMessage sentRequestToDevice(String requestFileName,
                                                     SimpleNetconfClientSessionListener sessionListener)
            throws SAXException, IOException, URISyntaxException,
            InterruptedException, ExecutionException, TimeoutException {

        final NetconfMessage actionSchemaMessage =
                new NetconfMessage(XmlUtil.readXmlToDocument(xmlFileToInputStream(requestFileName)));

        return sessionListener.sendRequest(actionSchemaMessage).get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    public static InputStream xmlFileToInputStream(final String fileName) throws URISyntaxException, IOException {
        final URL getRequest = ToasterDeviceTest.class.getClassLoader().getResource(fileName);
        return new FileInputStream(new File(Objects.requireNonNull(getRequest).toURI()));
    }

}
