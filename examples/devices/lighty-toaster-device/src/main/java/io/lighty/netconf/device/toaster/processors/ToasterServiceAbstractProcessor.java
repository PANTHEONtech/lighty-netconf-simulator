/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.toaster.processors;

import io.lighty.codecs.DataCodec;
import io.lighty.codecs.XmlNodeConverter;
import io.lighty.codecs.api.SerializationException;
import io.lighty.netconf.device.NetconfDeviceServices;
import io.lighty.netconf.device.requests.RpcOutputRequestProcessor;
import io.lighty.netconf.device.response.Response;
import io.lighty.netconf.device.response.ResponseData;
import io.lighty.netconf.device.utils.RPCUtil;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.io.Reader;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.transform.TransformerException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public abstract class ToasterServiceAbstractProcessor<I extends DataObject, O extends DataObject> extends
        RpcOutputRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterServiceAbstractProcessor.class);
    private DataCodec<I> dataCodec;

    @Override
    public void init(final NetconfDeviceServices netconfDeviceServices) {
        super.init(netconfDeviceServices);
        this.dataCodec = new DataCodec<I>(netconfDeviceServices.getSchemaContext());
    }

    @Override
    protected CompletableFuture<Response> execute(final Element requestXmlElement) {
        try {
            final XmlNodeConverter xmlNodeConverter = getNetconfDeviceServices().getXmlNodeConverter();

            //1. convert XML input into NormalizedNode<?, ?>
            final Reader readerFromElement = RPCUtil.createReaderFromElement(requestXmlElement);
            final NormalizedNode<?, ?> deserializedNode =
                    xmlNodeConverter.deserialize(getRpcDefinition().getInput(), readerFromElement);

            //2. convert NormalizedNode<?, ?> into RPC input
            final I input = this.dataCodec.convertToBindingAwareRpc(getRpcDefinition().getInput().getPath(),
                    (ContainerNode) deserializedNode);

            //3. invoke RPC and wait for completion
            final Future<RpcResult<O>> invokeRpc = execMethod(input);
            final RpcResult<O> rpcResult = invokeRpc.get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            //4. convert RPC output to ContainerNode
            final ContainerNode data = this.dataCodec.convertToBindingIndependentRpc(rpcResult.getResult());

            //5. create response
            return CompletableFuture.completedFuture(new ResponseData(Collections.singletonList(data)));
        } catch (final InterruptedException | ExecutionException | SerializationException | TransformerException
                | TimeoutException e) {
            LOG.error("Error while executing RPC", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    protected abstract Future<RpcResult<O>> execMethod(I input);

}
