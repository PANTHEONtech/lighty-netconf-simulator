/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster;

import java.util.concurrent.CountDownLatch;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;

public class NotificationNetconfSessionListener  extends SimpleNetconfClientSessionListener {

    private CountDownLatch countDownLatch;
    private NetconfMessage receivedNotif;

    public NotificationNetconfSessionListener(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
        this.receivedNotif = null;
    }

    @Override
    public synchronized void onMessage(final NetconfClientSession session, final NetconfMessage message) {
        super.onMessage(session, message);
        if (isNotification(message)) {
            this.receivedNotif = message;
            this.countDownLatch.countDown();
        }
    }

    public NetconfMessage getReceivedNotificationMessage() {
        return this.receivedNotif;
    }

    private boolean isNotification(NetconfMessage message) {
        return message.getDocument().getDocumentElement().getLocalName().equals("notification");
    }
}
