/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.internal.service.scopes;

import org.gradle.api.Action;
import org.gradle.internal.concurrent.Stoppable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Build service to suppress and collect exceptions for tooling purposes.
 */
public class ExceptionCollector implements Stoppable {

    private final boolean exceptionsSuppressed;
    private final List<Exception> exceptions = new CopyOnWriteArrayList<>();

    ExceptionCollector(boolean exceptionsSuppressed) {
        this.exceptionsSuppressed = exceptionsSuppressed;
    }

    public boolean isExceptionsSuppressed() {
        return exceptionsSuppressed;
    }

    public void addException(Exception e) {
        exceptions.add(e);
    }

    public List<Exception> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

    @Override
    public void stop() {
        exceptions.clear();
    }

    public <T> Action<T> decorate(Action<T> action) {
        if (exceptionsSuppressed) {
            return new ExceptionCollectingAction(action);
        } else {
            return action;
        }
    }

    private class ExceptionCollectingAction<T> implements Action<T> {

        private final Action<T> delegate;

        private ExceptionCollectingAction(Action<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(final T arg) {
            try {
                delegate.execute(arg);
            } catch (Exception e) {
                ExceptionCollector.this.addException(e);
            }
        }
    }
}
