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
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Implementation of commit netconf protocol operation.
 */
public class CommitRequestProcessor extends OkOutputRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CommitRequestProcessor.class);

    @Override
    protected CompletableFuture<Response> executeOkRequest(Element requestXmlElement) {
        LOG.info("commit: executeOkRequest");
        final CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        responseFuture.complete(new ResponseData(Collections.emptyList()));
        return responseFuture;
    }

    @Override
    public QName getIdentifier() {
        return QName.create(RPCUtil.NETCONF_BASE_NAMESPACE, "commit");
    }

}
