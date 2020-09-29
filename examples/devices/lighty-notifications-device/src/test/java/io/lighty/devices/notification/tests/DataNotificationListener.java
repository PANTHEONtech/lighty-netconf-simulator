package io.lighty.devices.notification.tests;

import java.util.concurrent.CountDownLatch;
import org.opendaylight.yang.gen.v1.yang.lighty.test.notifications.rev180820.DataNotification;
import org.opendaylight.yang.gen.v1.yang.lighty.test.notifications.rev180820.LightyTestNotificationsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataNotificationListener implements LightyTestNotificationsListener {

    private final CountDownLatch cdl;
    private static final Logger LOG = LoggerFactory.getLogger(DataNotificationListener.class);


    public DataNotificationListener(final CountDownLatch cdl) {
        this.cdl = cdl;
    }

    @Override
    public void onDataNotification(DataNotification notification) {
        LOG.info("Catched notification");
        cdl.countDown();
    }
}
