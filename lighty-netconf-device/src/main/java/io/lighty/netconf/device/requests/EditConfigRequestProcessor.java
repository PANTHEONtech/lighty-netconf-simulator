/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.lighty.codecs.util.SerializationException;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.response.ResponseData;
import io.lighty.netconf.device.response.ResponseErrorMessage;
import io.lighty.netconf.device.utils.DefaultOperation;
import io.lighty.netconf.device.utils.Operation;
import io.lighty.netconf.device.utils.RPCUtil;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.io.StringReader;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.copy.config.input.source.config.source.Config;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.SchemaValidationFailedException;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of edit-config netconf protocol operation.
 * https://tools.ietf.org/html/rfc6241#section-7
 */
public class EditConfigRequestProcessor extends OkOutputRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(EditConfigRequestProcessor.class);
    private static final String EDIT_CONFIG_RPC_NAME = "edit-config";

    @Override
    public QName getIdentifier() {
        return QName.create(RPCUtil.NETCONF_BASE_NAMESPACE, EDIT_CONFIG_RPC_NAME);
    }

    @SuppressWarnings("checkstyle:FallThrough")
    @Override
    protected CompletableFuture<Response> executeOkRequest(final Element requestXmlElement) {
        final Optional<DefaultOperation> defaultOperation = RPCUtil.retrieveDefaultOperation(requestXmlElement);
        final Optional<Operation> operation = RPCUtil.retrieveOperation(requestXmlElement);

        Operation operationToExecute;
        if (operation.isPresent()) {
            operationToExecute = operation.get();
        } else {
            if (defaultOperation.isPresent()) {
                if (defaultOperation.get() == DefaultOperation.NONE) {
                    throw new IllegalStateException("Operation was " + DefaultOperation.NONE + " or not defined!");
                } else {
                    operationToExecute = Operation.getOperationByName(defaultOperation.get().getOperationName());
                }
            } else {
                operationToExecute = Operation.MERGE;
            }
        }

        final Element configElement = (Element) requestXmlElement
                .getElementsByTagNameNS(Config.QNAME.getNamespace().toString(), Config.QNAME.getLocalName()).item(0);

        final String configString = RPCUtil.formatXml(configElement);

        NormalizedNode configNN;
        try {
            configNN = getNetconfDeviceServices().getXmlNodeConverter()
                    .deserialize(getNetconfDeviceServices().getRootSchemaNode(), new StringReader(configString));
        } catch (final SerializationException e) {
            return CompletableFuture.completedFuture(new ResponseErrorMessage(
                new NetconfDocumentedException(
                    "operation-failed",
                    ErrorType.APPLICATION,
                    ErrorTag.OPERATION_FAILED,
                    ErrorSeverity.ERROR)));
        }

        EffectiveModelContext effectiveModelContext = getNetconfDeviceServices().getAdapterContext()
                .currentSerializer().getRuntimeContext().getEffectiveModelContext();
        YangInstanceIdentifier path = retrieveElementYII(effectiveModelContext, configNN,
                configElement, "//*[@*[local-name() = 'operation']]");
        NormalizedNode data;
        if (path == null) {
            Optional<DataContainerChild> optionalData =
                ((AbstractCollection<DataContainerChild>) configNN.body()).stream().findFirst();
            if (optionalData.isEmpty()) {
                return CompletableFuture.completedFuture(new ResponseErrorMessage(
                    new NetconfDocumentedException(
                        "data-missing",
                        ErrorType.APPLICATION,
                        ErrorTag.DATA_MISSING,
                        ErrorSeverity.ERROR)));
            }
            data = optionalData.get();
            path = YangInstanceIdentifier.builder().node(data.getIdentifier().getNodeType()).build();
        } else {
            Optional<NormalizedNode> optionalData =
                NormalizedNodes.findNode(configNN, path);
            if (optionalData.isEmpty()) {
                return CompletableFuture.completedFuture(new ResponseErrorMessage(
                    new NetconfDocumentedException(
                        "data-missing",
                        ErrorType.APPLICATION,
                        ErrorTag.DATA_MISSING,
                        ErrorSeverity.ERROR)));
            }
            data = optionalData.get();
        }

        final DOMDataTreeWriteTransaction writeTx =
                getNetconfDeviceServices().getDOMDataBroker().newWriteOnlyTransaction();
        final CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        switch (operationToExecute) {
            case CREATE:
                if (dataExists(path, operationToExecute, data)) {
                    final NetconfDocumentedException netconfDocumentedException =
                            new NetconfDocumentedException("data-exists", ErrorType.RPC, ErrorTag.DATA_EXISTS,
                                    ErrorSeverity.ERROR);
                    responseFuture.complete(new ResponseErrorMessage(netconfDocumentedException));
                    return responseFuture;
                }
            case REPLACE:
                ensureParentsByMerge(path, writeTx);
                writeTx.put(LogicalDatastoreType.CONFIGURATION, path, data);
                break;
            case DELETE:
                if (!dataExists(path, operationToExecute, data)) {
                    final NetconfDocumentedException netconfDocumentedException =
                            new NetconfDocumentedException("data-missing", ErrorType.RPC, ErrorTag.DATA_MISSING,
                                    ErrorSeverity.ERROR);
                    responseFuture.complete(new ResponseErrorMessage(netconfDocumentedException));
                    return responseFuture;
                }
            case REMOVE:
                writeTx.delete(LogicalDatastoreType.CONFIGURATION, path);
                break;
            case MERGE:
                writeTx.merge(LogicalDatastoreType.CONFIGURATION, path, data);
                break;
            default:
                break;
        }
        try {
            writeTx.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            responseFuture.complete(new ResponseData(Collections.emptyList()));
            return responseFuture;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (e.getCause() instanceof TransactionCommitFailedException) {
                final Throwable error = e.getCause();
                if (error.getCause() instanceof SchemaValidationFailedException) {
                    final NetconfDocumentedException netconfDocumentedException =
                            new NetconfDocumentedException(error.getCause().getMessage(), ErrorType.APPLICATION,
                                    ErrorTag.BAD_ELEMENT, ErrorSeverity.ERROR);
                    responseFuture.complete(new ResponseErrorMessage(netconfDocumentedException));
                    return responseFuture;
                }
            }
            throw createTxException(data, e, operationToExecute.getOperationName().toUpperCase(Locale.ROOT));
        }
    }

    private void ensureParentsByMerge(final YangInstanceIdentifier path, final DOMDataTreeWriteTransaction writeTx) {
        final EffectiveModelContext effectiveModelContext = getNetconfDeviceServices().getAdapterContext()
                .currentSerializer().getRuntimeContext().getEffectiveModelContext();
        final List<PathArgument> normalizedPathWithoutChildArgs = new ArrayList<>();
        YangInstanceIdentifier rootNormalizedPath = null;

        final Iterator<PathArgument> it = path.getPathArguments().iterator();

        while (it.hasNext()) {
            final PathArgument pathArgument = it.next();
            if (rootNormalizedPath == null) {
                rootNormalizedPath = YangInstanceIdentifier.create(pathArgument);
            }

            if (it.hasNext()) {
                normalizedPathWithoutChildArgs.add(pathArgument);
            }
        }

        if (normalizedPathWithoutChildArgs.isEmpty()) {
            return;
        }

        Preconditions.checkArgument(rootNormalizedPath != null, "Empty path received");

        final NormalizedNode parentStructure = ImmutableNodes.fromInstanceId(effectiveModelContext,
                YangInstanceIdentifier.create(normalizedPathWithoutChildArgs));
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, rootNormalizedPath, parentStructure);
    }

    private IllegalStateException createTxException(
        final NormalizedNode data, final Exception exception, final String type) {
        return new IllegalStateException("Unable to execute " + type + " operation with data:\n"
            + NormalizedNodes.toStringTree(data), exception);
    }

    private static List<QName> buildQNamePath(final Node node) {
        final Set<String> stopNodes = Collections.singleton("config");
        return buildQNamePath(node, stopNodes);
    }

    private static List<QName> buildQNamePath(final Node node, final Set<String> stopNodes) {
        Node parentNode = node;
        final List<QName> qNames = Lists.newLinkedList();
        while (parentNode != null) {
            if (!stopNodes.contains(parentNode.getLocalName())) {
                qNames.add(QName.create(parentNode.getNamespaceURI(), parentNode.getLocalName()));
                parentNode = parentNode.getParentNode();
            } else {
                parentNode = null;
            }
        }
        return Lists.reverse(qNames);
    }

    protected static YangInstanceIdentifier retrieveElementYII(
            final EffectiveModelContext effectiveModelContext, final NormalizedNode normalizedNode,
            final Element deviceElement, final String xpathExpression) {
        final List<Node> nodes = RPCUtil.getNodes(deviceElement.getChildNodes());
        if (nodes.isEmpty()) {
            return null;
        }
        try {
            final Node foundNode =
                    ((NodeList) XPathFactory.newInstance().newXPath().compile(xpathExpression)
                            .evaluate(deviceElement, XPathConstants.NODESET)).item(0);
            if (foundNode == null) {
                return null;
            }
            final List<QName> buildQNamePath = buildQNamePath(foundNode);
            return getYangInstanceIdentifier(buildQNamePath, normalizedNode, effectiveModelContext);
        } catch (final XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Method will find the {@link YangInstanceIdentifier} from the given list of
     * {@link QName}.
     *
     * @param yangPath a path
     * @param input a list of normalized nodes
     * @param effectiveModelContext a schema context
     * @return YangInstanceIdentifier a yang instance identifier
     */
    private static YangInstanceIdentifier getYangInstanceIdentifier(final List<QName> yangPath,
            final NormalizedNode input, final EffectiveModelContext effectiveModelContext) {
        DataSchemaContextNode<?> contextNode = DataSchemaContextTree.from(effectiveModelContext).getRoot();
        YangInstanceIdentifier targetIdentifier = YangInstanceIdentifier.builder().build();
        final Iterator<QName> iterator = yangPath.iterator();
        while (iterator.hasNext()) {
            final QName currentQname = parseQname(effectiveModelContext, iterator.next());
            contextNode = requireNonNull(contextNode.getChild(currentQname));

            while (contextNode.isMixin()) {
                targetIdentifier = YangInstanceIdentifier.create(targetIdentifier.getPathArguments())
                        .node(contextNode.getIdentifier());
                contextNode = requireNonNull(contextNode.getChild(currentQname));
            }

            final Optional<NormalizedNode> findNode =
                    NormalizedNodes.findNode(input, targetIdentifier.getPathArguments());
            if (contextNode.isKeyedEntry() && findNode.isPresent()) {
                final MapEntryNode next = ((MapNode) findNode.get()).body().iterator().next();
                final Map<QName, Object> keyValues = next.getIdentifier().asMap();
                targetIdentifier = YangInstanceIdentifier
                        .builder(YangInstanceIdentifier.create(targetIdentifier.getPathArguments()))
                        .nodeWithKey(contextNode.getIdentifier().getNodeType(), keyValues).build();
            } else {
                targetIdentifier = YangInstanceIdentifier.create(targetIdentifier.getPathArguments())
                        .node(contextNode.getIdentifier());
            }
        }
        return targetIdentifier;
    }

    /**
     * Parses Qname.
     *
     * @param effectiveModelContext a schema context
     * @param pathArgument a path
     * @return QName a parsed path.
     */
    private static QName parseQname(final EffectiveModelContext effectiveModelContext, final QName pathArgument) {
        Optional<Module> module;
        if (pathArgument.getRevision().isPresent()) {
            module = effectiveModelContext.findModule(pathArgument.getNamespace(), pathArgument.getRevision());
        } else {
            final Collection<? extends Module> modules = effectiveModelContext.findModules(pathArgument.getNamespace());
            if (modules.size() == 1) {
                module = Optional.of(modules.iterator().next());
            } else {
                module = effectiveModelContext.findModule(pathArgument.getNamespace());
            }
        }
        if (!module.isPresent()) {
            throw new IllegalStateException(String.format(
                    "Couldn't find specified module: %s. Check if all necessary modules are loaded", pathArgument));
        }
        final Module next = module.get();
        return QName.create(next.getNamespace(), next.getRevision(), pathArgument.getLocalName());
    }

    private boolean dataExists(final YangInstanceIdentifier path, final Operation operationToExecute,
            final NormalizedNode data) {
        try (DOMDataTreeReadTransaction readTx =
                     getNetconfDeviceServices().getDOMDataBroker().newReadOnlyTransaction()) {
            return readTx.exists(LogicalDatastoreType.CONFIGURATION, path)
                    .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw createTxException(data, e, operationToExecute.getOperationName().toUpperCase(Locale.ROOT));
        }
    }
}
