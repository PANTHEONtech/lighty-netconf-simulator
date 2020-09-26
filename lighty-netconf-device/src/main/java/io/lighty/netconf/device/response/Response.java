/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.response;

import java.util.List;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.w3c.dom.Document;

public interface Response {

    /**
     * Data returned according to successful request.
     *
     * @return data
     */
    List<NormalizedNode<? ,?>> getData();

    /**
     * Specific Error returned according to unsuccessful requests.
     *
     * @return error
     */
    Document getErrorDocument();

}
