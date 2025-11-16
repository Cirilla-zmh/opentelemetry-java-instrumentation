package io.opentelemetry.instrumentation.spring.ai.alibaba.v1_0;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.recording.RecordingExtension;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class AbstractSpringAiAlibabaTest {

  protected static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-ai-alibaba-1.0";

  private static final String API_URL = "https://dashscope.aliyuncs.com";

  @RegisterExtension
  static final RecordingExtension recording = new RecordingExtension(API_URL);

  protected abstract InstrumentationExtension getTesting();

  private DashScopeApi dashScopeApi;

  private DashScopeChatModel chatModel;

  protected final DashScopeApi getDashScopeApi() {
    if (dashScopeApi == null) {
      HttpClient httpClient = HttpClient.newBuilder()
          .version(Version.HTTP_1_1)
          .build();

      DashScopeApi.Builder builder = DashScopeApi.builder()
          .restClientBuilder(RestClient.builder()
              .requestFactory(new JdkClientHttpRequestFactory(httpClient)))
          .webClientBuilder(WebClient.builder()
              .clientConnector(new JdkClientHttpConnector(httpClient)))
          .baseUrl("http://localhost:" + recording.getPort());
      if (recording.isRecording()) {
        builder.apiKey(System.getenv("DASHSCOPE_API_KEY"));
      } else {
        builder.apiKey("unused");
      }
      dashScopeApi = builder.build();
    }
    return dashScopeApi;
  }

  protected final DashScopeChatModel getChatModel() {
    if (chatModel == null) {
      chatModel = DashScopeChatModel.builder()
          .dashScopeApi(getDashScopeApi())
          // there's a bug of tool eligibility in dashscope 1.0.0.3, so we couldn't test tool calls
          .toolExecutionEligibilityPredicate((o1, o2) -> false)
          .build();
    }
    return chatModel;
  }
}
