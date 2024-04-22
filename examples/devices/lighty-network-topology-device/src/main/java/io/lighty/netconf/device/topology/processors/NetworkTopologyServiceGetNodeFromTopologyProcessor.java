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
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetNodeFromTopologyById;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetNodeFromTopologyByIdInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetNodeFromTopologyByIdOutput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MemberName")
public class NetworkTopologyServiceGetNodeFromTopologyProcessor extends
        NetworkTopologyServiceAbstractProcessor<GetNodeFromTopologyByIdInput, GetNodeFromTopologyByIdOutput>
        implements GetNodeFromTopologyById {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyServiceGetNodeFromTopologyProcessor.class);

    private final NetworkTopologyServiceImpl networkTopologyRpcsService;
    private final QName qName = QName.create("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
        "get-node-from-topology-by-id");

    public NetworkTopologyServiceGetNodeFromTopologyProcessor(final NetworkTopologyServiceImpl networkTopologyRpcsService) {
        this.networkTopologyRpcsService = networkTopologyRpcsService;
    }

    @Override
    public QName getIdentifier() {
        return qName;
    }


    @Override
    public ListenableFuture<RpcResult<GetNodeFromTopologyByIdOutput>> invoke(final GetNodeFromTopologyByIdInput input) {
        return networkTopologyRpcsService.getNodeFromTopologyById(input);
    }
}
