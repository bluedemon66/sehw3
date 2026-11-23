package edu.nju.sehw.takeout.client.core;

/** Participant identifiers used by the default client routing configuration. */
public final class ClientParticipants {
  /** UI controller. */
  public static final int UI = 1;

  /** UI event router. */
  public static final int EVENTS = 2;

  /** Page assembly. */
  public static final int PAGES = 3;

  /** Page catalogue. */
  public static final int CATALOG = 4;

  /** Resource acquisition. */
  public static final int RESOURCES = 5;

  /** Order and scalar operations. */
  public static final int CRUD = 6;

  /** Comment operations. */
  public static final int COMMENTS = 7;

  /** Request and response gateway. */
  public static final int GATEWAY = 8;

  /** Remote resource downloads. */
  public static final int DOWNLOAD = 9;

  /** Remote attachment uploads. */
  public static final int UPLOAD = 10;

  private ClientParticipants() {}
}
