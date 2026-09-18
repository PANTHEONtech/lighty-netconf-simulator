/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.action.processors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.codecs.util.exception.SerializationException;
import io.lighty.netconf.device.NetconfDeviceServices;
import io.lighty.netconf.device.action.actions.ResetAction;
import io.lighty.netconf.device.action.actions.StartAction;
import io.lighty.netconf.device.requests.BaseRequestProcessor;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.utils.RPCUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.Start;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.Reset;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.stmt.ActionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class ActionServiceDeviceProcessor extends BaseRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ActionServiceDeviceProcessor.class);
    private AdapterContext adapterContext;
    private ImmutableMap<Absolute, ActionEffectiveStatement> actions;
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
        final Optional<Entry<Absolute, ActionEffectiveStatement>> actionEntry = findActionInElement(fromDomElement);

        Preconditions.checkState(actionEntry.isPresent(), "Action is not present on the device.");

        if (actionEntry.get().getValue().argument().equals(Start.QNAME)) {
            this.actionProcessor = new StartActionProcessor(new StartAction(), actionEntry.get().getKey(),
                    actionEntry.get().getValue(), this.adapterContext.currentSerializer());
        }
        if (actionEntry.get().getValue().argument().equals(Reset.QNAME)) {
            this.actionProcessor = new ResetActionProcessor(new ResetAction(), actionEntry.get().getKey(),
                    actionEntry.get().getValue(), this.adapterContext.currentSerializer());
        }

        Preconditions.checkState(this.actionProcessor != null, "Action is not implemented on the device.");

        this.actionProcessor.init(getNetconfDeviceServices());
        return this.actionProcessor.execute(requestXmlElement);
    }

    @Override
    protected Document wrapToFinalDocumentReply(final List<NormalizedNode> responseOutput)
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
                final Element data = newDocument.createElementNS(this.actionProcessor.getActionStatement().argument()
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
    protected String convertNormalizedNodeToXmlString(final NormalizedNode normalizedNode)
            throws SerializationException {
        final Absolute actionOutput = getActionOutput(actionProcessor.getActionPath(),
                actionProcessor.getActionStatement());
        return getNetconfDeviceServices().getXmlNodeConverter().serializeRpc(actionOutput, normalizedNode).toString();
    }

    protected static Absolute getActionInput(final Absolute path, final ActionEffectiveStatement action) {
        final var inputPath = new ArrayList<>(path.getNodeIdentifiers());
        inputPath.add(action.inputStatement().argument());
        return Absolute.of(inputPath);
    }

    protected static Absolute getActionOutput(final Absolute path, final ActionEffectiveStatement action) {
        final var outputPath = new ArrayList<>(path.getNodeIdentifiers());
        outputPath.add(action.outputStatement().argument());
        return Absolute.of(outputPath);
    }

    protected ActionEffectiveStatement getActionStatement() {
        return null;
    }

    protected Absolute getActionPath() {
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

    private Optional<Entry<Absolute, ActionEffectiveStatement>> findActionInElement(final XmlElement fromDomElement) {
        for (final Entry<Absolute, ActionEffectiveStatement> actionEntry : actions.entrySet()) {
            final var actionStatement = actionEntry.getValue();
            final var actionQname = actionStatement.argument();
            try {
                if (actionQname.getLocalName().equals(fromDomElement.getName())
                        && actionQname.getNamespace().toString().equals(fromDomElement.getNamespace())) {
                    return Optional.of(actionEntry);
                } else {
                    for (final XmlElement element : fromDomElement.getChildElements()) {
                        final var foundActionEntry = findActionInElement(element);
                        if (foundActionEntry.isPresent()) {
                            return foundActionEntry;
                        }
                    }
                }
            } catch (final DocumentedException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    private ImmutableMap<Absolute, ActionEffectiveStatement> getAction() {
        final var builder = ImmutableMap.<Absolute, ActionEffectiveStatement>builder();
        final var context = adapterContext.currentSerializer().getRuntimeContext().modelContext();
        final var path = new ArrayDeque<QName>();
        for (final Module module : context.getModules()) {
            findAction(module.asEffectiveStatement(), builder, path);
        }
        return builder.build();
    }

    private void findAction(final SchemaTreeAwareEffectiveStatement<?, ?> parent,
            final Builder<Absolute, ActionEffectiveStatement> builder, final Deque<QName> path) {
        for (final SchemaTreeEffectiveStatement<?> child : parent.schemaTreeNodes()) {
            path.addLast(child.argument());
            if (child instanceof ActionEffectiveStatement action) {
                builder.put(Absolute.of(List.copyOf(path)), action);
            } else if (child instanceof SchemaTreeAwareEffectiveStatement<?, ?> aware) {
                findAction(aware, builder, path);
            }
            path.removeLast();
        }
    }

    protected Node wrapReplyResponse(final Document document, final List<Node> response) {
        final Element rpcReply = document.createElementNS(RPCUtil.NETCONF_BASE_NAMESPACE, "rpc-reply");
        response.forEach(rpcReply::appendChild);
        return rpcReply;
    }
}

