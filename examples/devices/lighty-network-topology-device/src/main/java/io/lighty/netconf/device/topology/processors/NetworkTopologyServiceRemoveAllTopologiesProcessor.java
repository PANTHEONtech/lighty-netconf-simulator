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
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveAllTopologies;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveAllTopologiesInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveAllTopologiesOutput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class NetworkTopologyServiceRemoveAllTopologiesProcessor extends NetworkTopologyServiceAbstractProcessor<
        RemoveAllTopologiesInput, RemoveAllTopologiesOutput> implements RemoveAllTopologies {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyServiceRemoveAllTopologiesProcessor.class);

    private final NetworkTopologyServiceImpl networkTopologyRpcsService;
    private final QName qName = QName.create("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
        "remove-all-topologies");

    public NetworkTopologyServiceRemoveAllTopologiesProcessor(
        final NetworkTopologyServiceImpl networkTopologyRpcsService) {
        this.networkTopologyRpcsService = networkTopologyRpcsService;
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }

    @Override
    public ListenableFuture<RpcResult<RemoveAllTopologiesOutput>> invoke(final RemoveAllTopologiesInput input) {
        return networkTopologyRpcsService.removeAllTopologies(input);
    }
}
