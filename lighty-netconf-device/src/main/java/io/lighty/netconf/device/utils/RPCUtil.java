/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Constants and util class for RPCs.
 *
 */
public final class RPCUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RPCUtil.class);
    public static final String BLANK = "";

    private RPCUtil() {
        throw new UnsupportedOperationException("do not instantiate utility class");
    }

    public static final String NETCONF_BASE_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    public static final String CREATE_SUBSCRIPTION_NAMESPACE = "urn:ietf:params:xml:ns:netconf:notification:1.0";

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    static {
        // When parsing the XML file, the content of the external entities is retrieved from an external storage such as
        // the file system or network, which may lead, if no restrictions are put in place, to arbitrary file
        // disclosures or server-side request forgery (SSRF) vulnerabilities.
        // https://rules.sonarsource.com/java/RSPEC-2755
        TRANSFORMER_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        TRANSFORMER_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    }

    /**
     * Transform {@link Element} instance into {@link Reader}.
     *
     * @param requestXmlElement Input {@link Element} instance.
     * @return {@link Reader} instance containing input XML element;
     * @throws TransformerException In case transformation fails;
     */
    public static Reader createReaderFromElement(Element requestXmlElement) throws TransformerException {
        Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        StringWriter sw = new StringWriter();
        StreamResult sr = new StreamResult(sw);
        DOMSource domSource = new DOMSource(requestXmlElement);
        transformer.transform(domSource, sr);
        return new StringReader(sw.toString());
    }

    public static List<NormalizedNode> createNormalizedNodesForMapNodeEntries(
        Optional<NormalizedNode> listNormalizedNode) {
        List<NormalizedNode> normalizedNodes = new ArrayList<>();
        if (listNormalizedNode.isEmpty()) {
            return normalizedNodes;
        }
        DataContainerChild nextToCollection = (DataContainerChild) listNormalizedNode.get();
        Iterator iterator = ((Collection) nextToCollection.body()).iterator();
        while (iterator.hasNext()) {
            NormalizedNode fabricListEntry =
                    (NormalizedNode) iterator.next();
            NormalizedNode fabricListEntryInsideMapNode = ImmutableNodes.newSystemMapBuilder()
                    .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier
                            .create(listNormalizedNode.get().name().getNodeType()))
                    .withChild((MapEntryNode) fabricListEntry).build();
            normalizedNodes.add(fabricListEntryInsideMapNode);
        }
        return normalizedNodes;
    }

    /**
     * Formats the given input xml.
     *
     * @param xml the XML element to format.
     * @return String the formatted XML element.
     */
    public static String formatXml(Element xml) {
        try {
            Transformer tf = TRANSFORMER_FACTORY.newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            Writer outWriter = new StringWriter();
            tf.transform(new DOMSource(xml), new StreamResult(outWriter));
            return outWriter.toString();
        } catch (TransformerException e) {
            LOG.warn("Could not format XML element, {}", e.getMessage());
            return BLANK;
        }
    }

    public static String formatXml(Node node) {
        return formatXml((Element) node);
    }

    /**
     * Creates an OK node which is a part of sample NETCONF RPCs.
     *
     * @param document context document
     * @return created &lt;ok /&gt; with namespace {@link RPCUtil#NETCONF_BASE_NAMESPACE}
     */
    public static Node createOkNode(Document document) {
        return document.createElementNS(NETCONF_BASE_NAMESPACE, "ok");
    }

    /**
     * Converts the {@link NodeList} into {@link List} of {@link Node} without types
     * given as parameter.
     *
     * @param nodes the nodes list
     * @param toBeSkipped a list of node types to be skipped
     * @return {@code List<Node>} a list of nodes
     */
    public static List<Node> getNodes(NodeList nodes, Set<Short> toBeSkipped) {
        List<Node> resultNodes = Lists.newLinkedList();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node item = nodes.item(i);
            if (!toBeSkipped.contains(item.getNodeType())) {
                resultNodes.add(item);
            }
        }
        return resultNodes;
    }

    /**
     * Converts the input {@link NodeList} into {@link List} of {@link Node}s without
     * {@link Node#TEXT_NODE}.
     *
     * @see RPCUtil#getNodes(NodeList, Set)
     * @param nodes a nodes list
     * @return {@code List<Node>} a list of nodes
     */
    public static List<Node> getNodes(NodeList nodes) {
        return getNodes(nodes, ImmutableSet.of(Node.TEXT_NODE));
    }

    public static Optional<Operation> retrieveOperation(Element element) {
        try {
            Node foundNode = retrieveNodeListByXpath(element, "//*[@*[local-name() = 'operation']]").item(0);
            if (foundNode == null) {
                return Optional.empty();
            }
            return Optional.of(Operation.getOperationByName(foundNode));
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Optional<DefaultOperation> retrieveDefaultOperation(Element element) {
        try {
            Node foundNode = retrieveNodeListByXpath(element, "//*[local-name() = 'default-operation']").item(0);
            if (foundNode == null) {
                return Optional.empty();
            }
            return Optional.of(DefaultOperation.getOperationByName(foundNode));
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NodeList retrieveNodeListByXpath(Element element, String xpath) throws XPathExpressionException {
        return (NodeList) XPathFactory.newInstance().newXPath().compile(xpath)
                .evaluate(element, XPathConstants.NODESET);
    }

}
