package edu.nju.sehw.takeout.client.ui;

/** Defines scheduling of a rendering action on the selected UI toolkit's thread. */
public interface UiThreadDispatcher {
  /**
   * Schedules the rendering action on the UI toolkit's thread.
   *
   * @param drawing rendering action to schedule on the UI toolkit's thread
   */
  void dispatch(Runnable drawing);
}
