package edu.nju.sehw.takeout.model.ui;

/** Defines the local callback through which an assembled page is returned to its UI controller. */
@FunctionalInterface
public interface UiDataPort {
  /**
   * Receives an assembled page so the controller can match its active request and install its
   * state.
   *
   * @param page assembled page associated with the UI request being completed
   */
  void receivePage(UiPage page);
}
