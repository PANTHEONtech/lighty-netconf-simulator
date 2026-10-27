/*
 * Copyright (c) 2026 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.subscribed.notifications.rev190909.EstablishSubscription;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.subscribed.notifications.rev190909.SubscriptionStarted;
import org.opendaylight.yangtools.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

class SchemaUtilsTest {

    private static EffectiveModelContext effectiveModelContext;

    @BeforeAll
    static void setUp() {
        effectiveModelContext = BindingRuntimeHelpers.createEffectiveModel(EstablishSubscription.class);
    }

    @Test
    void testLoadRpc() {
        final Optional<? extends RpcDefinition> loadedRpc =
            SchemaUtils.loadRpc(effectiveModelContext, EstablishSubscription.QNAME);
        assertTrue(loadedRpc.isPresent());
        assertEquals(EstablishSubscription.QNAME, loadedRpc.orElseThrow().getQName());
    }

    @Test
    void testLoadRpcWithoutRevision() {
        final QName noRevision = QName.create(EstablishSubscription.QNAME.getNamespace().toString(),
            EstablishSubscription.QNAME.getLocalName());
        assertTrue(SchemaUtils.loadRpc(effectiveModelContext, noRevision).isPresent());
    }

    @Test
    void testLoadNotification() {
        final Optional<? extends NotificationDefinition> loadedNotification =
            SchemaUtils.loadNotification(effectiveModelContext, SubscriptionStarted.QNAME);
        assertTrue(loadedNotification.isPresent());
        assertEquals(SubscriptionStarted.QNAME, loadedNotification.orElseThrow().getQName());
    }

    @Test
    void testLoadMissing() {
        final QName missing = QName.create(EstablishSubscription.QNAME, "non-existent");
        assertTrue(SchemaUtils.loadRpc(effectiveModelContext, missing).isEmpty());
        assertTrue(SchemaUtils.loadNotification(effectiveModelContext, missing).isEmpty());
    }
}
