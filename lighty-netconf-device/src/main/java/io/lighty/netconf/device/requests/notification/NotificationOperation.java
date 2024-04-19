/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.requests.notification;

import io.lighty.codecs.util.ConverterUtils;
import io.lighty.codecs.util.XmlNodeConverter;
import io.lighty.codecs.util.exception.SerializationException;
import io.lighty.netconf.device.utils.RPCUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.messages.NetconfMessage;
import org.opendaylight.netconf.api.NetconfSession;
import org.opendaylight.netconf.server.api.operations.HandlingPriority;
import org.opendaylight.netconf.server.api.operations.NetconfOperationChainedExecution;
import org.opendaylight.netconf.server.api.operations.SessionAwareNetconfOperation;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class NotificationOperation implements SessionAwareNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationOperation.class);
    private final EffectiveModelContext effectiveModelContext;
    private final AdapterContext adapterContext;

    private final Map<String, List<NetconfSession>> sessions = new HashMap<>();
    private boolean isSubscription;
    private String streamName;


    public NotificationOperation(final AdapterContext adapterContext) {
        this.adapterContext = adapterContext;
        this.effectiveModelContext = adapterContext.currentSerializer().getRuntimeContext().modelContext();
    }

    public void sendMessage(final Notification notificationMessage, final QName quName) {
        final List<NetconfSession> sessionList = this.sessions.get(quName.getLocalName());
        if (sessionList != null && !sessionList.isEmpty()) {
            final ContainerNode containerNode = this.adapterContext.currentSerializer()
                    .toNormalizedNodeNotification(notificationMessage);

            final Optional<? extends NotificationDefinition> notificationDefinition =
                    ConverterUtils.loadNotification(this.effectiveModelContext, quName);
            final XmlNodeConverter xmlNodeConverter = new XmlNodeConverter(this.effectiveModelContext);

            if (notificationDefinition.isEmpty()) {
                throw new UnsupportedOperationException("Cannot load definition for QName: " + quName);
            }

            final Writer writer;
            try {
                writer = xmlNodeConverter.serializeRpc(Absolute.of(notificationDefinition.get().getQName()),
                        containerNode);
                try (InputStream is = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8))) {
                    final DocumentBuilder builder = UntrustedXML.newDocumentBuilder();
                    final Document notification = builder.parse(is);
                    final Element body =
                        notification.createElementNS(RPCUtil.CREATE_SUBSCRIPTION_NAMESPACE,
                            "notification");

                    final Element notificationElement = notification.getDocumentElement();
                    final Element eventTime = notification.createElement("eventTime");
                    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    eventTime.setTextContent(dateFormat.format(new Date()));
                    body.appendChild(eventTime);
                    body.appendChild(notificationElement);
                    final Document document = builder.newDocument();
                    final org.w3c.dom.Node importNode = document.importNode(body, true);
                    document.appendChild(importNode);
                    final NetconfMessage netconfMessage = new NetconfMessage(document);
                    LOG.debug("Sending notification message: {}", netconfMessage.toString());
                    sessionList.forEach(session -> session.sendMessage(netconfMessage));
                } catch (IOException | SAXException e) {
                    LOG.error("Failed to send notification message", e);
                }
            } catch (final SerializationException e) {
                LOG.error("Failed to serialize notification to xml", e);
            }
        }
    }

    @Override
    public void setSession(final NetconfSession session) {
        if (this.isSubscription) {
            if (this.streamName == null) {
                final Collection<? extends NotificationDefinition> notifications =
                        this.effectiveModelContext.getNotifications();
                notifications.forEach(notification -> {
                    final String name = notification.getQName().getLocalName();
                    List<NetconfSession> sessionsList = this.sessions.get(name);
                    if (sessionsList == null) {
                        sessionsList = new ArrayList<>();
                    }
                    sessionsList.add(session);
                    this.sessions.put(name, sessionsList);
                });
            } else {
                List<NetconfSession> sessionsList = this.sessions.get(this.streamName);
                if (sessionsList == null) {
                    sessionsList = new ArrayList<>();
                }
                sessionsList.add(session);
                this.sessions.put(this.streamName, sessionsList);
            }
        }
    }

    @Override
    public HandlingPriority canHandle(final Document message) throws DocumentedException {
        this.isSubscription = message.getDocumentElement().getElementsByTagName("create-subscription").getLength() == 1;
        if (this.isSubscription) {
            final Node stream = message.getDocumentElement().getElementsByTagName("stream").item(0);
            if (stream == null) {
                this.streamName = null;
            } else {
                this.streamName = stream
                        .getChildNodes()
                        .item(0)
                        .getTextContent().split(":")[1];
            }
        }
        return HandlingPriority.getHandlingPriority(0);
    }

    @Override
    public Document handle(final Document requestMessage,
        final NetconfOperationChainedExecution subsequentOperation) throws DocumentedException {
        return subsequentOperation.execute(requestMessage);
    }

}
