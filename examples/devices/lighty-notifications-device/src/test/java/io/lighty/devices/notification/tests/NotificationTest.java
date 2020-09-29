package io.lighty.devices.notification.tests;

import static org.testng.Assert.assertTrue;
import static org.xmlunit.assertj.XmlAssert.assertThat;

import io.lighty.netconf.device.NetconfDevice;
import io.lighty.netconf.device.notification.Main;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
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
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.client.NetconfClientDispatcher;
import org.opendaylight.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.NetconfClientSessionListener;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.netconf.nettyutil.NeverReconnectStrategy;
import org.opendaylight.netconf.nettyutil.handler.ssh.authentication.LoginPasswordHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class NotificationTest {

    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;
    private static final String USER = "admin";
    private static final String PASS = "admin";
    private static final int DEVICE_SIMULATOR_PORT = 9090;

    private static Main deviceSimulator;
    private static NioEventLoopGroup nettyGroup;
    private static NetconfClientDispatcherImpl dispatcher;
    private static NetconfDevice netconfDevice;

    @BeforeAll
    public static void setupClass() {
        deviceSimulator = new Main();
        deviceSimulator.start(new String[]{DEVICE_SIMULATOR_PORT + ""}, false);
        nettyGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(NetconfClientDispatcher.class));
        dispatcher = new NetconfClientDispatcherImpl(nettyGroup, nettyGroup, new HashedWheelTimer());
        netconfDevice = deviceSimulator.getNetconfDevice();
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
                .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH)
                .withAuthHandler(new LoginPasswordHandler(USER, PASS))
                .build();
    }

    public static InputStream xmlFileToInputStream(final String fileName) throws URISyntaxException, IOException {
        final URL getRequest = NotificationTest.class.getClassLoader().getResource(fileName);
        return new FileInputStream(new File(Objects.requireNonNull(getRequest).toURI()));
    }

    @Test
    public void getNotificationSchemaTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
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

            boolean notificationSchemaContained = false;
            for (int i = 0; i < schema.getLength(); i++) {
                if (schema.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    final Element item = (Element) schema.item(i);
                    final String schemaName = item.getElementsByTagName("identifier").item(0).getTextContent();
                    final String schemaNameSpace = item.getElementsByTagName("namespace").item(0).getTextContent();
                    if ("lighty-test-notifications".equals(schemaName)
                            && "yang:lighty:test:notifications".equals(schemaNameSpace)) {
                        notificationSchemaContained = true;
                    }
                }
            }
            assertThat(notificationSchemaContained);
        }
    }

    @Test
    public void triggerNotificationRpcTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException {

        final CountDownLatch cdl = new CountDownLatch(1);
        netconfDevice.getNetconfDeviceServices().getNotificationService()
                .registerNotificationListener(new DataNotificationListener(cdl));

        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();
        final NetconfMessage getSchemaMessage = new NetconfMessage(
                XmlUtil.readXmlToDocument(xmlFileToInputStream("trigger_data_notification_request.xml")));

        try (NetconfClientSession session = dispatcher.createClient(createSHHConfig(sessionListener)).get()) {
            final NetconfMessage getSchemaResponse = sessionListener
                    .sendRequest(getSchemaMessage)
                    .get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            int responseLength = getSchemaResponse.getDocument().getElementsByTagName("ok").getLength();
            assertTrue(responseLength > 0);
            final boolean isNotificationPublished = cdl.await(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            assertThat(isNotificationPublished);
        }
    }
}
