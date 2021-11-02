/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.response;

import java.util.Collections;
import java.util.List;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.w3c.dom.Document;

public class ResponseErrorMessage implements Response {

    private final NetconfDocumentedException data;

    public ResponseErrorMessage(final NetconfDocumentedException data) {
        this.data = data;
    }

    @Override
    public List<NormalizedNode> getData() {
        return Collections.emptyList();
    }

    @Override
    public Document getErrorDocument() {
        return data.toXMLDocument();
    }

}
