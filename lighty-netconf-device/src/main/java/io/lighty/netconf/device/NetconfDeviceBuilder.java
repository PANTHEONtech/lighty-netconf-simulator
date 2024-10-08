/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device;

import io.lighty.netconf.device.requests.CommitRequestProcessor;
import io.lighty.netconf.device.requests.DeleteConfigRequestProcessor;
import io.lighty.netconf.device.requests.EditConfigRequestProcessor;
import io.lighty.netconf.device.requests.GetConfigRequestProcessor;
import io.lighty.netconf.device.requests.GetRequestProcessor;
import io.lighty.netconf.device.requests.RequestProcessor;
import io.lighty.netconf.device.requests.notification.CreateSubscriptionRequestProcessor;
import io.lighty.netconf.device.requests.notification.NotificationPublishServiceImpl;
import io.lighty.netconf.device.utils.ModelUtils;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.netconf.auth.AuthProvider;
import org.opendaylight.netconf.shaded.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.opendaylight.netconf.test.tool.config.ConfigurationBuilder;
import org.opendaylight.netconf.test.tool.rpchandler.RpcHandler;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;

public class NetconfDeviceBuilder {

    private Set<YangModuleInfo> moduleInfos;
    private ConfigurationBuilder configurationBuilder;
    private InputStream initialOperationalData;
    private InputStream initialConfigurationData;
    private Map<QName, RequestProcessor> requestProcessors;
    private Set<String> allCapabilities;
    private NotificationPublishServiceImpl creator;
    private boolean netconfMonitoringEnabled;

    public NetconfDeviceBuilder() {
        this.configurationBuilder = new ConfigurationBuilder();
        this.requestProcessors = new HashMap<>();
        this.moduleInfos = new HashSet<>();
        this.allCapabilities = new HashSet<>();
        this.netconfMonitoringEnabled = true;
    }

    public NetconfDeviceBuilder setCredentials(String userName, String password) {
        this.configurationBuilder
                .setAuthProvider((username, passwd) -> userName.equals(username) && password.equals(passwd));
        this.configurationBuilder
                .setPublickeyAuthenticator((username, key, session) -> false);
        return this;
    }

    public NetconfDeviceBuilder setAuthProvider(AuthProvider authProvider) {
        this.configurationBuilder.setAuthProvider(authProvider);
        return this;
    }

    public NetconfDeviceBuilder setPublickeyAuthenticator(PublickeyAuthenticator publickeyAuthenticator) {
        this.configurationBuilder.setPublickeyAuthenticator(publickeyAuthenticator);
        return this;
    }

    public NetconfDeviceBuilder setInitialOperationalData(InputStream initialOperationalData) {
        this.initialOperationalData = initialOperationalData;
        return this;
    }

    public NetconfDeviceBuilder setInitialConfigurationData(InputStream initialConfigurationData) {
        this.initialConfigurationData = initialConfigurationData;
        return this;
    }

    public NetconfDeviceBuilder withCapabilities(Set<String> capabilities) {
        this.allCapabilities.addAll(capabilities);
        return this;
    }

    public NetconfDeviceBuilder withModels(Set<YangModuleInfo> paramModuleInfos) {
        this.moduleInfos.addAll(paramModuleInfos);
        return this;
    }

    public NetconfDeviceBuilder withDefaultCapabilities() {
        this.allCapabilities.addAll(ModelUtils.DEFAULT_CAPABILITIES);
        return this;
    }

    public NetconfDeviceBuilder withDefaultRequestProcessors() {
        GetRequestProcessor getRequestProcessor = new GetRequestProcessor();
        this.requestProcessors.put(getRequestProcessor.getIdentifier(), getRequestProcessor);

        GetConfigRequestProcessor getConfigRequestProcessor = new GetConfigRequestProcessor();
        this.requestProcessors.put(getConfigRequestProcessor.getIdentifier(), getConfigRequestProcessor);

        EditConfigRequestProcessor editConfigRequestProcessor = new EditConfigRequestProcessor();
        this.requestProcessors.put(editConfigRequestProcessor.getIdentifier(), editConfigRequestProcessor);

        CommitRequestProcessor commitRequestProcessor = new CommitRequestProcessor();
        this.requestProcessors.put(commitRequestProcessor.getIdentifier(), commitRequestProcessor);

        DeleteConfigRequestProcessor deleteConfigRequestProcessor = new DeleteConfigRequestProcessor();
        this.requestProcessors.put(deleteConfigRequestProcessor.getIdentifier(), deleteConfigRequestProcessor);

        return this;
    }

    public NetconfDeviceBuilder withRequestProcessor(RequestProcessor requestProcessor) {
        this.requestProcessors.put(requestProcessor.getIdentifier(), requestProcessor);
        return this;
    }

    public NetconfDeviceBuilder withRequestProcessors(
        Map<QName, RequestProcessor> paramRequestProcessors) {
        this.requestProcessors.putAll(paramRequestProcessors);
        return this;
    }

    public NetconfDeviceBuilder withRpcHandler(RpcHandler rpcHandler) {
        this.configurationBuilder.setRpcMapping(rpcHandler);
        return this;
    }

    public NetconfDeviceBuilder withDefaultNotificationProcessor() {
        this.allCapabilities.add(ModelUtils.DEFAULT_NOTIFICATION_CAPABILITY);
        //TODO This model shouldn't be needed anymore and should be provided internally
        // based on the notification capability alone, but it turned it's still needed.
        //  Created issue for it https://jira.opendaylight.org/browse/NETCONF-754 so keep checking on it.
        YangModuleInfo netconfNotificationModel =
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netconf
                        .notification._1._0.rev080714.YangModuleInfoImpl.getInstance();
        this.moduleInfos.add(netconfNotificationModel);
        this.withRequestProcessor(new CreateSubscriptionRequestProcessor());
        this.creator = new NotificationPublishServiceImpl();
        return this;
    }

    /**
     * Method sets netconfMonitoringEnabled flag to parameter enabled value
     * which indicates if netconf-monitoring for the device will be enabled
     * or disabled.
     * @param enabled specifies if netconf-monitoring should be enabled or not
     * @return this Builder
     */
    public NetconfDeviceBuilder withNetconfMonitoringEnabled(boolean enabled) {
        this.netconfMonitoringEnabled = enabled;
        return this;
    }

    public NetconfDeviceBuilder setBindingPort(int port) {
        this.configurationBuilder.setStartingPort(port);
        return this;
    }

    // FIXME All created devices share the same datastore. Each simulated devices must have a separate datastore space.
    public NetconfDeviceBuilder setDeviceCount(int deviceCount) {
        this.configurationBuilder.setDeviceCount(deviceCount);
        return this;
    }

    public NetconfDeviceBuilder setThreadPoolSize(int threadPoolSize) {
        this.configurationBuilder.setThreadPoolSize(threadPoolSize);
        return this;
    }

    /**
     * Generates new {@link NetconfDevice} instance based on specified builder attributes.
     * If netconf-monitoring flag was set to enabled,
     * netconf-monitoring YANG model will be loaded along with specified models.
     * @return new implementation of NetconfDevice
     */
    public NetconfDevice build() {
        this.configurationBuilder.setCapabilities(this.allCapabilities);
        if (netconfMonitoringEnabled) {
            YangModuleInfo netconfMonitoringModule =
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring
                    .rev101004.YangModuleInfoImpl.getInstance();
            this.moduleInfos.add(netconfMonitoringModule);
        }
        this.configurationBuilder.setGetDefaultYangResources(Collections.emptySet());
        this.configurationBuilder.setModels(moduleInfos);
        return new NetconfDeviceImpl(moduleInfos, configurationBuilder.build(),
            initialOperationalData, initialConfigurationData, requestProcessors, creator,
            netconfMonitoringEnabled);
    }

}
