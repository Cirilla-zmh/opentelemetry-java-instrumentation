/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.ai.alibaba.v1_0;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionChunk;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput.Choice;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.TokenUsage;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.genai.messages.MessageCaptureOptions;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class ChatModelStreamListener {

  private final Context context;
  private final ChatCompletionRequest request;
  private final Instrumenter<ChatCompletionRequest, ChatCompletion> instrumenter;
  private final MessageCaptureOptions messageCaptureOptions;
  private final boolean newSpan;
  private final boolean incrementalOutput;
  private final AtomicBoolean hasEnded;
  private final List<ChatModelMessageBuffer> chatModelMessageBuffers;

  // Aggregated metadata
  private final AtomicLong inputTokens = new AtomicLong(0);
  private final AtomicLong outputTokens = new AtomicLong(0);
  private final AtomicReference<String> requestId = new AtomicReference<>();

  public ChatModelStreamListener(
      Context context,
      ChatCompletionRequest request,
      Instrumenter<ChatCompletionRequest, ChatCompletion> instrumenter,
      MessageCaptureOptions messageCaptureOptions,
      boolean newSpan) {
    this.context = context;
    this.request = request;
    if (this.request != null
        && this.request.parameters() != null
        && this.request.parameters().incrementalOutput() != null) {
      this.incrementalOutput = this.request.parameters().incrementalOutput();
    } else {
      // true by default
      this.incrementalOutput = true;
    }
    this.instrumenter = instrumenter;
    this.messageCaptureOptions = messageCaptureOptions;
    this.newSpan = newSpan;
    this.hasEnded = new AtomicBoolean();
    this.chatModelMessageBuffers = new ArrayList<>();
  }

  public void onChunk(ChatCompletionChunk chunk) {
    if (chunk == null) {
      return;
    }

    if (chunk.requestId() != null) {
      requestId.set(chunk.requestId());
    }
    if (chunk.usage() != null) {
      if (chunk.usage().inputTokens() != null) {
        inputTokens.set(chunk.usage().inputTokens().longValue());
      }
      if (chunk.usage().outputTokens() != null) {
        outputTokens.set(chunk.usage().outputTokens().longValue());
      }
    }

    if (chunk.output() != null && chunk.output().choices() != null) {
      List<Choice> choices = chunk.output().choices();
      for (int i = 0; i < choices.size(); i++) {
        while (chatModelMessageBuffers.size() <= i) {
          chatModelMessageBuffers.add(null);
        }
        ChatModelMessageBuffer buffer = chatModelMessageBuffers.get(i);
        if (buffer == null) {
          buffer = new ChatModelMessageBuffer(i, messageCaptureOptions, incrementalOutput);
          chatModelMessageBuffers.set(i, buffer);
        }

        buffer.append(choices.get(i));
      }
    }
  }

  public void endSpan(@Nullable Throwable error) {
    // Use an atomic operation since close() type of methods are exposed to the user
    // and can come from any thread.
    if (!this.hasEnded.compareAndSet(false, true)) {
      return;
    }

    if (this.chatModelMessageBuffers.isEmpty()) {
      // Only happens if we got no chunks, so we have no response.
      if (this.newSpan) {
        this.instrumenter.end(this.context, this.request, null, error);
      }
      return;
    }

    Integer inputTokens = null;
    if (this.inputTokens.get() > 0) {
      inputTokens = (int) this.inputTokens.get();
    }

    Integer outputTokens = null;
    if (this.outputTokens.get() > 0) {
      outputTokens = (int) this.outputTokens.get();
    }

    List<Choice> choices =
        this.chatModelMessageBuffers.stream()
            .map(ChatModelMessageBuffer::toChoice)
            .collect(Collectors.toList());

    ChatCompletion result =
        new ChatCompletion(
            this.requestId.get(),
            new ChatCompletionOutput(null, choices, null),
            new TokenUsage(outputTokens, inputTokens, null, null, null, null, null, null, null));

    if (this.newSpan) {
      this.instrumenter.end(this.context, this.request, result, error);
    }
  }
}
