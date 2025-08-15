/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import io.lighty.codecs.util.exception.SerializationException;
import io.lighty.netconf.device.NetconfDeviceServices;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Base class which implements the {@link RequestProcessor} interface.
 * This implementation cannot handle the input parameters and therefore is
 * suitable only for RPCs without any parameter served.
 */
public abstract class BaseRequestProcessor implements RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BaseRequestProcessor.class);

    private final DocumentBuilderFactory factory;
    private NetconfDeviceServices netconfDeviceServices;

    public BaseRequestProcessor() {
        this.factory = DocumentBuilderFactory.newInstance();
        this.factory.setNamespaceAware(true);
    }

    @Override
    public void init(NetconfDeviceServices paramNetconfDeviceServices) {
        this.netconfDeviceServices = paramNetconfDeviceServices;
    }

    /**
     * Should process the input {@link Element} and return data in form of {@link NormalizedNode}. {@link List} enables
     * method to return more than one top elements.
     *
     * <p>The child classes should override this method to achieve the expected behavior.
     *
     * @param requestXmlElement XML RPC request element
     * @return {@link List} containing {@link NormalizedNode}s to be returned by request.
     */
    protected abstract CompletableFuture<Response> execute(Element requestXmlElement);

    protected NetconfDeviceServices getNetconfDeviceServices() {
        return netconfDeviceServices;
    }

    protected DocumentBuilderFactory getDocumentBuilderFactory() {
        return factory;
    }

    @Override
    public Document processRequest(Element requestXmlElement) {
        try {
            CompletableFuture<Response> responseOutput = execute(requestXmlElement);
            return processResponse(responseOutput);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while processing XML request: {}", requestXmlElement, e);
            throw new IllegalStateException(e);
        } catch (ExecutionException | ParserConfigurationException | TimeoutException e) {
            LOG.error("Could not process XML request: {}", requestXmlElement, e);
            try {
                final DocumentedException error = NetconfDocumentedException.wrap(e);
                return error.toXMLDocument();
            } catch (DocumentedException ex) {
                LOG.error("Could not wrap exception", ex);
                return ex.toXMLDocument();
            }
        }
    }

    private Document processResponse(final CompletableFuture<Response> responseOutput)
            throws ExecutionException, InterruptedException, ParserConfigurationException, TimeoutException {
        final Response listResponse = responseOutput.get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        final Document error = listResponse.getErrorDocument();
        if (error != null) {
            return error;
        } else {
            return wrapToFinalDocumentReply(listResponse.getData());
        }
    }

    protected abstract Document wrapToFinalDocumentReply(List<NormalizedNode> responseOutput)
        throws ParserConfigurationException;

    /**
     * Prepares the output node. By default it parses calls the
     * {@link BaseRequestProcessor#execute(Element)} method
     * which returns a object representation of the RPC result.
     *
     * @param responseOutput output from execution of request
     * @param builder        a document builder
     * @param document       a document
     * @return Node the output node
     */
    protected List<Node> convertOutputToXmlNodes(List<NormalizedNode> responseOutput, DocumentBuilder builder,
                                                 Document document) {
        Node responseOutputAsXmlNode;
        if (responseOutput.isEmpty()) {
            LOG.error("Response output is not present!");
            throw new IllegalStateException("Response output is not present!");
        }
        List<String> responseOutputString = convertOutputNormalizedNodesToXmlStrings(responseOutput);
        List<Node> nodes = new ArrayList<>();
        for (String string : responseOutputString) {
            try (InputStream is = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8))) {
                responseOutputAsXmlNode = builder.parse(is).getFirstChild();
                nodes.add(document.importNode(responseOutputAsXmlNode, true));
            } catch (IOException | SAXException e) {
                String msg = "Error while serializing XML: " + e.getMessage();
                LOG.error("Could not convert NN to XML, {}", msg);
                throw new IllegalStateException(msg, e);
            }
        }
        return nodes;
    }

    private List<String> convertOutputNormalizedNodesToXmlStrings(List<NormalizedNode> responseOutput) {
        List<NormalizedNode> toConvert = new ArrayList<>();
        for (NormalizedNode normalizedNode : responseOutput) {
            // in case of MapNode we need to wrap every MapEntryNode to MapNode and serialize separately
            if (normalizedNode instanceof MapNode) {
                toConvert.addAll(((MapNode) normalizedNode).body().stream().map(mapEntryNode ->
                        ImmutableNodes.newSystemMapBuilder()
                                .withNodeIdentifier(
                                        YangInstanceIdentifier.NodeIdentifier.create(normalizedNode.name()
                                                .getNodeType()))
                                .withChild(mapEntryNode)
                                .build())
                        .collect(Collectors.toList()));
            } else {
                toConvert.add(normalizedNode);
            }
        }
        List<String> converted = new ArrayList<>();
        for (NormalizedNode node : toConvert) {
            try {
                converted.add(convertNormalizedNodeToXmlString(node));
            } catch (SerializationException e) {
                String msg = "Unable to serialize binding independent object: " + NormalizedNodes.toStringTree(node);
                LOG.error(msg, e);
                throw new IllegalStateException(msg, e);
            }
        }
        return converted;
    }

    protected abstract String convertNormalizedNodeToXmlString(NormalizedNode normalizedNode)
            throws SerializationException;

}
