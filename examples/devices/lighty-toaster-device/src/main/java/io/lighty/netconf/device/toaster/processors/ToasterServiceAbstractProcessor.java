/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster.processors;

import io.lighty.codecs.util.XmlNodeConverter;
import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.netconf.device.NetconfDeviceServices;
import io.lighty.netconf.device.requests.RpcOutputRequestProcessor;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.response.ResponseData;
import io.lighty.netconf.device.utils.RPCUtil;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.transform.TransformerException;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.CurrentAdapterSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public abstract class ToasterServiceAbstractProcessor<I extends DataObject, O extends DataObject> extends
        RpcOutputRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterServiceAbstractProcessor.class);
    private CurrentAdapterSerializer adapterSerializer;

    @Override
    public void init(final NetconfDeviceServices netconfDeviceServices) {
        super.init(netconfDeviceServices);
        final ConstantAdapterContext constantAdapterContext
                = new ConstantAdapterContext(netconfDeviceServices.getAdapterContext().currentSerializer());
        this.adapterSerializer = constantAdapterContext.currentSerializer();
    }

    @Override
    protected CompletableFuture<Response> execute(final Element requestXmlElement) {
        //1. convert XML input into NormalizedNode
        try (Reader readerFromElement = RPCUtil.createReaderFromElement(requestXmlElement);) {
            final XmlNodeConverter xmlNodeConverter = getNetconfDeviceServices().getXmlNodeConverter();

            final NormalizedNode deserializedNode = xmlNodeConverter.deserialize(
                    Absolute.of(getRpcDefinition().getQName(), getRpcDefinition().getInput().getQName()),
                    readerFromElement);

            //2. convert NormalizedNode into RPC input
            final I input = convertToBindingAwareRpc(getRpcDefInputAbsolutePath(), (ContainerNode) deserializedNode);

            //3. invoke RPC and wait for completion
            final Future<RpcResult<O>> invokeRpc = execMethod(input);
            final RpcResult<O> rpcResult = invokeRpc.get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            //4. convert RPC output to ContainerNode
            final ContainerNode data = this.adapterSerializer.toNormalizedNodeRpcData(rpcResult.getResult());

            //5. create response
            return CompletableFuture.completedFuture(new ResponseData(Collections.singletonList(data)));
        } catch (final ExecutionException | DeserializationException | TransformerException
                | TimeoutException | IOException e) {
            LOG.error("Error while executing RPC", e);
            return CompletableFuture.failedFuture(e);
        } catch (final InterruptedException e) {
            LOG.error("Interrupted while executing RPC", e);
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }

    protected abstract Future<RpcResult<O>> execMethod(I input);

    @SuppressWarnings("unchecked")
    public I convertToBindingAwareRpc(final Absolute absolute, final ContainerNode rpcData) {
        return (I) this.adapterSerializer.fromNormalizedNodeRpcData(absolute, rpcData);
    }
}
