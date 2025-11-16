/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.ai.alibaba.v1_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;

public final class SpringAiAlibabaSingletons {
  public static final SpringAiAlibabaTelemetry TELEMETRY =
      SpringAiAlibabaTelemetry.builder(GlobalOpenTelemetry.get())
          .setCaptureMessageContent(
              InstrumentationConfig.get()
                  .getBoolean("otel.instrumentation.genai.capture-message-content", true))
          .setContentMaxLength(
              InstrumentationConfig.get()
                  .getInt("otel.instrumentation.genai.message-content.max-length", 8192))
          .setCaptureMessageStrategy(
              InstrumentationConfig.get()
                  .getString("otel.instrumentation.genai.message-content.capture-strategy", "span-attributes"))
          .build();

  private SpringAiAlibabaSingletons() {}
}
