/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.utils;

import com.google.common.collect.ImmutableSet;
import io.lighty.core.common.models.ModuleId;
import io.lighty.core.common.models.YangModuleUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public final class ModelUtils {

    private ModelUtils() {
        throw new UnsupportedOperationException("do not instantiate utility class");
    }

    public static final Set<String> DEFAULT_CAPABILITIES =
            ImmutableSet.of("urn:ietf:params:netconf:base:1.0", "urn:ietf:params:netconf:base:1.1");
    public static final String DEFAULT_NOTIFICATION_CAPABILITY =
        "urn:ietf:params:netconf:capability:notification:1.0";

    /**
     * Get all Yang modules from classpath filtered by top-level module.
     * @param filter
     *     The collection of top-level modules represented by name and revision.
     * @return
     *     Collection top-level module and all of it's imported yang module dependencies recursively.
     *     Empty collection is returned if no suitable modules are found.
     */
    public static Set<YangModuleInfo> getModelsFromClasspath(final ModuleId... filter) {
        return YangModuleUtils.getModelsFromClasspath(new HashSet<>(Arrays.asList(filter)));
    }

    /**
     * Returns the {@link RpcDefinition} identified by the given {@link QName}.
     *
     * <p>If the {@link QName} has no revision, the module is looked up by namespace only and must be unique.
     *
     * @param effectiveModelContext model context to search
     * @param rpcQName {@link QName} of the RPC
     * @return found {@link RpcDefinition}, or empty if it is not found or is ambiguous
     */
    public static Optional<? extends RpcDefinition> loadRpc(final EffectiveModelContext effectiveModelContext,
            final QName rpcQName) {
        return findModule(effectiveModelContext, rpcQName)
            .flatMap(module -> findDefinition(rpcQName, module.getRpcs()));
    }

    /**
     * Returns the {@link NotificationDefinition} identified by the given {@link QName}.
     *
     * <p>If the {@link QName} has no revision, the module is looked up by namespace only and must be unique.
     *
     * @param effectiveModelContext model context to search
     * @param notificationQName {@link QName} of the notification
     * @return found {@link NotificationDefinition}, or empty if it is not found or is ambiguous
     */
    public static Optional<? extends NotificationDefinition> loadNotification(
            final EffectiveModelContext effectiveModelContext, final QName notificationQName) {
        return findModule(effectiveModelContext, notificationQName)
            .flatMap(module -> findDefinition(notificationQName, module.getNotifications()));
    }

    private static Optional<Module> findModule(final EffectiveModelContext effectiveModelContext, final QName qname) {
        if (qname.getRevision().isPresent()) {
            return effectiveModelContext.findModule(qname.getNamespace(), qname.getRevision());
        }

        final Collection<? extends Module> modules = effectiveModelContext.findModules(qname.getNamespace());
        return modules.size() == 1 ? Optional.of(modules.iterator().next()) : Optional.empty();
    }

    private static <T extends SchemaNode> Optional<T> findDefinition(final QName qname, final Collection<T> nodes) {
        final List<T> found = nodes.stream()
            .filter(node -> node.getQName().getLocalName().equals(qname.getLocalName()))
            .toList();
        return found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty();
    }

}
