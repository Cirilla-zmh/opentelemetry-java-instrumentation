/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of input messages sent to the model. <br>
 * Thread unsafe model.
 */
public class InputMessages {

  private final List<InputMessage> messages;

  public List<InputMessage> getMessages() {
    return this.messages;
  }

  public static InputMessages create() {
    return new InputMessages(new ArrayList<>());
  }

  public static InputMessages create(List<InputMessage> messages) {
    return new InputMessages(new ArrayList<>(messages));
  }

  public InputMessages append(InputMessage inputMessage) {
    this.messages.add(inputMessage);
    return this;
  }

  public List<InputMessage> getSerializableObject() {
    return this.messages;
  }

  private InputMessages(List<InputMessage> messages) {
    this.messages = messages;
  }
}
