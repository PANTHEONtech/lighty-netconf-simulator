/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.action.actions;

import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.Device;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.Start;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.start.Input;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.start.Output;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.device.start.OutputBuilder;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class StartAction implements Start {

    @Override
    public FluentFuture<RpcResult<Output>> invoke(final InstanceIdentifier<Device> path, final Input input) {
        final String startAt = input.getStartAt();
        return FluentFutures.immediateFluentFuture(RpcResultBuilder.success(new OutputBuilder().setStartFinishedAt(
                startAt)).build());
    }
}

