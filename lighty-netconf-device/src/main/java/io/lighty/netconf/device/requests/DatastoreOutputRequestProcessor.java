/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import com.google.common.util.concurrent.FluentFuture;
import io.lighty.codecs.util.SerializationException;
import io.lighty.netconf.device.utils.RPCUtil;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class DatastoreOutputRequestProcessor extends BaseRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreOutputRequestProcessor.class);

    @Override
    protected String convertNormalizedNodeToXmlString(NormalizedNode normalizedNode)
            throws SerializationException {
        return getNetconfDeviceServices().getXmlNodeConverter()
                .serializeData(getNetconfDeviceServices().getRootSchemaNode(), normalizedNode).toString();
    }

    protected List<NormalizedNode> getAllDataFromDatastore(LogicalDatastoreType datastoreType) {
        DOMDataBroker domDataBroker = getNetconfDeviceServices().getDOMDataBroker();
        Optional<NormalizedNode> listData;
        try (DOMDataTreeReadTransaction domDataReadOnlyTransaction = domDataBroker.newReadOnlyTransaction()) {
            FluentFuture<Optional<NormalizedNode>> readData =
                    domDataReadOnlyTransaction.read(datastoreType, YangInstanceIdentifier.empty());
            listData = readData.get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            if (listData.isPresent()) {
                ContainerNode containerNode = (ContainerNode) listData.get();
                return new ArrayList<>(containerNode.getValue());
            }
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("Exception thrown while getting data from datastore!", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while getting data from datastore!", e);
            Thread.currentThread().interrupt();
        }
        return Collections.emptyList();
    }

    @Override
    protected Document wrapToFinalDocumentReply(List<NormalizedNode> responseOutput)
            throws ParserConfigurationException {
        // convert normalized nodes to xml nodes
        DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
        Document newDocument = builder.newDocument();
        List<Node> wrappedOutputNodes = new ArrayList<>();
        if (!responseOutput.isEmpty()) {
            List<Node> outputNodes = convertOutputToXmlNodes(responseOutput, builder, newDocument);
            outputNodes.forEach(outputNode -> wrappedOutputNodes.add(newDocument.importNode(outputNode, true)));
        }

        // wrap nodes to final document
        newDocument.appendChild(wrapResponse(newDocument, wrappedOutputNodes));
        String formattedResponse = RPCUtil.formatXml(newDocument.getDocumentElement());
        LOG.debug("Response: {}.", formattedResponse);
        return newDocument;
    }

    private Node wrapResponse(Document document, List<Node> response) {
        Element rpcReply = document.createElementNS(RPCUtil.NETCONF_BASE_NAMESPACE, "rpc-reply");
        Element data = document.createElementNS(RPCUtil.NETCONF_BASE_NAMESPACE, "data");
        response.forEach(data::appendChild);
        rpcReply.appendChild(document.importNode(data, true));
        return rpcReply;
    }

}
