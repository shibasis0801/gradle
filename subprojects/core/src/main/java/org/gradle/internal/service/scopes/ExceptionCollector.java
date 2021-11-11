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

import org.gradle.internal.concurrent.Stoppable;

import java.util.ArrayList;
import java.util.List;

// TODO (donat) add test coverage. How is ClassPathModeExceptionCollector tested?

/**
 * Build service to suppress and collect exceptions for tooling purposes.
 */
public class ExceptionCollector implements Stoppable {

    private final boolean exceptionsSuppressed;
    private final List<Exception> exceptions = new ArrayList<>();

    ExceptionCollector(boolean exceptionsSuppressed) {
        this.exceptionsSuppressed = exceptionsSuppressed;
    }

    public boolean isExceptionsSuppressed() {
        return exceptionsSuppressed;
    }

    public void addException(Exception e) {
        // TODO (donat) do we need to make it thread-safe? Or, can we just use CopyOnWriteArrayList?
        synchronized (exceptions) {
            exceptions.add(e);
        }
    }

    public List<Exception> getExceptions() {
        List<Exception> result;
        synchronized (exceptions) {
            result = new ArrayList<>(exceptions);
        }
        return result;
    }

    @Override
    public void stop() {
        synchronized (exceptions) {
            exceptions.clear();
        }
    }

    // TODO (donat) add method similar to inline fun <T> ClassPathModeExceptionCollector.ignoringErrors(f: () -> T): T? =
}
