/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests.notification;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.netconf.impl.SessionIdProvider;
import org.opendaylight.netconf.mapping.api.NetconfOperation;
import org.opendaylight.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.netconf.test.tool.rpc.DataList;
import org.opendaylight.netconf.test.tool.rpc.SimulatedCommit;
import org.opendaylight.netconf.test.tool.rpc.SimulatedCreateSubscription;
import org.opendaylight.netconf.test.tool.rpc.SimulatedDiscardChanges;
import org.opendaylight.netconf.test.tool.rpc.SimulatedEditConfig;
import org.opendaylight.netconf.test.tool.rpc.SimulatedGet;
import org.opendaylight.netconf.test.tool.rpc.SimulatedGetConfig;
import org.opendaylight.netconf.test.tool.rpc.SimulatedLock;
import org.opendaylight.netconf.test.tool.rpc.SimulatedUnLock;

public class NotificationService implements NetconfOperationService {

    private final NetconfOperation netconfOperation;
    private final long currentSessionId;

    NotificationService(final NetconfOperation netconfOperation, SessionIdProvider idProvider) {
        this.netconfOperation = netconfOperation;
        this.currentSessionId = idProvider.getCurrentSessionId();
    }

    @Override
    public Set<NetconfOperation> getNetconfOperations() {
        final DataList storage = new DataList();
        final SimulatedGet sGet = new SimulatedGet(String.valueOf(currentSessionId), storage);
        final SimulatedEditConfig sEditConfig = new SimulatedEditConfig(String.valueOf(currentSessionId), storage);
        final SimulatedGetConfig sGetConfig = new SimulatedGetConfig(
                String.valueOf(currentSessionId), storage, Optional.empty());
        final SimulatedCommit sCommit = new SimulatedCommit(String.valueOf(currentSessionId));
        final SimulatedLock sLock = new SimulatedLock(String.valueOf(currentSessionId));
        final SimulatedUnLock sUnlock = new SimulatedUnLock(String.valueOf(currentSessionId));
        final SimulatedCreateSubscription sCreateSubs = new SimulatedCreateSubscription(
                String.valueOf(currentSessionId), Optional.empty());
        final SimulatedDiscardChanges sDiscardChanges = new SimulatedDiscardChanges(
                String.valueOf(currentSessionId));
        return Sets.newHashSet(
                sGet, sGetConfig, sEditConfig, sCommit, sLock, sUnlock, sCreateSubs, sDiscardChanges, netconfOperation);
    }

    @Override
    public void close() {

    }

}
