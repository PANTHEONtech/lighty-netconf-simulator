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
import io.lighty.netconf.device.toaster.rpcs.ToasterServiceImpl;
import io.lighty.netconf.device.utils.ModelUtils;
import java.io.InputStream;
import java.util.Set;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.opendaylight.netconf.test.tool.TesttoolParameters;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public static final int DEFAULT_PORT = 17830;
    public static final int DEFAULT_DEVICE_COUNT = 1;
    public static final int DEFAULT_POOL_SIZE = 8;

    private ShutdownHook shutdownHook;

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true, true);
    }

    @SuppressFBWarnings({"SLF4J_SIGN_ONLY_FORMAT", "OBL_UNSATISFIED_OBLIGATION"})
    public void start(String[] args, boolean registerShutdownHook, final boolean initDatastore) {
        //1. Load parameters
        final TesttoolParameters params = parseArgs(args, getParser());

        LOG.info("Lighty-Toaster device started {}", params.startingPort);
        LOG.info("___________             __        ________              .__");
        LOG.info("\\__    ___/___  _______/  |_______\\______ \\   _______  _|__| ____  ____");
        LOG.info("  |    | /  _ \\/  ___/\\   __\\_  __ \\    |  \\_/ __ \\  \\/ /  |/ ___\\/ __ \\");
        LOG.info("  |    |(  <_> )___ \\  |  |  |  | \\/    `   \\  ___/\\   /|  \\  \\__\\  ___/");
        LOG.info("  |____| \\____/____  > |__|  |__| /_______  /\\___  >\\_/ |__|\\___  >___  >");
        LOG.info("                   \\/                     \\/     \\/             \\/    \\/");
        LOG.info("[https://lighty.io]");

        //2. Load models from classpath
        Set<YangModuleInfo> toasterModules = ModelUtils.getModelsFromClasspath(
                ModuleId.from(
                        "http://netconfcentral.org/ns/toaster", "toaster", "2009-11-20"));

        //3. Initialize DataStores
        InputStream initialOperationalData = null;
        InputStream initialConfigurationData = null;
        if (initDatastore) {
            initialOperationalData =
                    Main.class.getResourceAsStream("/initial-toaster-operational-datastore.xml");
            initialConfigurationData =
                    Main.class.getResourceAsStream("/initial-toaster-config-datastore.xml");
        }

        ToasterServiceImpl toasterService = new ToasterServiceImpl();

        //4. Initialize Netconf device
        NetconfDevice netconfDevice = new NetconfDeviceBuilder()
                .setCredentials("admin", "admin")
                .setBindingPort(params.startingPort)
                .withModels(toasterModules)
                .withDefaultRequestProcessors()
                .withDefaultCapabilities()
                .withRequestProcessor(new ToasterServiceMakeToastProcessor(toasterService))
                .withRequestProcessor(new ToasterServiceCancelToastProcessor(toasterService))
                .setThreadPoolSize(params.threadPoolSize)
                .setDeviceCount(params.deviceCount)
                .setInitialOperationalData(initialOperationalData)
                .setInitialConfigurationData(initialConfigurationData)
                .build();

        netconfDevice.start();

        //5. Register shutdown hook
        this.shutdownHook = new ShutdownHook(netconfDevice,toasterService);
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    public void shutdown() {
        if (shutdownHook != null) {
            shutdownHook.execute();
        }
    }

    static TesttoolParameters parseArgs(final String[] args, final ArgumentParser parser) {
        final TesttoolParameters testtoolParams = new TesttoolParameters();
        try {
            parser.parseArgs(args, testtoolParams);
        } catch (final ArgumentParserException e) {
            LOG.warn("Exception while parsing example arguments. Using default values", e);
            testtoolParams.startingPort = DEFAULT_PORT;
            testtoolParams.deviceCount = DEFAULT_DEVICE_COUNT;
            testtoolParams.threadPoolSize = DEFAULT_POOL_SIZE;
        }
        return testtoolParams;
    }

    static ArgumentParser getParser() {
        final ArgumentParser parser = ArgumentParsers.newFor("netconf").build();

        parser.addArgument("--starting-port")
                .type(Integer.class)
                .setDefault(DEFAULT_PORT)
                .help("First port for simulated device. Each other device will have previous+1 port number")
                .dest("starting-port");

        parser.addArgument("--device-count")
                .type(Integer.class)
                .setDefault(DEFAULT_DEVICE_COUNT)
                .help("Number of simulated netconf devices to spin."
                        + " This is the number of actual ports which will be used for the devices.")
                .dest("devices-count");

        parser.addArgument("--thread-pool-size")
                .type(Integer.class)
                .setDefault(DEFAULT_POOL_SIZE)
                .help("The number of threads to keep in the pool, "
                        + "when creating a device simulator, even if they are idle.")
                .dest("thread-pool-size");

        return parser;
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
