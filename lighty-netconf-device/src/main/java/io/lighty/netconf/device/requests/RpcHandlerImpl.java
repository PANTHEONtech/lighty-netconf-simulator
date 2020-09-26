/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import io.lighty.netconf.device.NetconfDeviceServices;
import io.lighty.netconf.device.utils.RPCUtil;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.test.tool.rpchandler.RpcHandler;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RpcHandlerImpl implements RpcHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RpcHandlerImpl.class);

    private final Map<QName, RequestProcessor> cache;

    public RpcHandlerImpl(final NetconfDeviceServices netconfDeviceServices, final Map<QName, RequestProcessor> cache) {
        this.cache = cache;
        this.cache.values().forEach(rp -> rp.init(netconfDeviceServices));
    }

    @Override
    public Optional<Document> getResponse(final XmlElement rpcElement) {
        final Element element = rpcElement.getDomElement();
        final String formattedRequest = RPCUtil.formatXml(element);
        LOG.debug("Received get request with payload:\n{} ", formattedRequest);
        final Optional<RequestProcessor> processorForRequestOpt = getProcessorForRequest(element);
        if (processorForRequestOpt.isPresent()) {
            return Optional.ofNullable(processorForRequestOpt.get().processRequest(element));
        }
        return Optional.empty();
    }

    private Optional<RequestProcessor> getProcessorForRequest(final Element element) {
        final String namespace = element.getNamespaceURI();
        final String localName = element.getLocalName();
        final RequestProcessor foundProcessor = this.cache.get(QName.create(namespace, localName));
        return Optional.ofNullable(foundProcessor);
    }

}
