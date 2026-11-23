package edu.nju.sehw.takeout.model.message;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.network.AccessToken;
import edu.nju.sehw.takeout.model.network.SecureRequest;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import java.util.Objects;

/** Encodes authenticated requests and their replies for HTTPS and response persistence. */
public final class EnvelopeCodec {
  private static final int REQUEST = 0x53455101;
  private static final int RESPONSE = 0x53455201;
  private final MessageCodec messages;

  /** Creates an envelope codec using the shared message format. */
  public EnvelopeCodec(MessageCodec messages) {
    this.messages = Objects.requireNonNull(messages);
  }

  /** Encodes a request envelope. */
  public byte[] encodeRequest(SecureRequest request) {
    return BinaryMessageCodec.pack(
        out -> {
          out.writeInt(REQUEST);
          BinaryMessageCodec.writeUuid(out, request.requestId());
          BinaryMessageCodec.writeText(out, request.accessToken().value());
          BinaryMessageCodec.writeInstant(out, request.sentAt());
          BinaryMessageCodec.writeBytes(out, request.bodyDigest());
          BinaryMessageCodec.writeBytes(out, messages.encode(request.message()));
        });
  }

  /** Decodes a request envelope and validates its structural invariants. */
  public SecureRequest decodeRequest(byte[] bytes) {
    return BinaryMessageCodec.unpack(
        bytes,
        in -> {
          if (in.readInt() != REQUEST) {
            throw new IllegalArgumentException("Unknown request envelope");
          }
          return new SecureRequest(
              BinaryMessageCodec.readUuid(in),
              new AccessToken(BinaryMessageCodec.readText(in)),
              BinaryMessageCodec.readInstant(in),
              BinaryMessageCodec.readBytes(in),
              messages.decode(BinaryMessageCodec.readBytes(in)));
        });
  }

  /** Encodes a response envelope. */
  public byte[] encodeResponse(SecureResponse response) {
    return BinaryMessageCodec.pack(
        out -> {
          out.writeInt(RESPONSE);
          BinaryMessageCodec.writeUuid(out, response.requestId());
          BinaryMessageCodec.writeText(out, response.status().name());
          BinaryMessageCodec.writePayload(out, response.payload());
        });
  }

  /** Decodes a response envelope. */
  public SecureResponse decodeResponse(byte[] bytes) {
    return BinaryMessageCodec.unpack(
        bytes,
        in -> {
          if (in.readInt() != RESPONSE) {
            throw new IllegalArgumentException("Unknown response envelope");
          }
          return new SecureResponse(
              BinaryMessageCodec.readUuid(in),
              ResultStatus.valueOf(BinaryMessageCodec.readText(in)),
              BinaryMessageCodec.readPayload(in));
        });
  }
}
