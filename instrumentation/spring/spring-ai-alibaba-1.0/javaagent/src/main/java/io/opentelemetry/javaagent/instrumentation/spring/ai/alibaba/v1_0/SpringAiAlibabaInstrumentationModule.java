/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.ai.alibaba.v1_0;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class SpringAiAlibabaInstrumentationModule extends InstrumentationModule {
  public SpringAiAlibabaInstrumentationModule() {
    super("spring-ai-alibaba", "spring-ai-alibaba-1.0");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(new DashScopeChatModelInstrumentation(),
        new DashScopeApiInstrumentation());
  }
}
