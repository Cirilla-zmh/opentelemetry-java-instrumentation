/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai.messages;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MessageCaptureOptions {

  public abstract boolean captureMessageContent();

  public abstract int maxMessageContentLength();

  public abstract CaptureMessageStrategy captureMessageStrategy();

  public static MessageCaptureOptions create(
      boolean captureMessageContent, int maxMessageContentLength, String captureMessageStrategy) {
    return new AutoValue_MessageCaptureOptions(
        captureMessageContent,
        maxMessageContentLength,
        CaptureMessageStrategy.fromValue(captureMessageStrategy));
  }

  public enum CaptureMessageStrategy {
    SPAN_ATTRIBUTES("span-attributes"),
    EVENT("event");

    private final String value;

    public String getValue() {
      return value;
    }

    CaptureMessageStrategy(String value) {
      this.value = value;
    }

    public static CaptureMessageStrategy fromValue(String value) {
      for (CaptureMessageStrategy strategy : CaptureMessageStrategy.values()) {
        if (strategy.value.equals(value)) {
          return strategy;
        }
      }
      // default
      return SPAN_ATTRIBUTES;
    }
  }
}
