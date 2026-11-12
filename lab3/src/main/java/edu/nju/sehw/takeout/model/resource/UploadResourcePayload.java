package edu.nju.sehw.takeout.model.resource;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries the local resource reference selected for upload.
 *
 * @param file local resource reference identifying the bytes selected for upload
 */
public record UploadResourcePayload(LocalResourceRef file) implements MessagePayload {
  /**
   * Creates the UploadResourcePayload value from the components documented below.
   *
   * @param file local resource reference identifying the bytes selected for upload
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UploadResourcePayload {
    Objects.requireNonNull(file, "file");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public UploadResourcePayload deepCopy() {
    return new UploadResourcePayload(file);
  }
}
