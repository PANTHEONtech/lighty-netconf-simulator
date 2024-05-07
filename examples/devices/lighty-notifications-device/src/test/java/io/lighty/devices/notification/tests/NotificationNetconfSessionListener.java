/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.devices.notification.tests;

import java.util.concurrent.CountDownLatch;
import org.opendaylight.netconf.api.messages.NetconfMessage;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;

public class NotificationNetconfSessionListener  extends SimpleNetconfClientSessionListener {

    private CountDownLatch countDownLatch;
    private String expectedPayload;

    public NotificationNetconfSessionListener(CountDownLatch countDownLatch, String expectedPayload) {
        this.countDownLatch = countDownLatch;
        this.expectedPayload = expectedPayload;
    }

    @Override
    public synchronized void onMessage(final NetconfClientSession session, final NetconfMessage message) {
        super.onMessage(session, message);
        if (isNotification(message)) {
            if (checkNotificationPayload(message)) {
                this.countDownLatch.countDown();
            }
        }
    }

    private boolean checkNotificationPayload(NetconfMessage message) {
        return message.getDocument().getDocumentElement().getElementsByTagName("Payload")
                .item(0).getTextContent().equals(this.expectedPayload);
    }

    private boolean isNotification(NetconfMessage message) {
        return message.getDocument().getDocumentElement().getLocalName().equals("notification");
    }
}
