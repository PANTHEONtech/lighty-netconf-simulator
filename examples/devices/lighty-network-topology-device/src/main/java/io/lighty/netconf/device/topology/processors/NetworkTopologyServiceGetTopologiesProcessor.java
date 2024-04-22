/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology.processors;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.netconf.device.topology.rpcs.NetworkTopologyServiceImpl;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologies;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologiesInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologiesOutput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class NetworkTopologyServiceGetTopologiesProcessor extends NetworkTopologyServiceAbstractProcessor<
        GetTopologiesInput, GetTopologiesOutput> implements GetTopologies {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyServiceGetTopologiesProcessor.class);

    private final NetworkTopologyServiceImpl networkTopologyRpcsService;
    private final QName qName = QName.create("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
        "get-topologies");

    public NetworkTopologyServiceGetTopologiesProcessor(final NetworkTopologyServiceImpl networkTopologyRpcsService) {
        this.networkTopologyRpcsService = networkTopologyRpcsService;
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologiesOutput>> invoke(final GetTopologiesInput input) {
        return networkTopologyRpcsService.getTopologies(input);
    }
}
