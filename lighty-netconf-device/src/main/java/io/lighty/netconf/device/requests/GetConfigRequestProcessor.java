/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.response.ResponseData;
import io.lighty.netconf.device.utils.RPCUtil;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.w3c.dom.Element;

/**
 * Implementation of get-config netconf protocol operation.
 * https://tools.ietf.org/html/rfc6241#section-7
 */
public class GetConfigRequestProcessor extends DatastoreOutputRequestProcessor {

    private static final String GET_CONFIG_RPC_NAME = "get-config";

    @Override
    public QName getIdentifier() {
        return QName.create(RPCUtil.NETCONF_BASE_NAMESPACE, GET_CONFIG_RPC_NAME);
    }

    @Override
    public CompletableFuture<Response> execute(Element requestXml) {
        final CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        final List<NormalizedNode> allDataFromDatastore =
                getAllDataFromDatastore(LogicalDatastoreType.CONFIGURATION);
        responseFuture.complete(new ResponseData(allDataFromDatastore));
        return responseFuture;
    }
}
