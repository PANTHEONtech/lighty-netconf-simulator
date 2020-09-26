/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests.notification;

import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * Service for publishing NETCONF notifications.
 * Schema context of NETCONF device must contain proper yang models
 * specified by RFC5277
 */
public interface NotificationPublishService {

    @SuppressWarnings({"checkstyle:NonEmptyAtClauseDescritption", "checkstyle:ParameterName"})
    /**
     * Publish Notification.
     * @param notification
     * @param qName
     */
    void publish(Notification notification, QName qName);

}
