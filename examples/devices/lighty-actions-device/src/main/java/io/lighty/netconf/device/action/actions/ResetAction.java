/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.action.actions;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.Server;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.ServerKey;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.Reset;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.ResetInput;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.ResetOutput;
import org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.server.ResetOutputBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class ResetAction implements Reset {

    @Override
    public ListenableFuture<RpcResult<ResetOutput>> invoke(final DataObjectIdentifier.WithKey<Server, ServerKey> path,
            final ResetInput input) {
        final String resetAt = input.getResetAt();
        return FluentFutures.immediateFluentFuture(RpcResultBuilder.success(new ResetOutputBuilder().setResetFinishedAt(
            resetAt).build()).build());
    }
}

