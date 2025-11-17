/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.ai.alibaba.v1_0;

import static java.util.Collections.emptyList;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

enum ChatModelAttributesGetter
    implements GenAiAttributesGetter<ChatCompletionRequest, ChatCompletion> {
  INSTANCE;

  @Override
  public String getOperationName(ChatCompletionRequest request) {
    return GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.CHAT;
  }

  @Override
  public String getSystem(ChatCompletionRequest request) {
    return GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.DASHSCOPE;
  }

  @Nullable
  @Override
  public String getRequestModel(ChatCompletionRequest request) {
    return request.model();
  }

  @Override
  public String getOperationTarget(ChatCompletionRequest request) {
    return getRequestModel(request);
  }

  @Nullable
  @Override
  public Long getRequestSeed(ChatCompletionRequest request) {
    if (request.parameters() == null || request.parameters().seed() == null) {
      return null;
    }
    return Long.valueOf(request.parameters().seed());
  }

  @Nullable
  @Override
  public List<String> getRequestEncodingFormats(ChatCompletionRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Double getRequestFrequencyPenalty(ChatCompletionRequest request) {
    // dashscope only supports repetition_penalty
    return null;
  }

  @Nullable
  @Override
  public Long getRequestMaxTokens(ChatCompletionRequest request) {
    if (request.parameters() == null || request.parameters().maxTokens() == null) {
      return null;
    }
    return Long.valueOf(request.parameters().maxTokens());
  }

  @Nullable
  @Override
  public Double getRequestPresencePenalty(ChatCompletionRequest request) {
    if (request.parameters() == null) {
      return null;
    }
    return request.parameters().presencePenalty();
  }

  @Nullable
  @Override
  public List<String> getRequestStopSequences(ChatCompletionRequest request) {
    if (request.parameters() == null || request.parameters().stop() == null) {
      return null;
    }
    return request.parameters().stop().stream()
        .map(
            s -> {
              if (s instanceof String) {
                return s;
              }
              if (s instanceof Number) {
                return String.valueOf(s);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .map(s -> (String) s)
        .collect(Collectors.toList());
  }

  @Nullable
  @Override
  public Double getRequestTemperature(ChatCompletionRequest request) {
    if (request.parameters() == null) {
      return null;
    }
    return request.parameters().temperature();
  }

  @Nullable
  @Override
  public Double getRequestTopK(ChatCompletionRequest request) {
    if (request.parameters() == null || request.parameters().topK() == null) {
      return null;
    }
    return Double.valueOf(request.parameters().topK());
  }

  @Nullable
  @Override
  public Double getRequestTopP(ChatCompletionRequest request) {
    if (request.parameters() == null) {
      return null;
    }
    return request.parameters().topP();
  }

  @Nullable
  @Override
  public Long getChoiceCount(ChatCompletionRequest request) {
    return null;
  }

  @Override
  public List<String> getResponseFinishReasons(
      ChatCompletionRequest request, @Nullable ChatCompletion response) {
    if (response == null || response.output() == null) {
      return emptyList();
    }
    return response.output().choices().stream()
        .map(choice -> choice.finishReason().name().toLowerCase())
        .collect(Collectors.toList());
  }

  @Override
  @Nullable
  public String getResponseId(ChatCompletionRequest request, @Nullable ChatCompletion response) {
    if (response == null) {
      return null;
    }
    return response.requestId();
  }

  @Override
  @Nullable
  public String getResponseModel(ChatCompletionRequest request, @Nullable ChatCompletion response) {
    return null;
  }

  @Override
  @Nullable
  public Long getUsageInputTokens(
      ChatCompletionRequest request, @Nullable ChatCompletion response) {
    if (response == null || response.usage() == null || response.usage().inputTokens() == null) {
      return null;
    }
    return Long.valueOf(response.usage().inputTokens());
  }

  @Override
  @Nullable
  public Long getUsageOutputTokens(
      ChatCompletionRequest request, @Nullable ChatCompletion response) {
    if (response == null || response.usage() == null || response.usage().outputTokens() == null) {
      return null;
    }
    return Long.valueOf(response.usage().outputTokens());
  }
}
