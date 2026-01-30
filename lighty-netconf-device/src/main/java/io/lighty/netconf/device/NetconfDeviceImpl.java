/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import io.lighty.codecs.util.XmlNodeConverter;
import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.codecs.util.exception.SerializationException;
import io.lighty.netconf.device.requests.RequestProcessor;
import io.lighty.netconf.device.requests.RpcHandlerImpl;
import io.lighty.netconf.device.requests.notification.NotificationPublishServiceImpl;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.netconf.test.tool.config.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.ModuleLike;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDeviceImpl implements NetconfDevice {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceImpl.class);
    private static final long TIMEOUT_MILLIS = 30_000;

    private NetconfDeviceServices netconfDeviceServices;
    private NetconfDeviceSimulator netConfDeviceSimulator;
    private File operationalData;
    private File configurationData;
    private boolean netconfMonitoringEnabled;

    public NetconfDeviceImpl(Collection<YangModuleInfo> moduleInfos, Configuration config,
            File operationalData, File configurationData,
            Map<QName, RequestProcessor> requestProcessors, NotificationPublishServiceImpl creator,
            boolean netconfMonitoringEnabled) {
        if (creator != null) {
            config.setOperationsCreator(creator);
        }
        this.netconfDeviceServices = new NetconfDeviceServicesImpl(moduleInfos, creator);
        this.operationalData = operationalData;
        this.configurationData = configurationData;
        RpcHandlerImpl rpcHandler = new RpcHandlerImpl(netconfDeviceServices, requestProcessors);
        config.setRpcHandler(rpcHandler);
        this.netConfDeviceSimulator = new NetconfDeviceSimulator(config);
        this.netconfMonitoringEnabled = netconfMonitoringEnabled;
    }

    @Override
    public void start() {
        LOG.info("Starting Netconf device");
        if (operationalData != null && isNotEmpty(operationalData)) {
            initDatastore(LogicalDatastoreType.OPERATIONAL, operationalData);
        }
        if (configurationData != null && isNotEmpty(configurationData)) {
            initDatastore(LogicalDatastoreType.CONFIGURATION, configurationData);
        }
        netConfDeviceSimulator.start();
        if (netconfMonitoringEnabled) {
            try {
                prepareSchemasForNetconfMonitoring().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                LOG.info("Netconf monitoring enabled successfully");
            } catch (TimeoutException | ExecutionException e) {
                LOG.error("Could not prepare Schemas to expose through NETCONF Monitoring", e);
            } catch (InterruptedException e) {
                LOG.error("Interrupted while preparing Schemas to expose through NETCONF Monitoring", e);
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("Netconf device started");
    }

    @VisibleForTesting()
    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    void initDatastore(LogicalDatastoreType datastoreType, File initialData) {
        LOG.debug("Setting up initial state of {} datastore from XML", datastoreType);
        try (InputStream inputStream = initialData.toURI().toURL().openStream();
            Reader reader = new InputStreamReader(inputStream, Charset.defaultCharset())) {
            NormalizedNode initialDataBI = netconfDeviceServices.getXmlNodeConverter()
                    .deserialize(netconfDeviceServices.getRootInference(), reader);
            DOMDataTreeWriteTransaction writeTx = netconfDeviceServices.getDOMDataBroker().newWriteOnlyTransaction();
            writeTx.put(datastoreType, YangInstanceIdentifier.of(), initialDataBI);
            writeTx.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final String dataTreeString = NormalizedNodes.toStringTree(initialDataBI);
            LOG.trace("Initial {} datastore data: {}", datastoreType, dataTreeString);
        } catch (DeserializationException | IOException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException(
                    String.format("Unable to set initial state of %s datastore from XML!", datastoreType), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    String.format("Interrupted while setting initial state of %s datastore from XML!",
                            datastoreType), e);
        }
    }

    @VisibleForTesting
    void saveDatastore(@NonNull File fileName, LogicalDatastoreType datastoreType) {
        final DOMDataTreeReadTransaction readTransaction =
            netconfDeviceServices.getDOMDataBroker().newReadOnlyTransaction();
        final Optional<NormalizedNode> response;
        try {
            response = readTransaction.read(datastoreType,
                YangInstanceIdentifier.of()).get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.error("Could not retrieve configuration datastore! ", e);
            return;
        }
        if (response.isPresent()) {
            final XmlNodeConverter converter = netconfDeviceServices.getXmlNodeConverter();
            try {
                final Writer writer = converter.serializeRpc(YangInstanceIdentifier.of(), response.get());
                LOG.info("data: {}", writer.toString());
                saveToFile(writer.toString(), fileName);
            } catch (SerializationException e) {
                LOG.error("Unable to serialize config datastore: ", e);
            }
        } else {
            LOG.warn("No configuration data found in the datastore. "
                + "Aborting save operation to prevent overwriting existing data.");
        }
    }

    private void saveToFile(String data, File fileName) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
            writer.write(data);
        } catch (IOException e) {
            LOG.error("Unable to write to file: ", e);
        }
    }

    @Override
    public NetconfDeviceServices getNetconfDeviceServices() {
        return netconfDeviceServices;
    }

    @Override
    public void close() throws Exception {
        if (configurationData != null && configurationData.exists()) {
            LOG.info("Saving datastore as {}", configurationData);
            saveDatastore(configurationData, LogicalDatastoreType.CONFIGURATION);
        }
        if (operationalData != null && operationalData.exists()) {
            LOG.info("Saving datastore as {}", operationalData);
            saveDatastore(operationalData, LogicalDatastoreType.OPERATIONAL);
        }
        LOG.info("shutting down Netconf device");
        netConfDeviceSimulator.close();
    }

    /**
     * Method creates schemas from device's schema context and
     * stores them in the netconf-state/schemas path in operational datastore.
     * netconfDeviceServices represents device services from which
     * the modules loaded in schema-context will be converted to schemas
     * stored in netconf-state/schemas.
     * @return transaction commit information in FluentFuture
     */
    private FluentFuture<? extends CommitInfo> prepareSchemasForNetconfMonitoring() {
        HashMap<SchemaKey, Schema> mapSchemas = new HashMap<>();
        EffectiveModelContext modelContext =
            netconfDeviceServices.getAdapterContext()
                .currentSerializer().getRuntimeContext().modelContext();
        Queue<Collection<? extends ModuleLike>> queueModulesCollections = new LinkedList<>();
        queueModulesCollections.add(modelContext.getModules());
        while (!queueModulesCollections.isEmpty()) {
            Collection<? extends ModuleLike> modules = queueModulesCollections.poll();
            for (ModuleLike module : modules) {
                Schema schema = createSchemaFromModule(module);
                if (!mapSchemas.containsKey(schema.key())) {
                    mapSchemas.put(schema.key(), schema);
                    if (!module.getSubmodules().isEmpty()) {
                        queueModulesCollections.add(module.getSubmodules());
                    }
                }
            }
        }
        WriteTransaction writeTx = netconfDeviceServices.getDataBroker().newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL,
            DataObjectIdentifier.builder(NetconfState.class).child(Schemas.class).build(),
            new SchemasBuilder().setSchema(mapSchemas).build());
        return writeTx.commit();
    }

    /**
     * Creates and returns schema from given module.
     * @param module represents module from which schema will be created
     * @return schema created from module parameter
     */
    private Schema createSchemaFromModule(ModuleLike module) {
        return new SchemaBuilder()
            .setNamespace(new Uri(module.getNamespace().toString()))
            .setFormat(Yang.VALUE)
            .setIdentifier(module.getName())
            .setVersion(module.getRevision().map(Revision::toString).orElse(""))
            .setLocation(Collections.singleton(new Schema.Location(Schema.Location.Enumeration.NETCONF)))
            .build();
    }

    private boolean isNotEmpty(File inputStream) {
        try {
            boolean available = inputStream.toURI().toURL().openStream().available() > 0;
            if (!available) {
                LOG.warn("The provided initial datastore is empty!");
            }
            return available;
        } catch (IOException e) {
            LOG.warn("Unable to read datastore input: ", e);
            return false;
        }
    }

}
