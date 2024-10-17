/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.action.actions;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.Device;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.Start;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.StartInput;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.StartOutput;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.StartOutputBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class StartAction implements Start {

    @Override
    public ListenableFuture<RpcResult<StartOutput>> invoke(DataObjectIdentifier<Device> path, StartInput input) {
        final String startAt = input.getStartAt();
        return FluentFutures.immediateFluentFuture(RpcResultBuilder.success(new StartOutputBuilder().setStartFinishedAt(
                startAt).build()).build());
    }
}

