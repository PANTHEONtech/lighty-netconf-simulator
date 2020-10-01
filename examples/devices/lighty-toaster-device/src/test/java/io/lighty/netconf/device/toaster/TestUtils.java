package io.lighty.netconf.device.toaster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.lighty.codecs.xml.XmlUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;
import org.xml.sax.SAXException;

public final class TestUtils {

    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;

    private TestUtils() {
    }

    public static void checkForRpcErrors(NetconfMessage netconfMessage) {
        final int errorMsgLength = netconfMessage.getDocument()
                .getDocumentElement().getElementsByTagName("rpc-error").getLength();
        assertEquals(0, errorMsgLength);
    }

    public static NetconfMessage performNetconfRequest(String fileName,
                                                       SimpleNetconfClientSessionListener sessionListener)
            throws SAXException, IOException, URISyntaxException,
            InterruptedException, ExecutionException, TimeoutException {

        final NetconfMessage actionSchemaMessage =
                new NetconfMessage(XmlUtil.readXmlToDocument(xmlFileToInputStream(fileName)));

        return sessionListener.sendRequest(actionSchemaMessage).get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    public static InputStream xmlFileToInputStream(final String fileName) throws URISyntaxException, IOException {
        final URL getRequest = ToasterDeviceTest.class.getClassLoader().getResource(fileName);
        return new FileInputStream(new File(Objects.requireNonNull(getRequest).toURI()));
    }
}
