/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai;

import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_CHOICE_COUNT;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_ENCODING_FORMATS;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_FREQUENCY_PENALTY;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_MAX_TOKENS;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_PRESENCE_PENALTY;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_SEED;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_STOP_SEQUENCES;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_TEMPERATURE;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_K;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_P;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_RESPONSE_FINISH_REASONS;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_RESPONSE_ID;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS;
import static io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS;
import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Extractor of <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/">GenAI
 * attributes</a>.
 *
 * <p>This class delegates to a type-specific {@link GenAiAttributesGetter} for individual attribute
 * extraction from request/response objects.
 */
public final class GenAiAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  /** Creates the GenAI attributes extractor. */
  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      GenAiAttributesGetter<REQUEST, RESPONSE> attributesGetter) {
    return new GenAiAttributesExtractor<>(attributesGetter);
  }

  private final GenAiAttributesGetter<REQUEST, RESPONSE> getter;

  private GenAiAttributesExtractor(GenAiAttributesGetter<REQUEST, RESPONSE> getter) {
    this.getter = getter;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    internalSet(attributes, GEN_AI_OPERATION_NAME, getter.getOperationName(request));
    internalSet(attributes, GEN_AI_PROVIDER_NAME, getter.getSystem(request));
    internalSet(attributes, GEN_AI_REQUEST_MODEL, getter.getRequestModel(request));
    internalSet(attributes, GEN_AI_REQUEST_SEED, getter.getRequestSeed(request));
    internalSet(
        attributes, GEN_AI_REQUEST_ENCODING_FORMATS, getter.getRequestEncodingFormats(request));
    internalSet(
        attributes, GEN_AI_REQUEST_FREQUENCY_PENALTY, getter.getRequestFrequencyPenalty(request));
    internalSet(attributes, GEN_AI_REQUEST_MAX_TOKENS, getter.getRequestMaxTokens(request));
    internalSet(
        attributes, GEN_AI_REQUEST_PRESENCE_PENALTY, getter.getRequestPresencePenalty(request));
    internalSet(attributes, GEN_AI_REQUEST_STOP_SEQUENCES, getter.getRequestStopSequences(request));
    internalSet(attributes, GEN_AI_REQUEST_TEMPERATURE, getter.getRequestTemperature(request));
    internalSet(attributes, GEN_AI_REQUEST_TOP_K, getter.getRequestTopK(request));
    internalSet(attributes, GEN_AI_REQUEST_TOP_P, getter.getRequestTopP(request));
    internalSet(attributes, GEN_AI_CONVERSATION_ID, getter.getConversationId(request));
    internalSet(attributes, GEN_AI_REQUEST_CHOICE_COUNT, getter.getChoiceCount(request));
    internalSet(attributes, GEN_AI_OUTPUT_TYPE, getter.getOutputType(request));
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    List<String> finishReasons = getter.getResponseFinishReasons(request, response);
    if (finishReasons != null && !finishReasons.isEmpty()) {
      attributes.put(GEN_AI_RESPONSE_FINISH_REASONS, finishReasons);
    }
    internalSet(attributes, GEN_AI_RESPONSE_ID, getter.getResponseId(request, response));
    internalSet(attributes, GEN_AI_RESPONSE_MODEL, getter.getResponseModel(request, response));
    internalSet(
        attributes, GEN_AI_USAGE_INPUT_TOKENS, getter.getUsageInputTokens(request, response));
    internalSet(
        attributes, GEN_AI_USAGE_OUTPUT_TOKENS, getter.getUsageOutputTokens(request, response));
  }
}
