/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.utils;

import org.w3c.dom.Node;

public enum Operation {
    CREATE("create"), REPLACE("replace"), DELETE("delete"), REMOVE("remove"), MERGE("merge");
    private final String operationName;

    Operation(String name) {
        this.operationName = name;
    }

    public String getOperationName() {
        return operationName;
    }

    public static Operation getOperationByName(String operationName) {
        for (Operation operation : Operation.values()) {
            if (operation.getOperationName().equalsIgnoreCase(operationName)) {
                return operation;
            }
        }
        throw new UnsupportedOperationException(
                String.format("Operation %s is not a valid name for netconf operation", operationName));
    }

    public static Operation getOperationByName(Node existingNode) {
        Node namedItemNS = existingNode.getAttributes().getNamedItemNS(RPCUtil.NETCONF_BASE_NAMESPACE, "operation");
        return getOperationByName(namedItemNS.getNodeValue());
    }
}
