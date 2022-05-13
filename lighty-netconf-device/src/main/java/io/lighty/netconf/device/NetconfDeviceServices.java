/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device;

import io.lighty.codecs.util.XmlNodeConverter;
import io.lighty.netconf.device.requests.notification.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;

/**
 * This API provides properly initialized services from NETCONF device instance.
 */
public interface NetconfDeviceServices {

    DataBroker getDataBroker();

    DOMDataBroker getDOMDataBroker();

    Inference getRootInference();

    AdapterContext getAdapterContext();

    NotificationService getNotificationService();

    NotificationPublishService getNotificationPublishService();

    XmlNodeConverter getXmlNodeConverter();

}
