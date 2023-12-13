/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.topology.rpcs;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev231024.ConnectionOper.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev231024.connection.oper.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev231024.connection.oper.UnavailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev231024.connection.oper.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev231024.connection.oper.available.capabilities.AvailableCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev221225.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev221225.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.AddNodeIntoTopologyInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.AddNodeIntoTopologyOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.AddNodeIntoTopologyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.CreateTopologyInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.CreateTopologyOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.CreateTopologyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetNodeFromTopologyByIdInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetNodeFromTopologyByIdOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetNodeFromTopologyByIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologiesInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologiesOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologiesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologyByIdInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologyByIdOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologyByIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologyIdsInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologyIdsOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.GetTopologyIdsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.NetworkTopologyRpcsService;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveAllTopologiesInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveAllTopologiesOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveAllTopologiesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveNodeFromTopologyInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveNodeFromTopologyOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveNodeFromTopologyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveTopologyInput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveTopologyOutput;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.RemoveTopologyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.get.topologies.output.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.node.data.Node;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.node.data.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.node.data.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs.rev230927.topology.data.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetworkTopologyServiceImpl implements NetworkTopologyRpcsService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyServiceImpl.class);

    private final ExecutorService executor;
    private DataBroker dataBrokerService;
    private EffectiveModelContext effectiveModelContext;

    public NetworkTopologyServiceImpl() {
        this.executor = Executors.newFixedThreadPool(1);
    }

    @Override
    public void close() {
        this.executor.shutdown();
    }

    public void setDataBrokerService(final DataBroker dataBrokerService) {
        this.dataBrokerService = dataBrokerService;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<AddNodeIntoTopologyOutput>> addNodeIntoTopology(
            final AddNodeIntoTopologyInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);
        LOG.info("Adding node to topology {}", input.getTopologyId());
        final SettableFuture<RpcResult<AddNodeIntoTopologyOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<AddNodeIntoTopologyOutput>>() {
            @Override
            public RpcResult<AddNodeIntoTopologyOutput> call() throws Exception {
                final WriteTransaction writeTxConfig =
                        NetworkTopologyServiceImpl.this.dataBrokerService.newWriteOnlyTransaction();
                final WriteTransaction writeTxOper =
                        NetworkTopologyServiceImpl.this.dataBrokerService.newWriteOnlyTransaction();
                final TopologyId topologyId = input.getTopologyId();
                final Collection<Node> nodeCollection = input.nonnullNode().values();

                final Map<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network
                        .topology.topology.NodeKey, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network
                        .topology.rev131021.network.topology.topology.Node> nodeConfigMap = new HashMap<>();
                final Map<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network
                        .topology.topology.NodeKey, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network
                        .topology.rev131021.network.topology.topology.Node> nodeOperMap = new HashMap<>();
                for (final Node node : nodeCollection) {

                    NetconfNode netconfNode = new NetconfNodeBuilder()
                            .setHost(node.getHost())
                            .setPort(node.getPort())
                            .setCredentials(node.getCredentials())
                            .setKeepaliveDelay(node.getKeepaliveDelay())
                            .setSchemaless(node.getSchemaless())
                            .setTcpOnly(node.getTcpOnly())
                            .build();

                    final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                        .rev131021.network.topology.topology.Node nConfig =
                            createNetworkTopologyNode(node, netconfNode);

                    final List<AvailableCapability> availableCapabilities = new ArrayList<>();
                    for (final Module m : NetworkTopologyServiceImpl.this.effectiveModelContext.getModules()) {
                        final Revision revision = m.getRevision().orElse(Revision.of("2017-01-01"));
                        final AvailableCapability ac = new AvailableCapabilityBuilder()
                                .setCapabilityOrigin(AvailableCapability.CapabilityOrigin.DeviceAdvertised)
                                .setCapability(String.format("(%s?revision=%s)%s", m.getNamespace(), revision,
                                    m.getName())).build();
                        availableCapabilities.add(ac);
                    }
                    netconfNode = new NetconfNodeBuilder()
                            .setConnectionStatus(ConnectionStatus.Connected)
                            .setUnavailableCapabilities(new UnavailableCapabilitiesBuilder().build())
                            .setAvailableCapabilities(new AvailableCapabilitiesBuilder()
                                    .setAvailableCapability(availableCapabilities).build())
                            .setHost(node.getHost())
                            .setPort(node.getPort())
                            .build();
                    final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                        .rev131021.network.topology.topology.Node nOper =
                            createNetworkTopologyNode(node, netconfNode);
                    nodeConfigMap.put(nConfig.key() ,nConfig);
                    nodeOperMap.put(nOper.key() ,nOper);
                }

                Topology topology = new TopologyBuilder()
                        .setTopologyId(topologyId)
                        .setNode(nodeConfigMap)
                        .build();
                final InstanceIdentifier<Topology> tii =
                        InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                                topology.key()).build();
                writeTxConfig.merge(LogicalDatastoreType.CONFIGURATION, tii, topology);
                topology = new TopologyBuilder()
                        .setTopologyId(topologyId)
                        .setNode(nodeOperMap)
                        .build();
                writeTxOper.merge(LogicalDatastoreType.OPERATIONAL, tii, topology);
                writeTxConfig.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                writeTxOper.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                final AddNodeIntoTopologyOutput addNodeIntoTopologyOutput = new AddNodeIntoTopologyOutputBuilder()
                        .build();
                final RpcResult<AddNodeIntoTopologyOutput> rpcResult = RpcResultBuilder.success(
                        addNodeIntoTopologyOutput).build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<CreateTopologyOutput>> createTopology(final CreateTopologyInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);
        final SettableFuture<RpcResult<CreateTopologyOutput>> result = SettableFuture.create();
        LOG.info("Creating topology {}", input.getTopologyId());
        this.executor.submit(new Callable<RpcResult<CreateTopologyOutput>>() {
            @Override
            public RpcResult<CreateTopologyOutput> call() throws Exception {
                final WriteTransaction writeTxOper =
                    NetworkTopologyServiceImpl.this.dataBrokerService.newWriteOnlyTransaction();
                final WriteTransaction writeTxConfig =
                        NetworkTopologyServiceImpl.this.dataBrokerService.newWriteOnlyTransaction();
                final Topology topology = new TopologyBuilder().setTopologyId(input.getTopologyId()).build();
                final InstanceIdentifier<Topology> tii =
                        InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                                topology.key()).build();
                writeTxConfig.merge(LogicalDatastoreType.CONFIGURATION, tii, topology);
                writeTxOper.merge(LogicalDatastoreType.OPERATIONAL, tii, topology);
                writeTxConfig.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                writeTxOper.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                final CreateTopologyOutput topologyOutput = new CreateTopologyOutputBuilder().build();

                final RpcResult<CreateTopologyOutput> rpcResult = RpcResultBuilder.success(topologyOutput).build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<RemoveTopologyOutput>> removeTopology(final RemoveTopologyInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);

        final SettableFuture<RpcResult<RemoveTopologyOutput>> result = SettableFuture.create();
        LOG.info("Removing topology {}", input.getTopologyId());
        this.executor.submit(new Callable<RpcResult<RemoveTopologyOutput>>() {
            @Override
            public RpcResult<RemoveTopologyOutput> call() throws Exception {
                final Topology topology = new TopologyBuilder().setTopologyId(input.getTopologyId()).build();
                final InstanceIdentifier<Topology> tii =
                        InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class, topology.key())
                        .build();
                removeFromDatastore(tii);
                final RemoveTopologyOutput topologyOutput = new RemoveTopologyOutputBuilder().build();
                final RpcResult<RemoveTopologyOutput> rpcResult = RpcResultBuilder.success(topologyOutput).build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<RemoveNodeFromTopologyOutput>> removeNodeFromTopology(
            final RemoveNodeFromTopologyInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);

        LOG.info("Removing node {} from topology {}", input.getNodeId(), input.getTopologyId());
        final SettableFuture<RpcResult<RemoveNodeFromTopologyOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<RemoveNodeFromTopologyOutput>>() {
            @Override
            public RpcResult<RemoveNodeFromTopologyOutput> call() throws Exception {
                final Topology topology = new TopologyBuilder().setTopologyId(input.getTopologyId()).build();
                final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                    .rev131021.network.topology.topology.NodeKey nk =
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                            .rev131021.network.topology.topology.NodeKey(input.getNodeId());

                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                    .rev131021.network.topology.topology.Node> nii =
                        InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class, topology.key())
                        .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                            .rev131021.network.topology.topology.Node.class, nk)
                        .build();
                removeFromDatastore(nii);
                final RemoveNodeFromTopologyOutput topologyOutput = new RemoveNodeFromTopologyOutputBuilder().build();
                final RpcResult<RemoveNodeFromTopologyOutput> rpcResult = RpcResultBuilder.success(topologyOutput)
                        .build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<RemoveAllTopologiesOutput>> removeAllTopologies(
            final RemoveAllTopologiesInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);

        LOG.info("Removing whole topology");
        final SettableFuture<RpcResult<RemoveAllTopologiesOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<RemoveAllTopologiesOutput>>() {
            @Override
            public RpcResult<RemoveAllTopologiesOutput> call() throws Exception {
                final InstanceIdentifier<NetworkTopology> ntii =
                        InstanceIdentifier.builder(NetworkTopology.class).build();
                removeFromDatastore(ntii);
                final RemoveAllTopologiesOutput topologiesOutput = new RemoveAllTopologiesOutputBuilder().build();
                final RpcResult<RemoveAllTopologiesOutput> rpcResult = RpcResultBuilder.success(topologiesOutput)
                        .build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<GetTopologyByIdOutput>> getTopologyById(final GetTopologyByIdInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);

        LOG.info("Searching for topology {}", input.getTopologyId());
        final SettableFuture<RpcResult<GetTopologyByIdOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<GetTopologyByIdOutput>>() {
            @Override
            public RpcResult<GetTopologyByIdOutput> call() throws Exception {
                try (ReadTransaction readTx =
                             NetworkTopologyServiceImpl.this.dataBrokerService.newReadOnlyTransaction()) {
                    final Topology topology = new TopologyBuilder().setTopologyId(input.getTopologyId()).build();
                    final InstanceIdentifier<Topology> tii =
                            InstanceIdentifier.builder(NetworkTopology.class)
                                    .child(Topology.class, topology.key())
                                    .build();
                    final Optional<Topology> readTopology = readTx.read(LogicalDatastoreType.CONFIGURATION, tii)
                            .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    final Map<org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs
                            .rev230927.topology.data.TopologyKey, org.opendaylight.yang.gen.v1.urn.tech.pantheon
                            .netconfdevice.network.topology.rpcs.rev230927.topology.data.Topology> finalTopology
                            = new HashMap<>();
                    if (readTopology.isPresent()) {
                        final Topology t = readTopology.get();
                        final Map<NodeKey, Node> finalNodeMap = new HashMap<>();
                        if (t.getNode() != null) {
                            for (final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                                    .rev131021.network.topology.topology.Node node : t.nonnullNode().values()) {
                                final NodeKey nk2 = new NodeKey(node.getNodeId());
                                final Node nb = buildNode(nk2, node);
                                finalNodeMap.put(nb.key(), nb);
                            }
                        }
                        final TopologyKey topologyKey = new TopologyKey(t.getTopologyId());
                        finalTopology.put(topologyKey, new org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice
                                .network.topology.rpcs.rev230927.topology.data.TopologyBuilder()
                                .setTopologyId(new TopologyId(t.getTopologyId()))
                                .withKey(topologyKey)
                                .setNode(finalNodeMap)
                                .build());
                    }
                    final GetTopologyByIdOutput getTopologyByIdOutput = new GetTopologyByIdOutputBuilder().setTopology(
                            finalTopology).build();
                    final RpcResult<GetTopologyByIdOutput> rpcResult = RpcResultBuilder.success(getTopologyByIdOutput)
                            .build();
                    result.set(rpcResult);
                    return rpcResult;
                }


            }
        });
        return result;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<GetTopologyIdsOutput>> getTopologyIds(final GetTopologyIdsInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);

        LOG.info("Searching for list of topologies");
        final SettableFuture<RpcResult<GetTopologyIdsOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<GetTopologyIdsOutput>>() {
            @Override
            public RpcResult<GetTopologyIdsOutput> call() throws Exception {
                final GetTopologyIdsOutput topologyIdsOutput = new GetTopologyIdsOutputBuilder().setTopologyIds(
                        prepareGetTopologyIds()).build();
                final RpcResult<GetTopologyIdsOutput> rpcResult = RpcResultBuilder.success(topologyIdsOutput).build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
        return result;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<GetNodeFromTopologyByIdOutput>> getNodeFromTopologyById(
            final GetNodeFromTopologyByIdInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);
        LOG.info("Searching node {} on topology {}", input.getNodeId(), input.getTopologyId());
        final SettableFuture<RpcResult<GetNodeFromTopologyByIdOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<GetNodeFromTopologyByIdOutput>>() {
            @Override
            public RpcResult<GetNodeFromTopologyByIdOutput> call() throws Exception {
                try (ReadTransaction readTx =
                             NetworkTopologyServiceImpl.this.dataBrokerService.newReadOnlyTransaction()) {
                    LogicalDatastoreType datastoreType;
                    if (input.getIsConfig()) {
                        datastoreType = LogicalDatastoreType.CONFIGURATION;
                    } else {
                        datastoreType = LogicalDatastoreType.OPERATIONAL;
                    }
                    final Topology topology = new TopologyBuilder().setTopologyId(input.getTopologyId()).build();
                    final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                            .rev131021.network.topology.topology.NodeKey nk =
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                                    .rev131021.network.topology.topology.NodeKey(input.getNodeId());

                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                            .rev131021.network.topology.topology.Node> tii =
                            InstanceIdentifier.builder(NetworkTopology.class)
                                    .child(Topology.class, topology.key())
                                    .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                                            .rev131021.network.topology.topology.Node.class, nk)
                                    .build();
                    final Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                            .rev131021.network.topology.topology.Node> nodeOptional =
                            readTx.read(datastoreType, tii).get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    if (nodeOptional.isPresent()) {
                        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                                .rev131021.network.topology.topology.Node node = nodeOptional.get();
                        final Map<NodeKey, Node> finalNodeMap = new HashMap<>();
                        final NodeKey nk2 = new NodeKey(node.getNodeId());
                        final NodeBuilder build = new NodeBuilder()
                                .withKey(nk2);
                        build.setNodeId(node.getNodeId());
                        final NetconfNode augmentation = node.augmentation(NetconfNode.class);
                        if (augmentation != null) {
                            build.setHost(augmentation.getHost())
                                    .setPort(augmentation.getPort())
                                    .setCredentials(augmentation.getCredentials())
                                    .setTcpOnly(augmentation.getTcpOnly())
                                    .setConnectionStatus(augmentation.getConnectionStatus())
                                    .setUnavailableCapabilities(augmentation.getUnavailableCapabilities())
                                    .setAvailableCapabilities(augmentation.getAvailableCapabilities());
                            if (input.getIsConfig()) {
                                build.setSchemaless(augmentation.getSchemaless());
                            }
                        }

                        final Node nb = build.build();
                        finalNodeMap.put(nb.key(), nb);

                        final GetNodeFromTopologyByIdOutput nodeFromTopologyByIdOutput =
                                new GetNodeFromTopologyByIdOutputBuilder().setNode(finalNodeMap).build();
                        final RpcResult<GetNodeFromTopologyByIdOutput> rpcResult = RpcResultBuilder.success(
                                nodeFromTopologyByIdOutput).build();
                        result.set(rpcResult);
                        return rpcResult;
                    } else {
                        final GetNodeFromTopologyByIdOutput getNodeFromTopologyByIdOutput =
                                new GetNodeFromTopologyByIdOutputBuilder().build();
                        final RpcResult<GetNodeFromTopologyByIdOutput> rpcResult = RpcResultBuilder.success(
                                getNodeFromTopologyByIdOutput).build();
                        result.set(rpcResult);
                        return rpcResult;
                    }
                }
            }
        });
        return result;
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<GetTopologiesOutput>> getTopologies(final GetTopologiesInput input) {
        Preconditions.checkNotNull(this.dataBrokerService);
        LOG.info("Searching data for all topologies");
        final SettableFuture<RpcResult<GetTopologiesOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<GetTopologiesOutput>>() {
            @Override
            public RpcResult<GetTopologiesOutput> call() throws Exception {
                try (ReadTransaction readTx =
                             NetworkTopologyServiceImpl.this.dataBrokerService.newReadOnlyTransaction()) {
                    final InstanceIdentifier<NetworkTopology> tii =
                            InstanceIdentifier.builder(NetworkTopology.class).build();
                    final Optional<NetworkTopology> networkTopology =
                            readTx.read(LogicalDatastoreType.CONFIGURATION, tii)
                            .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    if (networkTopology.isPresent()) {
                        final org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs
                                .rev230927.topology.data.TopologyBuilder topologyBuilder =
                                new org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs
                                        .rev230927.topology.data.TopologyBuilder();
                        final Collection<Topology> topologyList = networkTopology.get().nonnullTopology().values();
                        final Map<org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs
                                .rev230927.topology.data.TopologyKey, org.opendaylight.yang.gen.v1.urn.tech.pantheon
                                .netconfdevice.network.topology.rpcs.rev230927.topology.data.Topology> topologyMapFinal
                                = new HashMap<>();
                        for (final Topology t : topologyList) {
                            final Map<NodeKey, Node> nodeMap = new HashMap<>();
                            if (t.getNode() != null) {
                                for (final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                                        .rev131021.network.topology.topology.Node n : t.nonnullNode().values()) {
                                    final NodeKey nk = new NodeKey(n.getNodeId());
                                    final Node nb = buildNode(nk, n);
                                    nodeMap.put(nb.key(), nb);
                                }
                            }

                            final org.opendaylight.yang.gen.v1.urn.tech.pantheon.netconfdevice.network.topology.rpcs
                                    .rev230927.topology.data.Topology tp =
                                    topologyBuilder.withKey(new TopologyKey(t.getTopologyId()))
                                            .setTopologyId(t.getTopologyId())
                                            .setNode(nodeMap)
                                            .build();
                            topologyMapFinal.put(tp.key(), tp);
                        }

                        final GetTopologiesOutput getTopologiesOutput = new GetTopologiesOutputBuilder()
                                .setNetworkTopology(new NetworkTopologyBuilder().setTopology(topologyMapFinal)
                                        .build()).build();
                        final RpcResult<GetTopologiesOutput> rpcResult = RpcResultBuilder.success(getTopologiesOutput)
                                .build();
                        result.set(rpcResult);
                        return rpcResult;
                    }
                    final RpcResult<GetTopologiesOutput> rpcResult = RpcResultBuilder.success(
                            new GetTopologiesOutputBuilder().build()).build();
                    result.set(rpcResult);
                    return rpcResult;
                }
            }
        });
        return result;
    }

    public void setEffectiveModelContext(final EffectiveModelContext effectiveModelContext) {
        this.effectiveModelContext = effectiveModelContext;
    }

    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
        .rev131021.network.topology.topology.Node createNetworkTopologyNode(
            final Node node, final NetconfNode ncNode) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
            .rev131021.network.topology.topology.NodeKey nk =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                    .network.topology.topology.NodeKey(node.getNodeId());

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
            .network.topology.topology.NodeBuilder()
                .withKey(nk)
                .setNodeId(node.getNodeId())
                .addAugmentation(ncNode)
                .build();
    }

    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private void removeFromDatastore(final InstanceIdentifier<?> instanceIdentifier)
            throws ExecutionException, InterruptedException, TimeoutException {
        final WriteTransaction writeTxConfig = this.dataBrokerService.newWriteOnlyTransaction();
        final WriteTransaction writeTxOper = this.dataBrokerService.newWriteOnlyTransaction();
        writeTxConfig.delete(LogicalDatastoreType.CONFIGURATION, instanceIdentifier);
        writeTxOper.delete(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
        writeTxConfig.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        writeTxOper.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Set<TopologyId> prepareGetTopologyIds() throws ExecutionException, InterruptedException, TimeoutException {
        try (ReadTransaction readTx = this.dataBrokerService.newReadOnlyTransaction()) {
            final InstanceIdentifier<NetworkTopology> ntii =
                    InstanceIdentifier.builder(NetworkTopology.class)
                            .build();
            final Optional<NetworkTopology> networkTopology = readTx.read(LogicalDatastoreType.CONFIGURATION, ntii)
                    .get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final Set<TopologyId> topologyIds = new HashSet<>();
            if (networkTopology.isPresent()) {
                for (final Topology topology : networkTopology.get().nonnullTopology().values()) {
                    topologyIds.add(topology.getTopologyId());
                }
            }
            return topologyIds;
        }
    }

    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Node buildNode(final NodeKey key,
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
            .rev131021.network.topology.topology.Node node) {
        final NodeBuilder build = new NodeBuilder().withKey(key);
        if (node != null) {
            build.setNodeId(node.getNodeId());
            final NetconfNode augmentation = node.augmentation(NetconfNode.class);
            if (augmentation != null) {
                build.setHost(augmentation.getHost())
                    .setPort(augmentation.getPort())
                    .setCredentials(augmentation.getCredentials())
                    .setTcpOnly(augmentation.getTcpOnly())
                    .setSchemaless(augmentation.getSchemaless());
            }
        }
        return build.build();
    }

}
