/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster.processors;

import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class ToasterServiceRestockToasterProcessor extends ToasterServiceAbstractProcessor<RestockToasterInput,
        RestockToasterOutput> {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterServiceRestockToasterProcessor.class);

    private final ToasterService toasterService;
    private final QName qName = QName.create("http://netconfcentral.org/ns/toaster", "restock-toaster");

    public ToasterServiceRestockToasterProcessor(final ToasterService toasterService) {
        this.toasterService = toasterService;
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }

    @Override
    protected Future<RpcResult<RestockToasterOutput>> execMethod(final RestockToasterInput input) {
        LOG.info("execute RPC: restock-toaster");
        return this.toasterService.restockToaster(input);
    }

}
