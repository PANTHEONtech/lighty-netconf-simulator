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
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

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

}
