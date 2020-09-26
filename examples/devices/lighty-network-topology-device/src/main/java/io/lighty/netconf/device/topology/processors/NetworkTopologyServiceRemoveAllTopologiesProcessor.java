/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology.processors;

import java.util.concurrent.ExecutionException;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev180320.NetworkTopologyRpcsService;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev180320.RemoveAllTopologiesInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev180320.RemoveAllTopologiesOutput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class NetworkTopologyServiceRemoveAllTopologiesProcessor extends NetworkTopologyServiceAbstractProcessor<
        RemoveAllTopologiesInput, RemoveAllTopologiesOutput> {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyServiceRemoveAllTopologiesProcessor.class);

    private final NetworkTopologyRpcsService networkTopologyRpcsService;
    private final QName qName = QName.create("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
        "remove-all-topologies");

    public NetworkTopologyServiceRemoveAllTopologiesProcessor(
        final NetworkTopologyRpcsService networkTopologyRpcsService) {
        this.networkTopologyRpcsService = networkTopologyRpcsService;
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }

    @Override
    protected RpcResult<RemoveAllTopologiesOutput> execMethod(final RemoveAllTopologiesInput input)
            throws ExecutionException, InterruptedException {
        final RpcResult<RemoveAllTopologiesOutput> voidRpcResult = this.networkTopologyRpcsService.removeAllTopologies(
                input).get();
        return voidRpcResult;
    }
}
