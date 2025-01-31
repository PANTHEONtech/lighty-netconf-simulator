/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device.utils;

import com.google.common.base.Preconditions;
import java.io.File;
import java.util.List;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Namespace;

public class ArgumentParser {

    public static final int DEFAULT_PORT = 17830;
    public static final int DEFAULT_DEVICE_COUNT = 1;
    public static final int DEFAULT_POOL_SIZE = 8;

    private boolean initDatastore;
    private boolean saveDatastore;

    public Namespace parseArguments(final String[] args) {
        final net.sourceforge.argparse4j.inf.ArgumentParser argumentParser =
            ArgumentParsers.newFor("Lighty-netconf-simulator").build();

        argumentParser.addArgument("-p", "--port")
            .nargs(1)
            .setDefault(List.of(DEFAULT_PORT))
            .help("Sets port. If no value is set, default value is used (17830).");
        argumentParser.addArgument("-i", "--init-datastore")
            .nargs(1)
            .help("Set path for the initial datastore folder which will be loaded. Folder must include two files "
                + "named initial-network-topo-config-datastore.xml and initial-network-topo-operational-datastore.xml");
        argumentParser.addArgument("-o", "--output-datastore")
            .nargs(1)
            .help("Set path where the output datastore which will be saved.");
        argumentParser.addArgument("-d", "--devices-count")
            .setDefault(List.of(DEFAULT_DEVICE_COUNT))
            .help("Number of simulated netconf devices to spin."
                + " This is the number of actual ports which will be used for the devices.")
            .dest("devices-count");
        argumentParser.addArgument("-t", "--thread-pool-size")
            .setDefault(List.of(DEFAULT_POOL_SIZE))
            .help("The number of threads to keep in the pool, "
                + "when creating a device simulator, even if they are idle.")
            .dest("thread-pool-size");

        final Namespace namespace = argumentParser.parseArgsOrFail(args);
        if (!(namespace.getString("init_datastore") == null)) {
            initDatastore = true;
            final String pathDoesNotExist = "Input datastore %s does not exist";
            final String filename = namespace.getString("init_datastore").replaceAll("[\\[\\]]", "");
            final File file = new File(filename);
            Preconditions.checkArgument(file.exists(), String.format(pathDoesNotExist, filename));
        } else {
            initDatastore = false;
        }
        saveDatastore = !(namespace.get("output_datastore") == null);

        return namespace;
    }

    public boolean isInitDatastore() {
        return initDatastore;
    }

    public boolean isSaveDatastore() {
        return saveDatastore;
    }
}
