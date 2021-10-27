/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import io.lighty.codecs.util.SerializationException;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.utils.RPCUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class OkOutputRequestProcessor extends BaseRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(OkOutputRequestProcessor.class);

    @Override
    protected final CompletableFuture<Response> execute(Element requestXmlElement) {
        return executeOkRequest(requestXmlElement);
    }

    protected abstract CompletableFuture<Response> executeOkRequest(Element requestXmlElement);

    @Override
    protected List<Node> convertOutputToXmlNodes(List<NormalizedNode> responseOutput, DocumentBuilder builder,
            Document document) {
        return Collections.singletonList(RPCUtil.createOkNode(document));
    }

    @Override
    protected String convertNormalizedNodeToXmlString(NormalizedNode normalizedNode)
            throws SerializationException {
        throw new IllegalStateException("This method should not be called!");
    }

    @Override
    protected Document wrapToFinalDocumentReply(List<NormalizedNode> responseOutput)
            throws ParserConfigurationException {
        // convert normalized nodes to xml nodes
        DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
        Document newDocument = builder.newDocument();
        List<Node> outputNodes = convertOutputToXmlNodes(responseOutput, builder, newDocument);
        // wrap nodes to final document
        List<Node> wrappedOutputNodes = new ArrayList<>();
        outputNodes.forEach(outputNode -> wrappedOutputNodes.add(newDocument.importNode(outputNode, true)));
        newDocument.appendChild(wrapResponse(newDocument, wrappedOutputNodes));
        String formattedResponse = RPCUtil.formatXml(newDocument.getDocumentElement());
        LOG.debug("Response: {}.", formattedResponse);
        return newDocument;
    }

    private Node wrapResponse(Document document, List<Node> response) {
        Element rpcReply = document.createElementNS(RPCUtil.NETCONF_BASE_NAMESPACE, "rpc-reply");
        response.forEach(rpcReply::appendChild);
        return rpcReply;
    }

}
