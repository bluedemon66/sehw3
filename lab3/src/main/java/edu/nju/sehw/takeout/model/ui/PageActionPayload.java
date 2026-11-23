package edu.nju.sehw.takeout.model.ui;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Associates a page navigation with optional remote data.
 *
 * @param pageId page to assemble
 * @param operationCode operation that supplies page fields, or zero for a static page
 * @param arguments typed arguments for the page query
 */
public record PageActionPayload(int pageId, int operationCode, MessagePayload arguments)
    implements MessagePayload {
  /** Validates and snapshots the supplied fields. */
  public PageActionPayload {
    Objects.requireNonNull(arguments, "arguments");
    arguments = arguments.deepCopy();
  }

  @Override
  public PageActionPayload deepCopy() {
    return new PageActionPayload(pageId, operationCode, arguments);
  }
}
