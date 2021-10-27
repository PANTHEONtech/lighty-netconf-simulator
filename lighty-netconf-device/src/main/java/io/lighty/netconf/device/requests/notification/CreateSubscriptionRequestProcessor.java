/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests.notification;

import io.lighty.codecs.api.SerializationException;
import io.lighty.netconf.device.requests.BaseRequestProcessor;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.response.ResponseData;
import io.lighty.netconf.device.utils.RPCUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class CreateSubscriptionRequestProcessor extends BaseRequestProcessor {

    private static final String CREATE_SUBSCRIPTION_RPC_NAME = "create-subscription";
    private static final Logger LOG = LoggerFactory.getLogger(CreateSubscriptionRequestProcessor.class);
    private String messageId;

    @Override
    public QName getIdentifier() {
        return QName.create(CreateSubscriptionInput.QNAME.getNamespace(), CREATE_SUBSCRIPTION_RPC_NAME);
    }

    @Override
    protected CompletableFuture<Response> execute(Element requestXmlElement) {
        messageId = requestXmlElement.getAttribute("message-id");
        final CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        responseFuture.complete(new ResponseData(Collections.emptyList()));
        return responseFuture;
    }

    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    @Override
    protected Document wrapToFinalDocumentReply(
        List<NormalizedNode> responseOutput) throws ParserConfigurationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document newDocument = builder.newDocument();

            // convert normalized nodes to xml nodes
            List<Node> outputNodes = Collections.singletonList(RPCUtil.createOkNode(newDocument));
            List<Node> wrappedOutputNodes = new ArrayList<>();
            outputNodes.forEach(outputNode -> wrappedOutputNodes.add(newDocument.importNode(outputNode, true)));

            Element rpcReply = newDocument.createElementNS(RPCUtil.NETCONF_BASE_NAMESPACE, "rpc-reply");
            rpcReply.setAttribute("message-id", messageId);
            wrappedOutputNodes.forEach(rpcReply::appendChild);
            newDocument.appendChild(rpcReply);

            String formattedResponse = RPCUtil.formatXml(newDocument.getDocumentElement());
            LOG.debug("Response: {}.", formattedResponse);
            return newDocument;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error while creating create-subscription reply XML document");
        }
    }

    @Override
    protected String convertNormalizedNodeToXmlString(NormalizedNode normalizedNode)
        throws SerializationException {
        throw new IllegalStateException("This method should not be called!");
    }
}
