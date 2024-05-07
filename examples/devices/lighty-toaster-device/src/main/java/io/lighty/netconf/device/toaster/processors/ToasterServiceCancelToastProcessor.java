/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster.processors;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.netconf.device.toaster.rpcs.ToasterServiceImpl;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToast;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToastOutput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class ToasterServiceCancelToastProcessor extends ToasterServiceAbstractProcessor<CancelToastInput,
        CancelToastOutput> implements CancelToast {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterServiceCancelToastProcessor.class);

    private final ToasterServiceImpl toasterService;
    private final QName qName = QName.create("http://netconfcentral.org/ns/toaster", "cancel-toast");

    public ToasterServiceCancelToastProcessor(final ToasterServiceImpl toasterService) {
        this.toasterService = toasterService;
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }

    @Override
    public ListenableFuture<RpcResult<CancelToastOutput>> invoke(final CancelToastInput input) {
        LOG.info("execute RPC: cancel-toast");
        return toasterService.cancelToast(input);
    }

}
