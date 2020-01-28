/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This program is licensed under the SuperTokens Community License (the
 *    "License") as published by VRAI Labs. You may not use this file except in
 *    compliance with the License. You are not permitted to transfer or
 *    redistribute this file without express written permission from VRAI Labs.
 *
 *    A copy of the License is available in the file titled
 *    "SuperTokensLicense.pdf" inside this repository or included with your copy of
 *    the software or its source code. If you have not received a copy of the
 *    License, please write to VRAI Labs at team@supertokens.io.
 *
 *    Please read the License carefully before accessing, downloading, copying,
 *    using, modifying, merging, transferring or sharing this software. By
 *    undertaking any of these activities, you indicate your agreement to the terms
 *    of the License.
 *
 *    This program is distributed with certain software that is licensed under
 *    separate terms, as designated in a particular file or component or in
 *    included license documentation. VRAI Labs hereby grants you an additional
 *    permission to link the program and your derivative works with the separately
 *    licensed software that they have included with this program, however if you
 *    modify this program, you shall be solely liable to ensure compliance of the
 *    modified program with the terms of licensing of the separately licensed
 *    software.
 *
 *    Unless required by applicable law or agreed to in writing, this program is
 *    distributed under the License on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *
 */

package io.supertokens.storageLayer;

import io.supertokens.Main;
import io.supertokens.ResourceDistributor;
import io.supertokens.exceptions.QuitProgramException;
import io.supertokens.output.Logging;
import io.supertokens.pluginInterface.Storage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.ServiceLoader;

public class StorageLayer extends ResourceDistributor.SingletonResource {

    private static final String RESOURCE_KEY = "io.supertokens.storageLayer.StorageLayer";
    private final Storage storageLayer;

    private StorageLayer(Main main, String pluginFolderPath) throws MalformedURLException {
        Logging.info(main, "Loading storage layer.");
        File loc = new File(pluginFolderPath);

        File[] flist = loc.listFiles(file -> file.getPath().toLowerCase().endsWith(".jar"));

        if (flist == null) {
            throw new QuitProgramException("No database plugin found. Please redownload and install SuperTokens");
        }
        URL[] urls = new URL[flist.length];
        for (int i = 0; i < flist.length; i++) {
            urls[i] = flist[i].toURI().toURL();
        }
        URLClassLoader ucl = new URLClassLoader(urls);

        ServiceLoader<Storage> sl = ServiceLoader.load(Storage.class, ucl);
        Iterator<Storage> it = sl.iterator();
        Storage storageLayerTemp = null;
        while (it.hasNext()) {
            Storage plugin = it.next();
            if (storageLayerTemp == null) {
                storageLayerTemp = plugin;
            } else {
                throw new QuitProgramException(
                        "Multiple database plugins found. Please make sure that just one plugin is in the /plugin " +
                                "folder of the installation. Alternatively, please redownload and install SuperTokens" +
                                ".");
            }
        }
        if (storageLayerTemp == null) {
            throw new QuitProgramException("No database plugin found. Please redownload and install SuperTokens");
        }
        this.storageLayer = storageLayerTemp;
        this.storageLayer.constructor(main.getProcessId(), Main.makeConsolePrintSilent);
    }

    private static StorageLayer getInstance(Main main) {
        return (StorageLayer) main.getResourceDistributor().getResource(RESOURCE_KEY);
    }

    public static void init(Main main, String pluginFolderPath) throws MalformedURLException {
        if (getInstance(main) != null) {
            return;
        }
        main.getResourceDistributor().setResource(RESOURCE_KEY, new StorageLayer(main, pluginFolderPath));
    }

    public static Storage getStorageLayer(Main main) {
        if (getInstance(main) == null) {
            throw new QuitProgramException("please call init() before calling getStorageLayer");
        }
        return getInstance(main).storageLayer;
    }
}
