/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology.datastore;

import io.lighty.netconf.device.requests.notification.NotificationPublishService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataObjectDeleted;
import org.opendaylight.mdsal.binding.api.DataObjectModification.WithDataAfter;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.NewTopologyCreated;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.NewTopologyCreatedBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.TopologyDeleted;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.TopologyDeletedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TopologyDataTreeChangeListener implements DataTreeChangeListener<NetworkTopology> {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataTreeChangeListener.class);

    private final NotificationPublishService notificationPublishService;

    TopologyDataTreeChangeListener(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<NetworkTopology>> changes) {
        changes.forEach(change -> {
            final var rootNode = change.getRootNode();
            Set<TopologyId> ids = new HashSet<>();
            switch (rootNode) {
                case WithDataAfter<NetworkTopology> written -> {
                    final var dataBefore = written.dataBefore();
                    final var dataAfter = written.dataAfter();
                    int sizeOfDataBefore = 0;
                    int sizeOfDataAfter = dataAfter.nonnullTopology().size();
                    if (dataBefore != null) {
                        sizeOfDataBefore = dataBefore.nonnullTopology().size();
                    }

                    if (sizeOfDataBefore < sizeOfDataAfter) {
                        LOG.info("Data has been created");
                        if (dataBefore != null) {
                            dataBefore.nonnullTopology().values().forEach(
                                topology -> ids.add(topology.getTopologyId()));
                        }

                        dataAfter.nonnullTopology().values().forEach(topology -> {
                            if (!ids.contains(topology.getTopologyId())) {
                                notificationPublishService.publish(new NewTopologyCreatedBuilder()
                                    .setTopologyId(topology.getTopologyId())
                                    .build(), NewTopologyCreated.QNAME);
                            }
                        });
                    } else {
                        LOG.info("Data has been modified");
                        notificationForDeletedData(dataBefore, dataAfter, ids);
                    }
                }
                case DataObjectDeleted<NetworkTopology> deleted -> {
                    LOG.info("Data has been deleted");
                    notificationForDeletedData(deleted.dataBefore(), null, ids);
                }
            }
        });
    }

    private void notificationForDeletedData(final NetworkTopology dataBefore, final NetworkTopology dataAfter,
                                            final Set<TopologyId> ids) {
        if (dataBefore != null) {
            dataBefore.nonnullTopology().values().forEach(topology -> ids.add(topology.getTopologyId()));
        }
        if (dataAfter != null) {
            dataAfter.nonnullTopology().values().forEach(topology -> ids.remove(topology.getTopologyId()));
        }
        if (ids.iterator().hasNext()) {
            notificationPublishService.publish(new TopologyDeletedBuilder()
                    .setTopologyIds(new HashSet<>(ids))
                    .build(), TopologyDeleted.QNAME);
        }
    }

}
