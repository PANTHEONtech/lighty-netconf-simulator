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
import java.util.stream.Collectors;
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
     * Returns the {@link RpcDefinition} from the given {@link EffectiveModelContext} and given {@link QName}.
     * The {@link QName} of a rpc can be constructed via
     *
     * <p>{@code
     * QName.create("http://netconfcentral.org/ns/toaster", "2009-11-20", "make-toast");
     * } , where {@code "make-toast"} is the name of the RPC given in the yang model.
     *
     * <p>If the given RPC was found in the {@link EffectiveModelContext} the {@link RpcDefinition} will be returned
     *
     * @param effectiveModelContext the effective model context used for the RPC resolution
     * @param rpcQName              {@link QName} of the RPC
     * @return {@link Optional} representation of the {@link RpcDefinition}
     * @see QName
     */
    public static Optional<? extends RpcDefinition> loadRpc(final EffectiveModelContext effectiveModelContext,
            final QName rpcQName) {
        Optional<Module> findModule = findModule(effectiveModelContext, rpcQName);
        if (findModule.isEmpty()) {
            return Optional.empty();
        }
        return findDefinition(rpcQName, findModule.get().getRpcs());
    }

    /**
     * Utility method to extract the {@link SchemaNode} for the given Notification.
     *
     * @param effectiveModelContext to be used
     * @param notificationQname     yang RPC name
     * @return {@link Optional} of {@link SchemaNode}
     */
    public static Optional<? extends NotificationDefinition> loadNotification(
            final EffectiveModelContext effectiveModelContext, final QName notificationQname) {
        Optional<Module> findModule = findModule(effectiveModelContext, notificationQname);
        if (!findModule.isPresent()) {
            return Optional.empty();
        }
        return findDefinition(notificationQname, findModule.get().getNotifications());
    }

    private static Optional<Module> findModule(final EffectiveModelContext effectiveModelContext, final QName qname) {
        if (qname.getRevision().isPresent()) {
            return effectiveModelContext.findModule(qname.getNamespace(), qname.getRevision());
        }

        Collection<? extends Module> moduleByNamespace = effectiveModelContext.findModules(qname.getNamespace());
        return Optional.ofNullable(moduleByNamespace.isEmpty() || moduleByNamespace.size() > 1 ? null
                : moduleByNamespace.iterator().next());
    }

    private static <T extends SchemaNode> Optional<T> findDefinition(final QName qname, final Collection<T> nodes) {
        List<T> foundNodes = nodes.stream().filter(node -> node.getQName().getLocalName().equals(qname.getLocalName()))
                .collect(Collectors.toList());
        return Optional.ofNullable(foundNodes.size() != 1 ? null : foundNodes.get(0));
    }

}
