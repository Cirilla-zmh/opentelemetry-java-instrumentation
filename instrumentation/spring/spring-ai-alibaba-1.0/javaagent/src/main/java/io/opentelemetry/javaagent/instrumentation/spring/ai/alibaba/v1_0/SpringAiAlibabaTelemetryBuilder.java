/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.ai.alibaba.v1_0;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.arms.enums.ArmsRpcTypeEnum;
import io.opentelemetry.instrumentation.api.genai.MessageCaptureOptions;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.arms.common.ArmsCommonAttributeExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.arms.common.ArmsServiceTypeFactory;
import io.opentelemetry.instrumentation.api.instrumenter.genai.ArmsGenAiAppTypeOnceOperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.genai.ArmsGenAiAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.genai.ArmsGenAiMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.genai.GenAiAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.genai.GenAiMessagesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.genai.GenAiSpanNameExtractor;

/**
 * Builder for {@link SpringAiAlibabaTelemetry}.
 */
public final class SpringAiAlibabaTelemetryBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-ai-alibaba-1.0";

  private final OpenTelemetry openTelemetry;

  private boolean captureMessageContent;

  private int contentMaxLength;

  private String captureMessageStrategy;

  SpringAiAlibabaTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * Sets whether to capture message content in spans. Defaults to false.
   */
  @CanIgnoreReturnValue
  public SpringAiAlibabaTelemetryBuilder setCaptureMessageContent(boolean captureMessageContent) {
    this.captureMessageContent = captureMessageContent;
    return this;
  }

  /**
   * Sets the maximum length of message content to capture. Defaults to 8192.
   */
  @CanIgnoreReturnValue
  public SpringAiAlibabaTelemetryBuilder setContentMaxLength(int contentMaxLength) {
    this.contentMaxLength = contentMaxLength;
    return this;
  }

  /**
   * Sets the strategy to capture message content. Defaults to "span-attributes".
   */
  @CanIgnoreReturnValue
  public SpringAiAlibabaTelemetryBuilder setCaptureMessageStrategy(String captureMessageStrategy) {
    this.captureMessageStrategy = captureMessageStrategy;
    return this;
  }

  /**
   * Returns a new {@link SpringAiAlibabaTelemetry} with the settings of this {@link
   * SpringAiAlibabaTelemetryBuilder}.
   */
  public SpringAiAlibabaTelemetry build() {
    MessageCaptureOptions messageCaptureOptions = MessageCaptureOptions.create(
        captureMessageContent, contentMaxLength, captureMessageStrategy);

    Instrumenter<ChatCompletionRequest, ChatCompletion> chatCompletionInstrumenter =
        Instrumenter.<ChatCompletionRequest, ChatCompletion>builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                GenAiSpanNameExtractor.create(ChatModelAttributesGetter.INSTANCE))
            .addAttributesExtractor(GenAiAttributesExtractor.create(ChatModelAttributesGetter.INSTANCE))
            .addAttributesExtractor(ArmsGenAiAttributesExtractor.create(ChatModelAttributesGetter.INSTANCE))
            .addAttributesExtractor(GenAiMessagesExtractor.create(
                ChatModelAttributesGetter.INSTANCE,
                ChatModelMessagesProvider.create(messageCaptureOptions),
                messageCaptureOptions, INSTRUMENTATION_NAME))
            .addAttributesExtractor(new ArmsCommonAttributeExtractor(ArmsRpcTypeEnum.LLM_CLIENT.getCode(),
                ArmsServiceTypeFactory.LLM_INTERNAL.getCode()))
            .addOperationListener(ArmsGenAiAppTypeOnceOperationListener.create())
            .addOperationMetrics(ArmsGenAiMetrics.get())
            .buildInstrumenter();

    return new SpringAiAlibabaTelemetry(chatCompletionInstrumenter, messageCaptureOptions);
  }
}
