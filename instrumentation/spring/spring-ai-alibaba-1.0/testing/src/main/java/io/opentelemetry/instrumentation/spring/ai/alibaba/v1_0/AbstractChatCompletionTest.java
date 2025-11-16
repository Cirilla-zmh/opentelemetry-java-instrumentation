package io.opentelemetry.instrumentation.spring.ai.alibaba.v1_0;

import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.ArmsGenAiAttributes.GEN_AI_SPAN_KIND;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.ArmsGenAiAttributes.GEN_AI_USAGE_TOTAL_TOKENS;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_INPUT_MESSAGES;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_OUTPUT_MESSAGES;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MAX_TOKENS;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_SEED;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_STOP_SEQUENCES;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TEMPERATURE;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_K;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_P;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_FINISH_REASONS;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_ID;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.CHAT;
import static io.opentelemetry.instrumentation.api.instrumenter.genai.incubating.GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.DASHSCOPE;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat.Type;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

public abstract class AbstractChatCompletionTest extends AbstractSpringAiAlibabaTest {

  protected static final String TEST_CHAT_MODEL = "qwen3-coder-flash";
  protected static final String TEST_CHAT_INPUT =
      "Answer in up to 3 words: Which ocean contains Bouvet Island?";

  @Test
  void basic() {
    Prompt prompt = Prompt.builder()
        .messages(UserMessage.builder().text(TEST_CHAT_INPUT).build())
        .chatOptions(ChatOptions.builder().model(TEST_CHAT_MODEL).build())
        .build();
    DashScopeChatModel chatModel = getChatModel();

    ChatResponse response = chatModel.call(prompt);
    String content = "South Atlantic";
    assertThat(response.getResults().get(0).getOutput().getText()).isEqualTo(content);

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasAttributesSatisfying(
                            equalTo(GEN_AI_PROVIDER_NAME, DASHSCOPE),
                            equalTo(GEN_AI_OPERATION_NAME, CHAT),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_REQUEST_TEMPERATURE, 0.7d),
                            equalTo(GEN_AI_RESPONSE_ID, response.getMetadata().getId()),
                            satisfies(
                                GEN_AI_RESPONSE_FINISH_REASONS,
                                reasons -> reasons.containsExactly("stop")),
                            equalTo(GEN_AI_USAGE_INPUT_TOKENS, 23L),
                            equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 2L),
                            equalTo(GEN_AI_USAGE_TOTAL_TOKENS, 25L),
                            equalTo(GEN_AI_SPAN_KIND, "LLM"),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("user")),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("Answer in up to 3 words: Which ocean contains Bouvet Island?")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("assistant")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("South Atlantic")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("stop")))));
  }

  @Test
  void stream() {
    Prompt prompt = Prompt.builder()
        .messages(UserMessage.builder().text(TEST_CHAT_INPUT).build())
        .chatOptions(ChatOptions.builder().model(TEST_CHAT_MODEL).build())
        .build();
    DashScopeChatModel chatModel = getChatModel();

    List<ChatResponse> chunks = chatModel.stream(prompt).collectList().block();

    String fullMessage =
        chunks.stream()
            .map(
                cc -> {
                  if (cc.getResults().isEmpty()) {
                    return Optional.<String>empty();
                  }
                  return Optional.of(cc.getResults().get(0).getOutput().getText());
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining());

    String content = "Southern Ocean";
    assertEquals(fullMessage, content);

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasAttributesSatisfying(
                            equalTo(GEN_AI_PROVIDER_NAME, DASHSCOPE),
                            equalTo(GEN_AI_OPERATION_NAME, CHAT),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_REQUEST_TEMPERATURE, 0.7d),
                            equalTo(GEN_AI_RESPONSE_ID, chunks.get(0).getMetadata().getId()),
                            satisfies(
                                GEN_AI_RESPONSE_FINISH_REASONS,
                                reasons -> reasons.containsExactly("stop")),
                            equalTo(GEN_AI_USAGE_INPUT_TOKENS, 23L),
                            equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 2L),
                            equalTo(GEN_AI_USAGE_TOTAL_TOKENS, 25L),
                            equalTo(GEN_AI_SPAN_KIND, "LLM"),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("user")),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("Answer in up to 3 words: Which ocean contains Bouvet Island?")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("assistant")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("Southern Ocean")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("stop")))));
  }

  @Test
  void streamWithNonIncrementalOutput() {
    Prompt prompt = Prompt.builder()
        .messages(UserMessage.builder().text(TEST_CHAT_INPUT).build())
        .chatOptions(DashScopeChatOptions.builder()
            .withModel(TEST_CHAT_MODEL)
            .withIncrementalOutput(false)
            .build())
        .build();
    DashScopeChatModel chatModel = getChatModel();

    List<ChatResponse> chunks = chatModel.stream(prompt).collectList().block();

    String content = "Southern Ocean";
    assertEquals(chunks.get(chunks.size() - 1).getResult().getOutput().getText(), content);

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasAttributesSatisfying(
                            equalTo(GEN_AI_PROVIDER_NAME, DASHSCOPE),
                            equalTo(GEN_AI_OPERATION_NAME, CHAT),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_REQUEST_TEMPERATURE, 0.7d),
                            equalTo(GEN_AI_RESPONSE_ID, chunks.get(0).getMetadata().getId()),
                            satisfies(
                                GEN_AI_RESPONSE_FINISH_REASONS,
                                reasons -> reasons.containsExactly("stop")),
                            equalTo(GEN_AI_USAGE_INPUT_TOKENS, 23L),
                            equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 2L),
                            equalTo(GEN_AI_USAGE_TOTAL_TOKENS, 25L),
                            equalTo(GEN_AI_SPAN_KIND, "LLM"),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("user")),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("Answer in up to 3 words: Which ocean contains Bouvet Island?")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("assistant")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("Southern Ocean")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("stop")))));
  }

  @Test
  void allTheClientOptions() {
    DashScopeChatOptions options = DashScopeChatOptions.builder()
        .withModel(TEST_CHAT_MODEL)
        .withMaxToken(1000)
        .withSeed(100)
        .withStop(singletonList("foo"))
        .withTopK(3)
        .withTopP(1.0)
        .withTemperature(0.8)
        .withResponseFormat(DashScopeResponseFormat.builder().type(Type.TEXT).build())
        .build();
    Prompt prompt = Prompt.builder()
        .messages(UserMessage.builder().text(TEST_CHAT_INPUT).build())
        .chatOptions(options)
        .build();
    DashScopeChatModel chatModel = getChatModel();

    ChatResponse response = chatModel.call(prompt);
    String content = "Southern Ocean";
    assertThat(response.getResults().get(0).getOutput().getText()).isEqualTo(content);

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasAttributesSatisfying(
                            equalTo(GEN_AI_PROVIDER_NAME, DASHSCOPE),
                            equalTo(GEN_AI_OPERATION_NAME, CHAT),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_REQUEST_TEMPERATURE, 0.8d),
                            equalTo(GEN_AI_REQUEST_MAX_TOKENS, options.getMaxTokens()),
                            equalTo(GEN_AI_REQUEST_SEED, options.getSeed()),
                            satisfies(GEN_AI_REQUEST_STOP_SEQUENCES, seq -> seq.hasSize(options.getStop().size())),
                            equalTo(GEN_AI_REQUEST_TOP_K, Double.valueOf(options.getTopK())),
                            equalTo(GEN_AI_REQUEST_TOP_P, options.getTopP()),
                            equalTo(GEN_AI_RESPONSE_ID, response.getMetadata().getId()),
                            satisfies(
                                GEN_AI_RESPONSE_FINISH_REASONS,
                                reasons -> reasons.containsExactly("stop")),
                            equalTo(GEN_AI_USAGE_INPUT_TOKENS, 23L),
                            equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 2L),
                            equalTo(GEN_AI_USAGE_TOTAL_TOKENS, 25L),
                            equalTo(GEN_AI_SPAN_KIND, "LLM"),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("user")),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("Answer in up to 3 words: Which ocean contains Bouvet Island?")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("assistant")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("Southern Ocean")),
                            satisfies(GEN_AI_OUTPUT_MESSAGES, messages -> messages.contains("stop")))));
  }

  @Test
  void with400Error() {
    Prompt prompt = Prompt.builder()
        .messages(UserMessage.builder().text(TEST_CHAT_INPUT).build())
        .chatOptions(ChatOptions.builder().model("gpt-4o").build())
        .build();
    DashScopeChatModel chatModel = getChatModel();

    Throwable thrown = catchThrowable(() -> chatModel.stream(prompt).collectList().block());
    assertThat(thrown).isInstanceOf(Exception.class);

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasAttributesSatisfying(
                            equalTo(GEN_AI_PROVIDER_NAME, DASHSCOPE),
                            equalTo(GEN_AI_OPERATION_NAME, CHAT),
                            equalTo(GEN_AI_REQUEST_MODEL, "gpt-4o"),
                            equalTo(GEN_AI_SPAN_KIND, "LLM"),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("user")),
                            satisfies(GEN_AI_INPUT_MESSAGES, messages -> messages.contains("Answer in up to 3 words: Which ocean contains Bouvet Island?")))));
  }

  private ToolCallback buildGetWeatherToolDefinition() {
    return FunctionToolCallback.builder("get_weather", new GetWeatherFunction())
        .description("The location to get the current temperature for")
        .inputType(String.class)
        .build();
  }

  private static class GetWeatherFunction implements Function<String, String> {
    @Override
    public String apply(String location) {
      return "test";
    }
  }
}
