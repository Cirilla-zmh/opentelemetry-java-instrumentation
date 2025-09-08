/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai.messages;

/** Interface for all message parts. */
public interface MessagePart {

  /**
   * Get the type of this message part.
   *
   * @return the type string
   */
  String getType();
}
