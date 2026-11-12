package edu.nju.sehw.takeout.model.message;

/** Defines the deterministic wire encoding and decoding shared by request senders and verifiers. */
public interface MessageCodec {
  /**
   * Encodes a message using the deterministic wire representation shared by both peers.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @return the canonical encoded bytes used for transport and body-digest calculation
   */
  byte[] encode(Message message);

  /**
   * Decodes wire bytes into the typed message envelope and payload.
   *
   * @param encoded wire bytes to decode using the shared message format
   * @return the reconstructed message and its request identities
   */
  Message decode(byte[] encoded);
}
