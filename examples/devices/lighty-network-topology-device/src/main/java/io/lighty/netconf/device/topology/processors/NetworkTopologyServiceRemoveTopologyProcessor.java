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
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev180320.RemoveTopologyInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev180320.RemoveTopologyOutput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class NetworkTopologyServiceRemoveTopologyProcessor extends
    NetworkTopologyServiceAbstractProcessor<RemoveTopologyInput, RemoveTopologyOutput> {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyServiceRemoveTopologyProcessor.class);

    private final NetworkTopologyRpcsService networkTopologyRpcsService;
    private final QName qName = QName.create("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
        "remove-topology");

    public NetworkTopologyServiceRemoveTopologyProcessor(final NetworkTopologyRpcsService networkTopologyRpcsService) {
        this.networkTopologyRpcsService = networkTopologyRpcsService;
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }

    @Override
    protected RpcResult<RemoveTopologyOutput> execMethod(final RemoveTopologyInput input)
            throws ExecutionException, InterruptedException {
        final RpcResult<RemoveTopologyOutput> voidRpcResult = this.networkTopologyRpcsService.removeTopology(input)
                .get();
        return voidRpcResult;
    }
}
