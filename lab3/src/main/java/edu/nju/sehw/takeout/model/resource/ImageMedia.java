package edu.nju.sehw.takeout.model.resource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Identifies the image media types accepted by the attachment protocol. */
public final class ImageMedia {
  private static final byte[] PNG = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
  private static final byte[] JPEG = {(byte) 255, (byte) 216, (byte) 255};
  private static final byte[] GIF87 = "GIF87a".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] GIF89 = "GIF89a".getBytes(StandardCharsets.US_ASCII);

  private ImageMedia() {}

  /** Returns the image media type recognized from its file signature. */
  public static String detect(byte[] bytes) {
    if (startsWith(bytes, PNG)) {
      return "image/png";
    }
    if (startsWith(bytes, JPEG)) {
      return "image/jpeg";
    }
    if (startsWith(bytes, GIF87) || startsWith(bytes, GIF89)) {
      return "image/gif";
    }
    throw new IllegalArgumentException("Expected a PNG, JPEG, or GIF image");
  }

  private static boolean startsWith(byte[] data, byte[] signature) {
    return data.length >= signature.length
        && Arrays.mismatch(data, 0, signature.length, signature, 0, signature.length) == -1;
  }
}
