package edu.nju.sehw.takeout.model.comment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries an immutable page of comment views and the cursor for the next page.
 *
 * @param items ordered comment display projections returned for this page
 * @param nextCursor optional cursor used to request the following page of comments
 */
public record CommentPage(List<CommentView> items, Optional<String> nextCursor) {
  /**
   * Creates the CommentPage value from the components documented below.
   *
   * @param items ordered comment display projections returned for this page
   * @param nextCursor optional cursor used to request the following page of comments
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentPage {
    Objects.requireNonNull(items, "items");
    items = List.copyOf(items);
    Objects.requireNonNull(nextCursor, "nextCursor");
  }
}
