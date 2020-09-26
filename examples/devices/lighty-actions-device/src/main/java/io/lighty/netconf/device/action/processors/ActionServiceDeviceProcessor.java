/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.action.processors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import io.lighty.codecs.api.SerializationException;
import io.lighty.netconf.device.NetconfDeviceServices;
import io.lighty.netconf.device.action.actions.ResetAction;
import io.lighty.netconf.device.action.actions.StartAction;
import io.lighty.netconf.device.requests.BaseRequestProcessor;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.utils.RPCUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.Start;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.ActionNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ActionServiceDeviceProcessor extends BaseRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ActionServiceDeviceProcessor.class);
    private AdapterContext adapterContext;
    private Collection<ActionDefinition> actions;
    private ActionServiceDeviceProcessor actionProcessor;

    @Override
    public void init(final NetconfDeviceServices netconfDeviceServices) {
        super.init(netconfDeviceServices);
        this.adapterContext = netconfDeviceServices.getAdapterContext();
        this.actions = getAction();
    }

    @Override
    public QName getIdentifier() {
        return QName.create("urn:ietf:params:xml:ns:yang:1", "action");
    }

    @Override
    protected CompletableFuture<Response> execute(final Element requestXmlElement) {
        final XmlElement fromDomElement = XmlElement.fromDomElement(requestXmlElement);
        final ActionDefinition actionDefinition = findActionInElement(fromDomElement);

        if (actionDefinition.getQName().equals(Start.QNAME)) {
            this.actionProcessor = new StartActionProcessor(new StartAction(), this.adapterContext.currentSerializer());
        } else {
            this.actionProcessor = new ResetActionProcessor(new ResetAction(), this.adapterContext.currentSerializer());
        }
        this.actionProcessor.init(getNetconfDeviceServices());
        return this.actionProcessor.execute(requestXmlElement, actionDefinition);
    }

    protected CompletableFuture<Response> execute(final Element requestXmlElement,
        final ActionDefinition actionDefinition) {
        return null;
    }

    @Override
    protected Document wrapToFinalDocumentReply(final List<NormalizedNode<?, ?>> responseOutput)
            throws ParserConfigurationException {
        final DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
        final Document newDocument = builder.newDocument();
        if (!responseOutput.isEmpty()) {
            // convert normalized nodes to xml nodes
            final List<Node> outputNodes = convertOutputToXmlNodes(responseOutput, builder, newDocument);
            final List<Node> outputNodesData = new ArrayList<>();
            final NodeList nodeList = outputNodes.get(0).getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                final Node node = nodeList.item(i);
                final Element data = newDocument.createElementNS(this.actionProcessor.getActionDefinition().getQName()
                        .getNamespace()
                        .toString(), node.getNodeName());
                final int length = node.getChildNodes().getLength();
                for (int j = 0; j < length; j++) {
                    data.appendChild(node.getFirstChild());
                }
                outputNodesData.add(data);
            }
            // wrap nodes to final document
            final List<Node> wrappedOutputNodes = new ArrayList<>();
            outputNodesData.forEach(outputNode -> wrappedOutputNodes.add(newDocument.importNode(outputNode, true)));
            newDocument.appendChild(wrapReplyResponse(newDocument, wrappedOutputNodes));
        } else {
            // convert normalized nodes to xml nodes
            final List<Node> outputNodes = Collections.singletonList(RPCUtil.createOkNode(newDocument));
            final List<Node> wrappedOutputNodes = new ArrayList<>();
            outputNodes.forEach(outputNode -> wrappedOutputNodes.add(newDocument.importNode(outputNode, true)));
            newDocument.appendChild(wrapReplyResponse(newDocument, wrappedOutputNodes));
        }
        final String formattedResponse = RPCUtil.formatXml(newDocument.getDocumentElement());
        LOG.debug("Response: {}.", formattedResponse);
        return newDocument;
    }

    @Override
    protected String convertNormalizedNodeToXmlString(final NormalizedNode<?, ?> normalizedNode)
            throws SerializationException {
        return getNetconfDeviceServices().getXmlNodeConverter().serializeRpc(this.actionProcessor.getActionDefinition()
                .getOutput(),
                normalizedNode).toString();
    }

    protected ActionDefinition getActionDefinition() {
        return null;
    }

    protected Element findInputElement(final XmlElement fromDomElement, final QName actionQName)
            throws DocumentedException {
        Preconditions.checkNotNull(fromDomElement);
        if (fromDomElement.getNamespace().equals(actionQName.getNamespace().toString()) && fromDomElement.getName()
                .equals(actionQName.getLocalName())) {
            return fromDomElement.getDomElement();
        }
        for (final XmlElement xmlElement : fromDomElement.getChildElements()) {
            final Element findInputElement = findInputElement(xmlElement, actionQName);
            if (findInputElement != null) {
                return findInputElement;
            }
        }
        return null;
    }

    private ActionDefinition findActionInElement(final XmlElement fromDomElement) {
        for (final ActionDefinition actionDefinition : this.actions) {
            final QName actionQname = actionDefinition.getQName();
            try {
                if (actionQname.getLocalName().equals(fromDomElement.getName()) && actionQname.getNamespace().toString()
                        .equals(fromDomElement.getNamespace())) {
                    return actionDefinition;
                } else {
                    for (final XmlElement element : fromDomElement.getChildElements()) {
                        final ActionDefinition actionDefinitionEle = findActionInElement(element);
                        if (actionDefinitionEle != null) {
                            return actionDefinitionEle;
                        }
                    }
                }
            } catch (final DocumentedException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private Collection<ActionDefinition> getAction() {
        final Builder<ActionDefinition> builder = ImmutableSet.builder();
        Collection<? extends DataSchemaNode> childNodes =
                this.adapterContext.currentSerializer().getRuntimeContext().getEffectiveModelContext().getChildNodes();
        for (final DataSchemaNode dataSchemaNode : childNodes) {
            if (dataSchemaNode instanceof ActionNodeContainer) {
                findAction(dataSchemaNode, builder);
            }
        }
        return builder.build();
    }

    private void findAction(final DataSchemaNode dataSchemaNode, final Builder<ActionDefinition> builder) {
        if (dataSchemaNode instanceof ActionNodeContainer) {
            final ActionNodeContainer containerSchemaNode = (ActionNodeContainer) dataSchemaNode;
            for (final ActionDefinition actionDefinition : containerSchemaNode.getActions()) {
                builder.add(actionDefinition);
            }
        }
        if (dataSchemaNode instanceof DataNodeContainer) {
            for (final DataSchemaNode innerDataSchemaNode : ((DataNodeContainer) dataSchemaNode).getChildNodes()) {
                findAction(innerDataSchemaNode, builder);
            }
        }
    }

    protected Node wrapReplyResponse(final Document document, final List<Node> response) {
        final Element rpcReply = document.createElementNS(RPCUtil.NETCONF_BASE_NAMESPACE, "rpc-reply");
        response.forEach(rpcReply::appendChild);
        return rpcReply;
    }
}

