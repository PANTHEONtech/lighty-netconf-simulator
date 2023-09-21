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
import org.opendaylight.netconf.server.api.operations.NetconfOperation;
import org.opendaylight.netconf.server.api.operations.NetconfOperationService;
import org.opendaylight.netconf.test.tool.rpc.DataList;
import org.opendaylight.netconf.test.tool.rpc.SimulatedCommit;
import org.opendaylight.netconf.test.tool.rpc.SimulatedCreateSubscription;
import org.opendaylight.netconf.test.tool.rpc.SimulatedDiscardChanges;
import org.opendaylight.netconf.test.tool.rpc.SimulatedEditConfig;
import org.opendaylight.netconf.test.tool.rpc.SimulatedGet;
import org.opendaylight.netconf.test.tool.rpc.SimulatedGetConfig;
import org.opendaylight.netconf.test.tool.rpc.SimulatedLock;
import org.opendaylight.netconf.test.tool.rpc.SimulatedUnLock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.SessionIdType;

public class NotificationService implements NetconfOperationService {

    private final NetconfOperation netconfOperation;
    private final SessionIdType sessionIdType;

    NotificationService(final NetconfOperation netconfOperation, final SessionIdType idType) {
        this.netconfOperation = netconfOperation;
        this.sessionIdType = idType;
    }

    @Override
    public Set<NetconfOperation> getNetconfOperations() {
        final DataList storage = new DataList();
        final SimulatedGet sGet = new SimulatedGet(sessionIdType, storage);
        final SimulatedEditConfig sEditConfig = new SimulatedEditConfig(sessionIdType, storage);
        final SimulatedGetConfig sGetConfig = new SimulatedGetConfig(sessionIdType, storage, Optional.empty());
        final SimulatedCommit sCommit = new SimulatedCommit(sessionIdType);
        final SimulatedLock sLock = new SimulatedLock(sessionIdType);
        final SimulatedUnLock sUnlock = new SimulatedUnLock(sessionIdType);
        final SimulatedCreateSubscription sCreateSubs = new SimulatedCreateSubscription(sessionIdType,
            Optional.empty());
        final SimulatedDiscardChanges sDiscardChanges = new SimulatedDiscardChanges(sessionIdType);
        return Sets.newHashSet(sGet, sGetConfig, sEditConfig, sCommit, sLock, sUnlock, sCreateSubs, sDiscardChanges,
            netconfOperation);
    }

    @Override
    public void close() {

    }

}
