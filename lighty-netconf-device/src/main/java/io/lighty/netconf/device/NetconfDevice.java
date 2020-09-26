/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device;

/**
 * This interface represents NETCONF device.
 */
public interface NetconfDevice extends AutoCloseable {

    /**
     * Starts NETCONF device. This action will start listening on TCP socket for incoming NETCONF traffic.
     */
    void start();

    /**
     * Provides configured services for this NETCONF device instance.
     */
    NetconfDeviceServices getNetconfDeviceServices();

}
