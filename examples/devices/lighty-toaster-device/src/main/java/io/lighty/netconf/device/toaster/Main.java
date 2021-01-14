/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.netconf.device.NetconfDevice;
import io.lighty.netconf.device.NetconfDeviceBuilder;
import io.lighty.netconf.device.toaster.processors.ToasterServiceCancelToastProcessor;
import io.lighty.netconf.device.toaster.processors.ToasterServiceMakeToastProcessor;
import io.lighty.netconf.device.toaster.processors.ToasterServiceRestockToasterProcessor;
import io.lighty.netconf.device.toaster.rpcs.ToasterServiceImpl;
import java.io.InputStream;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private ShutdownHook shutdownHook;
    private static final Set<YangModuleInfo> TOASTER_MODEL_PATHS = ImmutableSet.of(
        org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714
            .$YangModuleInfoImpl.getInstance()
    );

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true, true);
    }

    public void start(String[] args) {
        start(args, false, true);
    }

    @SuppressFBWarnings({"SLF4J_SIGN_ONLY_FORMAT", "OBL_UNSATISFIED_OBLIGATION"})
    public void start(String[] args, boolean registerShutdownHook, final boolean initDataStore) {
        int port = getPortFromArgs(args);
        LOG.info("Lighty-Toaster device started at port {}", port);
        LOG.info("___________             __        ________              .__");
        LOG.info("\\__    ___/___  _______/  |_______\\______ \\   _______  _|__| ____  ____");
        LOG.info("  |    | /  _ \\/  ___/\\   __\\_  __ \\    |  \\_/ __ \\  \\/ /  |/ ___\\/ __ \\");
        LOG.info("  |    |(  <_> )___ \\  |  |  |  | \\/    `   \\  ___/\\   /|  \\  \\__\\  ___/");
        LOG.info("  |____| \\____/____  > |__|  |__| /_______  /\\___  >\\_/ |__|\\___  >___  >");
        LOG.info("                   \\/                     \\/     \\/             \\/    \\/");

        LOG.info("[https://lighty.io]");

        //1. Initialize RPCs
        ToasterServiceImpl toasterService = new ToasterServiceImpl();
        ToasterServiceMakeToastProcessor makeToastProcessor =
            new ToasterServiceMakeToastProcessor(toasterService);
        ToasterServiceCancelToastProcessor cancelToastProcessor =
            new ToasterServiceCancelToastProcessor(toasterService);
        ToasterServiceRestockToasterProcessor restockToasterProcessor =
            new ToasterServiceRestockToasterProcessor(toasterService);

        //2. Initialize Netconf device
        final NetconfDeviceBuilder netconfDeviceBuilder = new NetconfDeviceBuilder()
                .setCredentials("admin", "admin")
                .setBindingPort(port)
                .withModels(TOASTER_MODEL_PATHS)
                .withDefaultRequestProcessors()
                .withDefaultNotificationProcessor()
                .withDefaultCapabilities()
                .withRequestProcessor(makeToastProcessor)
                .withRequestProcessor(cancelToastProcessor)
                .withRequestProcessor(restockToasterProcessor);

        //3. Initialize DataStores
        if (initDataStore) {
            InputStream initialOperationalData = Main.class
                    .getResourceAsStream("/initial-toaster-operational-datastore.xml");
            InputStream initialConfigurationData = Main.class
                    .getResourceAsStream("/initial-toaster-config-datastore.xml");

            netconfDeviceBuilder.setInitialOperationalData(initialOperationalData)
                    .setInitialConfigurationData(initialConfigurationData);
        }

        NetconfDevice netconfDevice = netconfDeviceBuilder.build();
        toasterService.setNotificationPublishService(
                netconfDevice.getNetconfDeviceServices().getNotificationPublishService());

        netconfDevice.start();
        //4. Register shutdown hook
        shutdownHook = new ShutdownHook(netconfDevice, toasterService);
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    public void shutdown() {
        if (shutdownHook != null) {
            shutdownHook.execute();
        }
    }

    private static class ShutdownHook extends Thread {

        private final NetconfDevice netConfDevice;
        private final ToasterServiceImpl toasterService;

        ShutdownHook(NetconfDevice netConfDevice, ToasterServiceImpl toasterService) {
            this.netConfDevice = netConfDevice;
            this.toasterService = toasterService;
        }

        @Override
        public void run() {
            this.execute();
        }

        @SuppressWarnings("checkstyle:IllegalCatch")
        public void execute() {
            LOG.info("Shutting down Lighty-Toaster device.");
            if (toasterService != null) {
                toasterService.close();
            }
            if (netConfDevice != null) {
                try {
                    netConfDevice.close();
                } catch (Exception e) {
                    LOG.error("Failed to close Netconf device properly", e);
                }
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static int getPortFromArgs(String[] args) {
        try {
            return Integer.parseInt(args[0]);
        } catch (Exception e) {
            return 17830;
        }
    }

}
