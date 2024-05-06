/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology.datastore;

import io.lighty.netconf.device.requests.notification.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataTreeChangeListenerActivator {

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerActivator.class);
    private static final InstanceIdentifier<NetworkTopology> TOPOLOGY_II =
            InstanceIdentifier.create(NetworkTopology.class);

    private final DataBroker dataBroker;
    private final NotificationPublishService notificationPublishService;
    private Registration dataTreeChangeListenerRegistration;

    public DataTreeChangeListenerActivator(final NotificationPublishService notificationPublishService,
                                           final DataBroker dataBroker) {
        this.notificationPublishService = notificationPublishService;
        this.dataBroker = dataBroker;
    }

    public void init() {
        TopologyDataTreeChangeListener topologyDataTreeChangeListener =
                new TopologyDataTreeChangeListener(notificationPublishService);
        dataTreeChangeListenerRegistration =
                dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                        TOPOLOGY_II), topologyDataTreeChangeListener);

        LOG.info("Data tree change listener registered");
    }

    public void close() {
        dataTreeChangeListenerRegistration.close();
    }
}
