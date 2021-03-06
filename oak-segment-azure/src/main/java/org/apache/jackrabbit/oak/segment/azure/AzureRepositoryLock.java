/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.segment.azure;

import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.jackrabbit.oak.segment.spi.persistence.RepositoryLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AzureRepositoryLock implements RepositoryLock {

    private static final Logger log = LoggerFactory.getLogger(AzureRepositoryLock.class);

    private static int INTERVAL = 60;

    private final Runnable shutdownHook;

    private final CloudBlockBlob blob;

    private final ExecutorService executor;

    private String leaseId;

    private volatile boolean doUpdate;

    public AzureRepositoryLock(CloudBlockBlob blob, Runnable shutdownHook) {
        this.shutdownHook = shutdownHook;
        this.blob = blob;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public AzureRepositoryLock lock() throws IOException {
        try {
            blob.openOutputStream().close();
            leaseId = blob.acquireLease(INTERVAL, null);
            log.info("Acquired lease {}", leaseId);
        } catch (StorageException e) {
            throw new IOException(e);
        }
        executor.submit(this::refreshLease);
        return this;
    }

    private void refreshLease() {
        doUpdate = true;
        long lastUpdate = 0;
        while (doUpdate) {
            try {
                long timeSinceLastUpdate = (System.currentTimeMillis() - lastUpdate) / 1000;
                if (timeSinceLastUpdate > INTERVAL / 2) {
                    blob.renewLease(AccessCondition.generateLeaseCondition(leaseId));
                    lastUpdate = System.currentTimeMillis();
                }
            } catch (StorageException e) {
                log.error("Can't renew the lease", e);
                shutdownHook.run();
                doUpdate = false;
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Interrupted the lease renewal loop", e);
            }
        }
    }

    @Override
    public void unlock() throws IOException {
        doUpdate = false;
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            releaseLease();
        }
    }

    private void releaseLease() throws IOException {
        try {
            blob.releaseLease(AccessCondition.generateLeaseCondition(leaseId));
            blob.delete();
            log.info("Released lease {}", leaseId);
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }
}
