/*
 * Copyright 2014 - 2019 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence.view.impl.tx;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class JtaTransactionSynchronizationStrategy implements TransactionSynchronizationStrategy {
    
    private final TransactionSynchronizationRegistry synchronizationRegistry;

    public JtaTransactionSynchronizationStrategy(TransactionSynchronizationRegistry synchronizationRegistry) {
        this.synchronizationRegistry = synchronizationRegistry;
    }

    @Override
    public boolean isActive() {
        return synchronizationRegistry.getTransactionStatus() == Status.STATUS_ACTIVE;
    }

    @Override
    public void markRollbackOnly() {
        synchronizationRegistry.setRollbackOnly();
    }

    @Override
    public void registerSynchronization(Synchronization synchronization) {
        SynchronizationRegistry registry = (SynchronizationRegistry) synchronizationRegistry.getResource(SynchronizationRegistry.class.getName());
        if (registry == null) {
            registry = new SynchronizationRegistry();
            synchronizationRegistry.registerInterposedSynchronization(registry);
        }
        registry.addSynchronization(synchronization);
    }

}
