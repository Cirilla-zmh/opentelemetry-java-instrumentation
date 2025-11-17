/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of tool definitions. <br>
 * Thread unsafe model.
 */
public class ToolDefinitions {

  private final List<ToolDefinition> toolDefinitions;

  public List<ToolDefinition> getToolDefinitions() {
    return this.toolDefinitions;
  }

  public static ToolDefinitions create() {
    return new ToolDefinitions(new ArrayList<>());
  }

  public static ToolDefinitions create(List<ToolDefinition> toolDefinitions) {
    return new ToolDefinitions(new ArrayList<>(toolDefinitions));
  }

  public ToolDefinitions append(ToolDefinition toolDefinition) {
    this.toolDefinitions.add(toolDefinition);
    return this;
  }

  public List<ToolDefinition> getSerializableObject() {
    return this.getToolDefinitions();
  }

  private ToolDefinitions(List<ToolDefinition> toolDefinitions) {
    this.toolDefinitions = toolDefinitions;
  }
}
