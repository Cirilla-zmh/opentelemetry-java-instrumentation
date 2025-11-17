/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai.messages;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of output messages from the model. <br>
 * Thread unsafe model.
 */
public class OutputMessages {

  private final List<OutputMessage> messages;

  public List<OutputMessage> getMessages() {
    return this.messages;
  }

  public static OutputMessages create() {
    return new OutputMessages(new ArrayList<>());
  }

  public static OutputMessages create(List<OutputMessage> messages) {
    return new OutputMessages(new ArrayList<>(messages));
  }

  @CanIgnoreReturnValue
  public OutputMessages append(OutputMessage outputMessage) {
    this.messages.add(outputMessage);
    return this;
  }

  public List<OutputMessage> getSerializableObject() {
    return this.messages;
  }

  /**
   * Merges a chunk OutputMessage into the existing messages at the specified index. This method is
   * used for streaming responses where content is received in chunks.
   *
   * @param index the index of the message to merge into
   * @param chunkMessage the chunk message to append
   * @return a new OutputMessages instance with the merged content
   */
  public OutputMessages merge(int index, OutputMessage chunkMessage) {
    List<OutputMessage> currentMessages = new ArrayList<>(getMessages());

    if (index < 0 || index >= currentMessages.size()) {
      throw new IllegalArgumentException(
          "Index "
              + index
              + " is out of bounds for messages list of size "
              + currentMessages.size());
    }

    OutputMessage existingMessage = currentMessages.get(index);

    // Merge the parts by appending text content from chunk to existing message
    List<MessagePart> mergedParts = new ArrayList<>(existingMessage.getParts());

    // If the chunk message has text parts, append their content to the first text part of existing
    // message
    for (MessagePart chunkPart : chunkMessage.getParts()) {
      if (chunkPart instanceof TextPart) {
        TextPart chunkTextPart =
            (TextPart) chunkPart;

        // Find the first text part in existing message to append to
        boolean appended = false;
        for (int i = 0; i < mergedParts.size(); i++) {
          MessagePart existingPart = mergedParts.get(i);
          if (existingPart instanceof TextPart) {
            TextPart existingTextPart = (TextPart) existingPart;
            // Create a new TextPart with combined content
            TextPart mergedTextPart =
                TextPart.create(existingTextPart.getContent() + chunkTextPart.getContent());
            mergedParts.set(i, mergedTextPart);
            appended = true;
            break;
          }
        }

        // If no existing text part found, add the chunk as a new part
        if (!appended) {
          mergedParts.add(chunkTextPart);
        }
      } else {
        // For non-text parts, add them as new parts
        mergedParts.add(chunkPart);
      }
    }

    // Create new OutputMessage with merged parts, using the chunk's finish reason if available
    String finalFinishReason =
        chunkMessage.getFinishReason() != null
            ? chunkMessage.getFinishReason()
            : existingMessage.getFinishReason();

    OutputMessage mergedMessage =
        OutputMessage.create(existingMessage.getRole(), mergedParts, finalFinishReason);

    // Replace the message at the specified index
    currentMessages.set(index, mergedMessage);

    return new OutputMessages(currentMessages);
  }

  private OutputMessages(List<OutputMessage> messages) {
    this.messages = messages;
  }
}
