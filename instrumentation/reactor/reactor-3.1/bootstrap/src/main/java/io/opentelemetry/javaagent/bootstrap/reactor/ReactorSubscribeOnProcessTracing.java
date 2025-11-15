/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.bootstrap.reactor;

// Classes used by multiple instrumentations should be in a bootstrap module to ensure that all
// instrumentations see the same class. Helper classes are injected into each class loader that
// contains an instrumentation that uses them, so instrumentations in different class loaders will
// have separate copies of helper classes.
public final class ReactorSubscribeOnProcessTracing {
  public static final Object CONTEXT_PROPAGATION_KEY =
      new Object() {
        @Override
        public String toString() {
          return "otel_context_propagation";
        }
      };
}
