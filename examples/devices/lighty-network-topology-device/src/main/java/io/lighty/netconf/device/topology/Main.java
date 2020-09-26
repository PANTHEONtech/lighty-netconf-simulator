/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.core.common.models.ModuleId;
import io.lighty.netconf.device.NetconfDevice;
import io.lighty.netconf.device.NetconfDeviceBuilder;
import io.lighty.netconf.device.topology.datastore.DataTreeChangeListenerActivator;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceAddNodeToTopologyProcessor;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceCreateTopologyProcessor;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceGetNodeFromTopologyProcessor;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceGetTopologiesProcessor;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceGetTopologyByIdProcessor;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceGetTopologyIdsProcessor;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceRemoveAllTopologiesProcessor;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceRemoveNodeProcessor;
import io.lighty.netconf.device.topology.processors.NetworkTopologyServiceRemoveTopologyProcessor;
import io.lighty.netconf.device.topology.rpcs.NetworkTopologyServiceImpl;
import io.lighty.netconf.device.utils.ModelUtils;
import java.io.InputStream;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private ShutdownHook shutdownHook;

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true, true);
    }

    public void start(String[] args) {
        start(args, false, true);
    }

    @SuppressFBWarnings({"SLF4J_SIGN_ONLY_FORMAT", "OBL_UNSATISFIED_OBLIGATION"})
    public void start(String[] args, boolean registerShutdownHook, final boolean initDatastore) {
        int port = getPortFromArgs(args);
        LOG.info("Lighty-Network-Topology device started {}", port);
        LOG.info(" _______          __ ________              .__");
        LOG.info(" \\      \\   _____/  |\\______ \\   _______  _|__| ____  ____");
        LOG.info(" /   |   \\_/ __ \\   __\\    |  \\_/ __ \\  \\/ /  |/ ___\\/ __ ");
        LOG.info("/    |    \\  ___/|  | |    `   \\  ___/\\   /|  \\  \\__\\  ___/");
        LOG.info("\\____|__  /\\___  >__|/_______  /\\___  >\\_/ |__|\\___  >___  >");
        LOG.info(" \\/     \\/            \\/     \\/             \\/    \\/");
        LOG.info("[https://lighty.io]");

        //1. Load models from classpath
        Set<YangModuleInfo> modules = ModelUtils.getModelsFromClasspath(
                ModuleId.from("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
                    "network-topology-rpcs",
                    "2018-03-20"),
                ModuleId.from("urn:opendaylight:netconf-node-topology",
                    "netconf-node-topology",
                    "2015-01-14"));

        //2. Initialize DataStores
        InputStream initialOperationalData = null;
        InputStream initialConfigurationData = null;
        if (initDatastore) {
            initialOperationalData = Main.class.getResourceAsStream("/initial-network-topo-operational-datastore.xml");
            initialConfigurationData = Main.class.getResourceAsStream("/initial-network-topo-config-datastore.xml");
        }

        //3. Initialize RPCs
        NetworkTopologyServiceImpl networkTopologyService = new NetworkTopologyServiceImpl();

        //4. Initialize Netconf device
        NetconfDevice netconfDevice = new NetconfDeviceBuilder()
                .setCredentials("admin", "admin")
                .setBindingPort(port)
                .withModels(modules)
                .withDefaultRequestProcessors()
                .withDefaultCapabilities()
                .setInitialOperationalData(initialOperationalData)
                .setInitialConfigurationData(initialConfigurationData)
                .withRequestProcessor(new NetworkTopologyServiceGetTopologiesProcessor(networkTopologyService))
                .withRequestProcessor(new NetworkTopologyServiceGetTopologyByIdProcessor(networkTopologyService))
                .withRequestProcessor(new NetworkTopologyServiceGetTopologyIdsProcessor(networkTopologyService))
                .withRequestProcessor(new NetworkTopologyServiceGetNodeFromTopologyProcessor(networkTopologyService))
                .withRequestProcessor(new NetworkTopologyServiceCreateTopologyProcessor(networkTopologyService))
                .withRequestProcessor(new NetworkTopologyServiceAddNodeToTopologyProcessor(networkTopologyService))
                .withRequestProcessor(new NetworkTopologyServiceRemoveTopologyProcessor(networkTopologyService))
                .withRequestProcessor(new NetworkTopologyServiceRemoveAllTopologiesProcessor(networkTopologyService))
                .withRequestProcessor(new NetworkTopologyServiceRemoveNodeProcessor(networkTopologyService))
                .withDefaultNotificationProcessor()
                .build();
        netconfDevice.start();
        final DataBroker dataBroker = netconfDevice.getNetconfDeviceServices().getDataBroker();
        networkTopologyService.setDataBrokerService(dataBroker);
        DataTreeChangeListenerActivator listenerActivator =
                new DataTreeChangeListenerActivator(
                    netconfDevice.getNetconfDeviceServices().getNotificationPublishService(),
                    dataBroker);
        listenerActivator.init();

        networkTopologyService.setDataBrokerService(
                netconfDevice.getNetconfDeviceServices().getDataBroker());
        networkTopologyService.setSchemaContext(
                netconfDevice.getNetconfDeviceServices().getSchemaContext());

        //5. Register shutdown hook
        this.shutdownHook = new ShutdownHook(netconfDevice, networkTopologyService, listenerActivator);
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    public void shutdown() {
        if (shutdownHook != null) {
            shutdownHook.execute();
        }
    }

    private static class ShutdownHook extends Thread {

        private final NetconfDevice netConfDevice;
        private final NetworkTopologyServiceImpl networkTopologyService;
        private final DataTreeChangeListenerActivator listener;

        ShutdownHook(final NetconfDevice netConfDevice, final NetworkTopologyServiceImpl networkTopologyService,
                     final DataTreeChangeListenerActivator listener) {
            this.netConfDevice = netConfDevice;
            this.networkTopologyService = networkTopologyService;
            this.listener = listener;
        }

        @Override
        public void run() {
            this.execute();
        }

        @SuppressWarnings("checkstyle:IllegalCatch")
        public void execute() {
            LOG.info("Shutting down Lighty-Network-Topology device.");
            if (networkTopologyService != null) {
                networkTopologyService.close();
            }
            if (listener != null) {
                listener.close();
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
