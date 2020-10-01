package io.lighty.netconf.device.toaster;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.codecs.xml.XmlUtil;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.netconf.api.NetconfMessage;
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
                .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH)
                .withAuthHandler(new LoginPasswordHandler(USER, PASS))
                .build();
    }

    @Test
    public void getSchemaTest() throws IOException, URISyntaxException, SAXException, InterruptedException,
            ExecutionException, TimeoutException {
        final SimpleNetconfClientSessionListener sessionListener = new SimpleNetconfClientSessionListener();
        final NetconfMessage getSchemaMessage = new NetconfMessage(
                XmlUtil.readXmlToDocument(TestUtils.xmlFileToInputStream("get_schemas_request.xml")));

        try (NetconfClientSession session = dispatcher.createClient(createSHHConfig(sessionListener)).get()) {
            final NetconfMessage getSchemaResponse = sessionListener
                    .sendRequest(getSchemaMessage)
                    .get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            final NodeList schema = getSchemaResponse.getDocument().getDocumentElement().getElementsByTagName("schema");
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
                    }
                }
            }
            assertThat(toasterSchemaContained);
        }
    }

    @Test
    public void toasterRpcsTest() throws ExecutionException, InterruptedException, URISyntaxException, SAXException,
            TimeoutException, IOException {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final SimpleNetconfClientSessionListener sessionListener = new NotificationNetconfSessionListener(countDownLatch);
        try (NetconfClientSession session = dispatcher.createClient(createSHHConfig(sessionListener)).get()) {
            final NetconfMessage createToasterResponse = createToaster(sessionListener);
            assertEquals(createToasterResponse.getDocument().getElementsByTagName("ok").item(0).getLocalName(), "ok");

            final NetconfMessage toasterData = getToaster(sessionListener);
            final String toasterDarknessFactor = toasterData.getDocument()
                    .getDocumentElement().getElementsByTagName("darknessFactor").item(0).getTextContent();

            assertEquals(EXPECTED_DARKNESS_FACTOR, toasterDarknessFactor);

            makeToast(MAKE_TOAST_REQUEST_XML, sessionListener);
            restockToast(RESTOCK_TOAST_REQUEST_XML, sessionListener);
            countDownLatch.await();
        }
    }

    private NetconfMessage createToaster(SimpleNetconfClientSessionListener sessionListener) throws InterruptedException,
            ExecutionException, IOException, TimeoutException, URISyntaxException, SAXException {

        return TestUtils.performNetconfRequest(CREATE_TOASTER_REQUEST_XML, sessionListener);
    }

    private NetconfMessage getToaster(SimpleNetconfClientSessionListener sessionListener) throws InterruptedException,
            ExecutionException, IOException, TimeoutException, URISyntaxException, SAXException {

        return TestUtils.performNetconfRequest(GET_TOASTER_DATA_REQUEST_XML, sessionListener);
    }

    public void makeToast(String fileName, SimpleNetconfClientSessionListener sessionListener)
            throws InterruptedException, ExecutionException, TimeoutException, IOException,
            URISyntaxException, SAXException {

        final NetconfMessage makeToastResponse = TestUtils.performNetconfRequest(fileName, sessionListener);
        TestUtils.checkForRpcErrors(makeToastResponse);
    }


    private void restockToast(String fileName, SimpleNetconfClientSessionListener sessionListener)
            throws InterruptedException, ExecutionException, IOException,
            TimeoutException, URISyntaxException, SAXException {

        final NetconfMessage restockToastResponse = TestUtils.performNetconfRequest(fileName, sessionListener);
        TestUtils.checkForRpcErrors(restockToastResponse);
    }

}
