/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests;

import io.lighty.netconf.device.NetconfDeviceServices;
import org.opendaylight.yangtools.yang.common.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Netconf request processor.
 *
 */
public interface RequestProcessor {

    /**
     * Returns the QName identifier of the implementation.
     *
     * @return FQN a FQN
     */
    QName getIdentifier();

    /**
     * Parses the input element and do its operation around it.
     *
     * @param requestXmlElement XML RPC request element
     * @return Document a document
     */
    Document processRequest(Element requestXmlElement);

    /**
     * Inject services into this instance of request processor.
     * @param netconfDeviceServices NETCONF device services
     */
    void init(NetconfDeviceServices netconfDeviceServices);

}
