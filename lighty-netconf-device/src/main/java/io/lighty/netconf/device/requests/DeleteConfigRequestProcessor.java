/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.utils.RPCUtil;
import java.util.concurrent.CompletableFuture;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.w3c.dom.Element;

public class DeleteConfigRequestProcessor extends OkOutputRequestProcessor {

    private static final String DELETE_CONFIG_RPC_NAME = "delete-config";

    public DeleteConfigRequestProcessor() {
    }

    @Override
    public QName getIdentifier() {
        return QName.create(RPCUtil.NETCONF_BASE_NAMESPACE, DELETE_CONFIG_RPC_NAME);
    }

    @Override
    protected CompletableFuture<Response> executeOkRequest(Element requestXmlElement) {
        //Currently only running datastores are implemented,
        //in the future when other datastores are added, handling here needs to be done
        return CompletableFuture.failedFuture(new NetconfDocumentedException("operation-not-supported",
            ErrorType.RPC,
            ErrorTag.OPERATION_NOT_SUPPORTED,
            ErrorSeverity.ERROR));
    }

}
