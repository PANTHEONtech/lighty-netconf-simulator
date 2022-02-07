/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import io.lighty.codecs.util.ConverterUtils;
import io.lighty.codecs.util.SerializationException;
import io.lighty.netconf.device.NetconfDeviceServices;
import io.lighty.netconf.device.utils.RPCUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class RpcOutputRequestProcessor extends BaseRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RpcOutputRequestProcessor.class);

    private RpcDefinition rpcDefinition;

    @Override
    public void init(NetconfDeviceServices netconfDeviceServices) {
        super.init(netconfDeviceServices);
        EffectiveModelContext schemaContext = getNetconfDeviceServices().getAdapterContext().currentSerializer()
                .getRuntimeContext().getEffectiveModelContext();
        Optional<? extends RpcDefinition> rpcDefinitionOptional =
            ConverterUtils.loadRpc(schemaContext, getIdentifier());
        if (rpcDefinitionOptional.isPresent()) {
            this.rpcDefinition = rpcDefinitionOptional.get();
        } else {
            throw new IllegalStateException("RpcDefinition for " + getIdentifier() + " was not found!");
        }
    }

    @Override
    protected String convertNormalizedNodeToXmlString(NormalizedNode normalizedNode)
            throws SerializationException {
        return getNetconfDeviceServices().getXmlNodeConverter()
                .serializeRpc(rpcDefinition.getOutput(), normalizedNode).toString();
    }

    public RpcDefinition getRpcDefinition() {
        return rpcDefinition;
    }

    protected Absolute getRpcDefInputAbsolutePath() {
        return Absolute.of(rpcDefinition.getQName(), rpcDefinition.getInput().getQName());
    }

    @Override
    protected Document wrapToFinalDocumentReply(List<NormalizedNode> responseOutput)
        throws ParserConfigurationException {
        DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
        Document newDocument = builder.newDocument();
        if (!responseOutput.isEmpty()) {
            // convert normalized nodes to xml nodes
            List<Node> outputNodes = convertOutputToXmlNodes(responseOutput, builder, newDocument);
            List<Node> outputNodesData = new ArrayList<>();
            NodeList nodeList = outputNodes.get(0).getChildNodes();
            if (nodeList.getLength() < 1) {
                outputNodesData.add(RPCUtil.createOkNode(newDocument));
            } else {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    Element data = newDocument.createElementNS(
                            getIdentifier().getNamespace().toString(), node.getNodeName());
                    final int length = node.getChildNodes().getLength();
                    for (int j = 0; j < length; j++) {
                        data.appendChild(node.getFirstChild());
                    }
                    outputNodesData.add(data);
                }
            }
            // wrap nodes to final document
            List<Node> wrappedOutputNodes = new ArrayList<>();
            outputNodesData.forEach(outputNode -> wrappedOutputNodes.add(newDocument.importNode(outputNode, true)));
            newDocument.appendChild(wrapReplyResponse(newDocument, wrappedOutputNodes));
        } else {
            // convert normalized nodes to xml nodes
            List<Node> outputNodes = Collections.singletonList(RPCUtil.createOkNode(newDocument));
            List<Node> wrappedOutputNodes = new ArrayList<>();
            outputNodes.forEach(outputNode -> wrappedOutputNodes.add(newDocument.importNode(outputNode, true)));
            newDocument.appendChild(wrapReplyResponse(newDocument, wrappedOutputNodes));
        }
        String formattedResponse = RPCUtil.formatXml(newDocument.getDocumentElement());
        LOG.debug("Response: {}.", formattedResponse);
        return newDocument;
    }

    private Node wrapReplyResponse(Document document, List<Node> response) {
        Element rpcReply = document.createElementNS(RPCUtil.NETCONF_BASE_NAMESPACE, "rpc-reply");
        response.forEach(rpcReply::appendChild);
        return rpcReply;
    }

}
