/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.netconf.device.requests.notification.NotificationPublishService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToastOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToastOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestocked;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestockedBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToasterServiceImpl implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterServiceImpl.class);

    private final ExecutorService executor;
    private NotificationPublishService notificationPublishService;

    public ToasterServiceImpl() {
        this.executor = Executors.newFixedThreadPool(1);
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<MakeToastOutput>> makeToast(final MakeToastInput makeToastInput) {
        LOG.info("makeToast {} {}", makeToastInput.getToasterDoneness(), makeToastInput.getToasterToastType());
        final SettableFuture<RpcResult<MakeToastOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<MakeToastOutput>>() {
            @Override
            public RpcResult<MakeToastOutput> call() throws Exception {
                final MakeToastOutput output = new MakeToastOutputBuilder().build();
                final RpcResult<MakeToastOutput> rpcResult = RpcResultBuilder.success(output).build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<CancelToastOutput>> cancelToast(final CancelToastInput input) {
        LOG.info("cancelToast");
        final SettableFuture<RpcResult<CancelToastOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<CancelToastOutput>>() {
            @Override
            public RpcResult<CancelToastOutput> call() throws Exception {
                final CancelToastOutput output = new CancelToastOutputBuilder().build();
                final RpcResult<CancelToastOutput> rpcResult = RpcResultBuilder.success(output).build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<RestockToasterOutput>> restockToaster(final RestockToasterInput input) {
        LOG.info("restockToaster {}", input.getAmountOfBreadToStock());

        publishRestockNotification(input);

        final SettableFuture<RpcResult<RestockToasterOutput>> result = SettableFuture.create();

        this.executor.submit(new Callable<RpcResult<RestockToasterOutput>>() {
            @Override
            public RpcResult<RestockToasterOutput> call() throws Exception {
                final RestockToasterOutput output = new RestockToasterOutputBuilder().build();
                final RpcResult<RestockToasterOutput> rpcResult = RpcResultBuilder.success(output).build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    private void publishRestockNotification(RestockToasterInput input) {
        if (this.notificationPublishService != null) {
            ToasterRestocked reStockedNotification = new ToasterRestockedBuilder()
                    .setAmountOfBread(input.getAmountOfBreadToStock()).build();
            notificationPublishService.publish(reStockedNotification, ToasterRestocked.QNAME);
        }
    }

    public void setNotificationPublishService(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public void close() {
        this.executor.shutdown();
    }

}
