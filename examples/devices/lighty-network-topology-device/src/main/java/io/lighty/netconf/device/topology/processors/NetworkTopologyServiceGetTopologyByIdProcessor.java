/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology.processors;

import io.lighty.netconf.device.utils.TimeoutUtil;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologyByIdInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologyByIdOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.NetworkTopologyRpcsService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class NetworkTopologyServiceGetTopologyByIdProcessor extends
    NetworkTopologyServiceAbstractProcessor<GetTopologyByIdInput, GetTopologyByIdOutput> {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyServiceGetTopologyByIdProcessor.class);

    private final NetworkTopologyRpcsService networkTopologyRpcsService;
    private final QName qName = QName.create("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
        "get-topology-by-id");

    public NetworkTopologyServiceGetTopologyByIdProcessor(NetworkTopologyRpcsService networkTopologyRpcsService) {
        this.networkTopologyRpcsService = networkTopologyRpcsService;
    }

    @Override
    public QName getIdentifier() {
        return qName;
    }

    @Override
    protected RpcResult<GetTopologyByIdOutput> execMethod(final GetTopologyByIdInput input)
            throws ExecutionException, InterruptedException, TimeoutException {
        final RpcResult<GetTopologyByIdOutput> voidRpcResult = networkTopologyRpcsService
                .getTopologyById(input).get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        return voidRpcResult;
    }
}
