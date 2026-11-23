package edu.nju.sehw.takeout.model.network;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Computes the digest shared by request preparation and verification. */
public final class MessageDigests {
  private MessageDigests() {}

  /**
   * Computes the SHA-256 digest of a message.
   *
   * @param body encoded message
   * @return the 32-byte digest
   */
  public static byte[] sha256(byte[] body) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(body);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }
}
