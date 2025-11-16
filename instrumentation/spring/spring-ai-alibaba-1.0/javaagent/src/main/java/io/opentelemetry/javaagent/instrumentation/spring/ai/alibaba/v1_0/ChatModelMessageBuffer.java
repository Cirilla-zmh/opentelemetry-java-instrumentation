/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.ai.alibaba.v1_0;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ChatCompletionFunction;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.Role;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ToolCall;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput.Choice;
import io.opentelemetry.instrumentation.api.genai.MessageCaptureOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

final class ChatModelMessageBuffer {
  private static final String TRUNCATE_FLAG = "...[truncated]";
  private final int index;
  private final MessageCaptureOptions messageCaptureOptions;
  private final boolean incrementalOutput;

  @Nullable private ChatCompletionFinishReason finishReason;

  @Nullable private StringBuilder rawContentBuffer;
  
  @Nullable private String rawContent;

  @Nullable private Role role;

  @Nullable private String name;

  @Nullable private String toolCallId;

  @Nullable private Map<Integer, ToolCallBuffer> toolCalls;

  ChatModelMessageBuffer(int index,
      MessageCaptureOptions messageCaptureOptions,
      boolean incrementalOutput) {
    this.index = index;
    this.messageCaptureOptions = messageCaptureOptions;
    this.incrementalOutput = incrementalOutput;
  }

  Choice toChoice() {
    List<ToolCall> toolCalls = null;
    if (this.toolCalls != null) {
      toolCalls = new ArrayList<>(this.toolCalls.size());
      for (Map.Entry<Integer, ToolCallBuffer> entry : this.toolCalls.entrySet()) {
        if (entry.getValue() != null) {
          String arguments = null;
          if (entry.getValue().function.arguments != null) {
            arguments = entry.getValue().function.arguments.toString();
          }
          toolCalls.add(new ToolCall(entry.getValue().id, entry.getValue().type,
              new ChatCompletionFunction(entry.getValue().function.name, arguments)));
        }
      }
    }

    String content = "";
    // Type of content is String for DashScope: https://bailian.console.aliyun.com/#/api/?type=model&url=2712576
    if (this.incrementalOutput) {
      if (this.rawContentBuffer != null) {
        content = this.rawContentBuffer.toString();
      }
    } else {
      if (this.rawContent != null) {
        content = this.rawContent;
      }
    }

    return new Choice(
        this.finishReason,
        new ChatCompletionMessage(content, this.role, this.name, this.toolCallId, toolCalls, null),
        null);
  }

  void append(Choice choice) {
    if (choice.message() != null) {
      if (this.messageCaptureOptions.captureMessageContent()) {
        // Type of content is String for DashScope: https://bailian.console.aliyun.com/#/api/?type=model&url=2712576
        if (choice.message().rawContent() instanceof String) {
          if (this.incrementalOutput) {
            if (this.rawContentBuffer == null) {
              this.rawContentBuffer = new StringBuilder();
            }

            String deltaContent = (String) choice.message().rawContent();
            if (this.rawContentBuffer.length() < this.messageCaptureOptions.maxMessageContentLength()) {
              if (this.rawContentBuffer.length() + deltaContent.length() >= this.messageCaptureOptions.maxMessageContentLength()) {
                deltaContent = deltaContent.substring(0, this.messageCaptureOptions.maxMessageContentLength() - this.rawContentBuffer.length());
                this.rawContentBuffer.append(deltaContent).append(TRUNCATE_FLAG);
              } else {
                this.rawContentBuffer.append(deltaContent);
              }
            }
          } else {
            // for non-incremental output
            this.rawContent = (String) choice.message().rawContent();
            if (this.rawContent.length() > this.messageCaptureOptions.maxMessageContentLength()) {
              this.rawContent = this.rawContent.substring(0, this.messageCaptureOptions.maxMessageContentLength());
              this.rawContent = this.rawContent + TRUNCATE_FLAG;
            }
          }
        }
      }

      if (choice.message().toolCalls() != null) {
        if (this.toolCalls == null) {
          this.toolCalls = new HashMap<>();
        }

        for (int i = 0; i < choice.message().toolCalls().size(); i++) {
          ToolCall toolCall = choice.message().toolCalls().get(i);
          ToolCallBuffer buffer =
              this.toolCalls.computeIfAbsent(
                  i, unused -> new ToolCallBuffer(toolCall.id()));
          if (toolCall.type() != null) {
            buffer.type = toolCall.type();
          }

          if (toolCall.function() != null) {
            if (toolCall.function().name() != null) {
              buffer.function.name = toolCall.function().name();
            }
            if (this.messageCaptureOptions.captureMessageContent() && toolCall.function().arguments() != null) {
              if (buffer.function.arguments == null) {
                buffer.function.arguments = new StringBuilder();
              }
              buffer.function.arguments.append(toolCall.function().arguments());
            }
          }
        }
      }

      if (choice.message().role() != null) {
        this.role = choice.message().role();
      }
      if (choice.message().name() != null) {
        this.name = choice.message().name();
      }
      if (choice.message().toolCallId() != null) {
        this.toolCallId = choice.message().toolCallId();
      }
    }

    if (choice.finishReason() != null) {
      this.finishReason = choice.finishReason();
    }
  }

  private static class FunctionBuffer {
    @Nullable String name;
    @Nullable StringBuilder arguments;
  }

  private static class ToolCallBuffer {
    final String id;
    final FunctionBuffer function = new FunctionBuffer();
    @Nullable String type;

    ToolCallBuffer(String id) {
      this.id = id;
    }
  }
}
