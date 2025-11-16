/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.ai.alibaba.v1_0;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.genai.MessageCaptureOptions;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

/**
 * Entrypoint for instrumenting Spring AI AlibabaDashScope clients.
 */
public final class SpringAiAlibabaTelemetry {

  /**
   * Returns a new {@link SpringAiAlibabaTelemetryBuilder} configured with the given {@link OpenTelemetry}.
   */
  public static SpringAiAlibabaTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new SpringAiAlibabaTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<ChatCompletionRequest, ChatCompletion> chatCompletionInstrumenter;
  private final MessageCaptureOptions messageCaptureOptions;

  SpringAiAlibabaTelemetry(
      Instrumenter<ChatCompletionRequest, ChatCompletion> chatCompletionInstrumenter,
      MessageCaptureOptions messageCaptureOptions) {
    this.chatCompletionInstrumenter = chatCompletionInstrumenter;
    this.messageCaptureOptions = messageCaptureOptions;
  }

  public Instrumenter<ChatCompletionRequest, ChatCompletion> chatCompletionInstrumenter() {
    return chatCompletionInstrumenter;
  }

  public MessageCaptureOptions messageCaptureOptions() {
    return messageCaptureOptions;
  }

}
