package edu.nju.sehw.takeout.model.comment;

/** Describes whether a comment is published, withdrawn, or hidden by the platform. */
public enum CommentStatus {
  /** The comment is published and eligible for public display. */
  PUBLISHED,
  /** The author has withdrawn the comment. */
  WITHDRAWN,
  /** The platform has hidden the comment from public display. */
  HIDDEN_BY_PLATFORM
}
