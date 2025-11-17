/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.auto.value.AutoValue;
import java.util.Map;
import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ToolDefinition {

  @JsonProperty(required = true, value = "type")
  @JsonPropertyDescription("Type of tool")
  public abstract String getType();

  @JsonProperty(required = true, value = "name")
  @JsonPropertyDescription("Name of tool")
  public abstract String getName();

  @Nullable
  @JsonProperty(value = "description")
  @JsonPropertyDescription("Description of tool")
  public abstract String getDescription();

  @Nullable
  @JsonProperty(value = "parameters")
  @JsonPropertyDescription("Parameters definitions of tool")
  public abstract Map<String, Object> getParameters();

  public static ToolDefinition create(String type, String name) {
    return create(type, name, null, null);
  }

  public static ToolDefinition create(
      String type, String name, @Nullable String description, @Nullable Map<String, Object> parameters) {
    return new AutoValue_ToolDefinition(type, name, description, parameters);
  }
}
