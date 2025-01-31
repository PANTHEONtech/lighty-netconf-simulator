/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.core.common.models.ModuleId;
import io.lighty.netconf.device.NetconfDevice;
import io.lighty.netconf.device.NetconfDeviceBuilder;
import io.lighty.netconf.device.toaster.processors.ToasterServiceCancelToastProcessor;
import io.lighty.netconf.device.toaster.processors.ToasterServiceMakeToastProcessor;
import io.lighty.netconf.device.toaster.processors.ToasterServiceRestockToasterProcessor;
import io.lighty.netconf.device.toaster.rpcs.ToasterServiceImpl;
import io.lighty.netconf.device.utils.ArgumentParser;
import io.lighty.netconf.device.utils.ModelUtils;
import java.io.File;
import java.util.List;
import java.util.Set;
import net.sourceforge.argparse4j.inf.Namespace;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private ShutdownHook shutdownHook;

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true);
    }

    public void start(String[] args) {
        start(args, false);
    }

    @SuppressFBWarnings({"SLF4J_SIGN_ONLY_FORMAT", "OBL_UNSATISFIED_OBLIGATION"})
    public void start(String[] args, boolean registerShutdownHook) {
        final ArgumentParser argumentParser = new ArgumentParser();
        final Namespace parseArguments = argumentParser.parseArguments(args);

        //parameters are stored as string list
        final List<?> portList = parseArguments.get("port");
        final int port = Integer.parseInt(String.valueOf(portList.getFirst()));

        LOG.info("Lighty-Toaster device started at port {}", port);
        LOG.info("___________             __        ________              .__");
        LOG.info("\\__    ___/___  _______/  |_______\\______ \\   _______  _|__| ____  ____");
        LOG.info("  |    | /  _ \\/  ___/\\   __\\_  __ \\    |  \\_/ __ \\  \\/ /  |/ ___\\/ __ \\");
        LOG.info("  |    |(  <_> )___ \\  |  |  |  | \\/    `   \\  ___/\\   /|  \\  \\__\\  ___/");
        LOG.info("  |____| \\____/____  > |__|  |__| /_______  /\\___  >\\_/ |__|\\___  >___  >");
        LOG.info("                   \\/                     \\/     \\/             \\/    \\/");

        LOG.info("[https://lighty.io]");

        //1. Load models from classpath
        Set<YangModuleInfo> toasterModules = ModelUtils.getModelsFromClasspath(
            ModuleId.from(
                "http://netconfcentral.org/ns/toaster", "toaster", "2009-11-20"));

        //2. Initialize RPCs
        ToasterServiceImpl toasterService = new ToasterServiceImpl();
        ToasterServiceMakeToastProcessor makeToastProcessor =
            new ToasterServiceMakeToastProcessor(toasterService);
        ToasterServiceCancelToastProcessor cancelToastProcessor =
            new ToasterServiceCancelToastProcessor(toasterService);
        ToasterServiceRestockToasterProcessor restockToasterProcessor =
            new ToasterServiceRestockToasterProcessor(toasterService);

        //3. Initialize Netconf device
        final NetconfDeviceBuilder netconfDeviceBuilder = new NetconfDeviceBuilder()
                .setCredentials("admin", "admin")
                .setBindingPort(port)
                .withModels(toasterModules)
                .withDefaultRequestProcessors()
                .withDefaultNotificationProcessor()
                .withDefaultCapabilities()
                .withRequestProcessor(makeToastProcessor)
                .withRequestProcessor(cancelToastProcessor)
                .withRequestProcessor(restockToasterProcessor);

        // Initialize DataStores
        File operationalFile = null;
        File configFile = null;
        final String configDir = System.getProperty("config.dir",
            "./examples/devices/lighty-toaster-device/src/main/resources");
        if (argumentParser.isInitDatastore()) {
            LOG.info("Using initial datastore from: {}", configDir);
            operationalFile = new File(
                configDir, "initial-toaster-operational-datastore.xml");
            configFile = new File(
                configDir, "initial-toaster-config-datastore.xml");
        }
        if (argumentParser.isSaveDatastore()) {
            operationalFile = new File(configDir, "initial-toaster-operational-datastore.xml");
            configFile = new File(configDir, "initial-toaster-config-datastore.xml");
        }

        NetconfDevice netconfDevice = netconfDeviceBuilder
            .setOperationalDatastore(operationalFile)
            .setConfigDatastore(configFile)
            .build();
        toasterService.setNotificationPublishService(
                netconfDevice.getNetconfDeviceServices().getNotificationPublishService());

        netconfDevice.start();
        //5. Register shutdown hook
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
}
