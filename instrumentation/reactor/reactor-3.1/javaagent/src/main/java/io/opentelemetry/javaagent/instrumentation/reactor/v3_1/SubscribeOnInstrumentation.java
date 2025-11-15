/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.reactor.v3_1;

import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.reactor.v3_1.ContextPropagationOperator;
import io.opentelemetry.javaagent.bootstrap.reactor.ReactorSubscribeOnProcessTracing;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import reactor.core.CoreSubscriber;

public class SubscribeOnInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("reactor.core.publisher.FluxSubscribeOn$SubscribeOnSubscriber");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(named("run"), this.getClass().getName() + "$RunAdvice");
  }

  @SuppressWarnings({"unused", "rawtypes"})
  public static class RunAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void runEnter(
        @Advice.FieldValue("actual") CoreSubscriber subscriber,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (subscriber == null) {
        return;
      }
      Boolean contextPropagationEnabled =
          subscriber
              .currentContext()
              .getOrDefault(ReactorSubscribeOnProcessTracing.CONTEXT_PROPAGATION_KEY, false);
      if (contextPropagationEnabled == null || !contextPropagationEnabled) {
        // 开关未开启时，关闭上下文透传，与之前行为保持一致
        return;
      }

      // 从 Reactor 上下文中获取 OTel 上下文，进行透传
      context =
          ContextPropagationOperator.getOpenTelemetryContext(
              subscriber.currentContext(), Context.current());

      if (context != null) {
        scope = context.makeCurrent();
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void runExit(@Advice.Local("otelScope") Scope scope) {
      if (scope != null) {
        scope.close();
      }
    }
  }
}
