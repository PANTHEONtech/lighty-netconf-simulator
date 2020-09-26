/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests.notification;

import java.util.Set;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.netconf.api.capability.Capability;
import org.opendaylight.netconf.impl.SessionIdProvider;
import org.opendaylight.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.netconf.test.tool.operations.OperationsCreator;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NotificationPublishServiceImpl implements OperationsCreator, NotificationPublishService {

    private NotificationOperation notificationOperation;
    private SchemaContext schemaContext;
    private BindingNormalizedNodeCodecRegistry codec;

    @Override
    public void publish(final Notification notification, final QName quName) {
        // If the device is not fully started, the mountPoint will not be available, so it is not able to
        // send the notification.
        if (this.notificationOperation != null) {
            this.notificationOperation.sendMessage(notification, quName);
        }
    }

    @Override
    public NetconfOperationService getNetconfOperationService(final Set<Capability> capabilities,
            final SessionIdProvider idProvider, final String netconfSessionIdForReporting) {
        this.notificationOperation = new NotificationOperation(this.schemaContext, this.codec);
        return new NotificationService(this.notificationOperation, idProvider);
    }

    public void setSchemaContext(final SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    public void setCodec(final BindingNormalizedNodeCodecRegistry codec) {
        this.codec = codec;
    }

}
