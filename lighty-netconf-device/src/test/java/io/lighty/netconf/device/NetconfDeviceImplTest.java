/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device;

import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.core.common.models.ModuleId;
import io.lighty.netconf.device.utils.ModelUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.testng.Assert;

public class NetconfDeviceImplTest {

    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;
    private static NetconfDeviceImpl netconfDevice;

    @BeforeAll
    public static void setUp() {
        //set up a simple device
        final Set<YangModuleInfo> modules = ModelUtils.getModelsFromClasspath(
            ModuleId.from("urn:tech.pantheon.netconfdevice.network.topology.rpcs",
                "network-topology-rpcs",
                "2023-09-27"),
            ModuleId.from("urn:opendaylight:netconf-node-topology",
                "netconf-node-topology",
                "2023-11-21"),
            ModuleId.from("urn:TBD:params:xml:ns:yang:network-topology",
                "network-topology",
                "2013-10-21"));

        netconfDevice = (NetconfDeviceImpl) new NetconfDeviceBuilder()
            .setCredentials("admin", "admin")
            .setBindingPort(17830)
            .withModels(modules)
            .build();
        netconfDevice.start();
    }

    @AfterAll
    public static void cleanUp() throws Exception {
        netconfDevice.close();
    }

    @Test
    public void testInitDatastore() throws ExecutionException, InterruptedException, TimeoutException {
        //get the file as stream
        final File file = new File(
            NetconfDeviceImplTest.class.getResource("/initial-network-topo-config-datastore.xml").getFile());
        //use the stream to init datastore
        netconfDevice.initDatastore(LogicalDatastoreType.CONFIGURATION,
            file);
        //create identifier
        final Topology topology = new TopologyBuilder().setTopologyId(new TopologyId("default-topology")).build();
        final InstanceIdentifier<Topology> tii =
            InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topology.key())
                .build();
        //read the datastore using the identifier from the simulator
        final Topology response = netconfDevice.getNetconfDeviceServices().getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tii).get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).get();
        //check if the simulator contains the initial datastore
        Assert.assertEquals(response.getNode().values().size(), 2);
        Assert.assertTrue(response.getNode().values().contains(
            new NodeBuilder().setNodeId(new NodeId("new-netconf-device")).build()));
        Assert.assertTrue(response.getNode().values().contains(
            new NodeBuilder().setNodeId(new NodeId("new-netconf-device-1")).build()));
    }

    @Test
    public void testSaveDatastore() throws ExecutionException, InterruptedException,
        TimeoutException, DeserializationException {
        //create identifier
        final Topology topology = new TopologyBuilder().setTopologyId(new TopologyId("default-topology")).build();
        final InstanceIdentifier<Topology> tii =
            InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topology.key())
                .build();
        //write a new topology to device using the identifier
        final WriteTransaction writeTransaction =
            netconfDevice.getNetconfDeviceServices().getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, tii,
            new TopologyBuilder().setTopologyId(new TopologyId("default-topology")).build());
        writeTransaction.commit().get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        //save the datastore to temporary file called saved-datastore.xml
        netconfDevice.saveDatastore(
            new File("./src/test/resources/saved-datastore.xml"), LogicalDatastoreType.CONFIGURATION);
        //verify that the file exists and contains the topology
        final File file = new File("./src/test/resources/saved-datastore.xml");
        Assert.assertTrue(file.exists());
        try (InputStream stream = new FileInputStream(file)) {
            Assert.assertNotNull(stream);
            final Reader reader = new InputStreamReader(stream, Charset.defaultCharset());
            final NormalizedNode initialDataBI = netconfDevice.getNetconfDeviceServices().getXmlNodeConverter()
                .deserialize(netconfDevice.getNetconfDeviceServices().getRootInference(), reader);
            final String dataTreeString = NormalizedNodes.toStringTree(initialDataBI);
            Assert.assertTrue(dataTreeString.contains("default-topology"));
            Assert.assertTrue(dataTreeString.contains("default-topology"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            //delete the temporary file
            file.delete();
        }
    }
}
