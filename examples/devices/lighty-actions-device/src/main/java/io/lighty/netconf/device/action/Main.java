/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.action;

import com.google.common.collect.ImmutableSet;
import io.lighty.netconf.device.NetconfDevice;
import io.lighty.netconf.device.NetconfDeviceBuilder;
import io.lighty.netconf.device.action.processors.ActionServiceDeviceProcessor;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public static final Set<YangModuleInfo> ACTION_MODEL_PATHS = ImmutableSet.of(
            org.opendaylight.yang.gen.v1.urn.example.data.center.rev180807.$YangModuleInfoImpl.getInstance());

    private ShutdownHook shutdownHook;

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true);
    }

    public void start(String[] args) {
        start(args, false);
    }

    public void start(String[] args, boolean registerShutdownHook) {
        final int port = getPortFromArgs(args);

        final NetconfDevice netconfDevice = new NetconfDeviceBuilder()
                .setCredentials("admin", "admin")
                .setBindingPort(port)
                .withModels(ACTION_MODEL_PATHS)
                .withDefaultRequestProcessors()
                .withDefaultCapabilities()
                .withRequestProcessor(new ActionServiceDeviceProcessor())
                .build();
        netconfDevice.start();

        //5. Register shutdown hook
        this.shutdownHook = new ShutdownHook(netconfDevice);
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    public void shutdown() {
        if (this.shutdownHook != null) {
            this.shutdownHook.execute();
        }
    }

    private static class ShutdownHook extends Thread {
        private final NetconfDevice netConfDevice;

        ShutdownHook(final NetconfDevice netConfDevice) {
            this.netConfDevice = netConfDevice;
        }

        @Override
        public void run() {
            this.execute();
        }

        @SuppressWarnings("checkstyle:IllegalCatch")
        public void execute() {
            LOG.info("Shutting down Lighty-Action device.");
            if (this.netConfDevice != null) {
                try {
                    this.netConfDevice.close();
                } catch (final Exception e) {
                    LOG.error("Failed to close Netconf device properly", e);
                }
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static int getPortFromArgs(final String[] args) {
        try {
            return Integer.parseInt(args[0]);
        } catch (final Exception e) {
            return 17830;
        }
    }

}
