package edu.nju.sehw.takeout.client.processing;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;

/**
 * Keeps the UI origin and arguments associated with one request and its root operation.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param correlationId identifier shared by the child requests of one root UI operation
 * @param sourceUiId registered participant identifier of the UI that should receive the final page
 * @param pageId identifier of the page in the client's local page catalogue
 * @param arguments typed arguments retained for this individual request
 */
public record RequestContext(
    UUID requestId, UUID correlationId, int sourceUiId, int pageId, MessagePayload arguments) {
  /**
   * Creates the RequestContext value from the components documented below.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param correlationId identifier shared by the child requests of one root UI operation
   * @param sourceUiId registered participant identifier of the UI that should receive the final
   *     page
   * @param pageId identifier of the page in the client's local page catalogue
   * @param arguments typed arguments retained for this individual request
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public RequestContext {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(arguments, "arguments");
    arguments = Objects.requireNonNull(arguments.deepCopy(), "payload snapshot");
  }

  /**
   * Returns an isolated snapshot of the stored operation data.
   *
   * @return a copy of the stored typed payload
   */
  @Override
  public MessagePayload arguments() {
    return arguments.deepCopy();
  }
}
