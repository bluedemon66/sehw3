package edu.nju.sehw.takeout.model.resource;

/** Classifies a resource as a UI layout, image, text, or arbitrary binary data. */
public enum ResourceKind {
  /** A local layout describing a UI page. */
  UI_LAYOUT,
  /** An image resource. */
  IMAGE,
  /** A text resource. */
  TEXT,
  /** An arbitrary binary resource. */
  BINARY
}
