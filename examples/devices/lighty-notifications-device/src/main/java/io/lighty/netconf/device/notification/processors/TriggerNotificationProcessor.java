/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.notification.processors;

import io.lighty.codecs.DataCodec;
import io.lighty.codecs.XmlNodeConverter;
import io.lighty.codecs.api.SerializationException;
import io.lighty.netconf.device.requests.RpcOutputRequestProcessor;
import io.lighty.netconf.device.requests.notification.NotificationPublishService;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.response.ResponseData;
import io.lighty.netconf.device.utils.RPCUtil;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import javax.xml.transform.TransformerException;
import org.opendaylight.yang.gen.v1.yang.lighty.test.notifications.rev180820.DataNotification;
import org.opendaylight.yang.gen.v1.yang.lighty.test.notifications.rev180820.DataNotificationBuilder;
import org.opendaylight.yang.gen.v1.yang.lighty.test.notifications.rev180820.TriggerDataNotificationInput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Class is processing rpc requests, currently its looking for triggerDataNotification rpc only.
 * When the rpc is triggered execute method is called. Using device notification publish service to pass to
 * LightyTriggerNotificationsImpl.
 */
@SuppressWarnings("checkstyle:MemberName")
public class TriggerNotificationProcessor extends RpcOutputRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerNotificationProcessor.class);

    private final QName qName = QName.create("yang:lighty:test:notifications", "triggerDataNotification");
    private NotificationPublishService notificationPublishService;
    private DataCodec<TriggerDataNotificationInput> dataCodec;

    public void init(final NotificationPublishService paramNotificationPublishService) {
        this.notificationPublishService = paramNotificationPublishService;
        this.dataCodec = new DataCodec<>(getNetconfDeviceServices().getSchemaContext());
    }

    /**
     * Converts Xml input element into TriggerDataNotificationInput using DataCodec
     * and publish notification.
     *
     * @param requestXmlElement XML RPC request element
     * @return The future result of RPC processing
     */
    @Override
    protected CompletableFuture<Response> execute(final Element requestXmlElement) {
        try {
            final XmlNodeConverter xmlNodeConverter = getNetconfDeviceServices().getXmlNodeConverter();
            try (Reader readerFromElement = RPCUtil.createReaderFromElement(requestXmlElement)) {
                final NormalizedNode<?, ?> deserializedNode =
                        xmlNodeConverter.deserialize(getRpcDefinition().getInput(), readerFromElement);
                final TriggerDataNotificationInput input = this.dataCodec
                        .convertToBindingAwareRpc(getRpcDefinition().getInput().getPath(),
                                (ContainerNode) deserializedNode);

                final DataNotification notification = createNotification(input);

                LOG.info("sending notification clientId={} {}/{}", input.getClientId(), 1, input.getCount());
                this.notificationPublishService.publish(notification, DataNotification.QNAME);
            }
            return CompletableFuture.completedFuture(new ResponseData(Collections.emptyList()));
        } catch (final TransformerException | SerializationException | IOException ex) {
            LOG.error("Could not trigger notification, {}", ex.getMessage());
            return CompletableFuture.failedFuture(ex);
        }
    }

    private DataNotification createNotification(final TriggerDataNotificationInput input) {
        LOG.info("triggering notifications: clientId={} {} delay={}ms, payload={}",
                input.getClientId(), input.getCount(), input.getDelay(), input.getPayload());
        return new DataNotificationBuilder()
                .setClientId(input.getClientId())
                .setOrdinal(input.getCount())
                .setPayload(input.getPayload())
                .build();
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }
}
