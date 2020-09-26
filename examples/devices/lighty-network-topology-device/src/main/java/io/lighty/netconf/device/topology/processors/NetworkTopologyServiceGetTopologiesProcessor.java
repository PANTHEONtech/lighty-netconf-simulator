/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology.processors;

import java.util.concurrent.ExecutionException;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev180320.GetTopologiesInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev180320.GetTopologiesOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev180320.NetworkTopologyRpcsService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class NetworkTopologyServiceGetTopologiesProcessor extends NetworkTopologyServiceAbstractProcessor<
        GetTopologiesInput, GetTopologiesOutput> {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyServiceGetTopologiesProcessor.class);

    private final NetworkTopologyRpcsService networkTopologyRpcsService;
    private final QName qName = QName.create("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
        "get-topologies");

    public NetworkTopologyServiceGetTopologiesProcessor(final NetworkTopologyRpcsService networkTopologyRpcsService) {
        this.networkTopologyRpcsService = networkTopologyRpcsService;
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }

    @Override
    protected RpcResult<GetTopologiesOutput> execMethod(final GetTopologiesInput input) throws ExecutionException,
            InterruptedException {
        final RpcResult<GetTopologiesOutput> voidRpcResult = this.networkTopologyRpcsService.getTopologies(input).get();
        return voidRpcResult;
    }
}
