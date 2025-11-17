/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.ai.alibaba.v1_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

@AutoService(TypeInstrumentation.class)
public class DashScopeChatModelInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("internalStream"))
            .and(takesArguments(2))
            .and(returns(named("reactor.core.publisher.Flux"))),
        this.getClass().getName() + "$StreamAdvice");
  }

  @SuppressWarnings("unused")
  public static class StreamAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void streamEnter() {
      // do nothing
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void streamExit(
        @Advice.Return(readOnly = false) Flux<ChatResponse> response,
        @Advice.Thrown Throwable throwable) {
      if (throwable != null) {
        return;
      }

      response = ChatModelStreamWrapper.enableContextPropagation(response);
    }
  }
}
