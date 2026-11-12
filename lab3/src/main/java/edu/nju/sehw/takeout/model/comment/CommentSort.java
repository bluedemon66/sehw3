package edu.nju.sehw.takeout.model.comment;

/** Selects the ordering strategy for a comment-list query. */
public enum CommentSort {
  /** Order comments using the configured text-length policy. */
  LONG_FIRST,
  /** Prioritize comments that have visible image attachments. */
  IMAGE_FIRST
}
