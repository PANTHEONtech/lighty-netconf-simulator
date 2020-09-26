/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.utils;

import org.w3c.dom.Node;

public enum DefaultOperation {
    NONE("none"), REPLACE("replace"), MERGE("merge");
    private final String operationName;

    DefaultOperation(String name) {
        this.operationName = name;
    }

    public String getOperationName() {
        return operationName;
    }

    public static DefaultOperation getOperationByName(String operationName) {
        for (DefaultOperation operation : DefaultOperation.values()) {
            if (operation.getOperationName().equalsIgnoreCase(operationName)) {
                return operation;
            }
        }
        throw new UnsupportedOperationException(
                String.format("Default operation %s is not a valid name for netconf default operation", operationName));
    }

    public static DefaultOperation getOperationByName(Node existingNode) {
        return getOperationByName(existingNode.getChildNodes().item(0).getTextContent());
    }
}
