package edu.nju.sehw.takeout.model.message;

import edu.nju.sehw.takeout.model.comment.CommentPage;
import edu.nju.sehw.takeout.model.comment.CommentPagePayload;
import edu.nju.sehw.takeout.model.comment.CommentSort;
import edu.nju.sehw.takeout.model.comment.CommentStatus;
import edu.nju.sehw.takeout.model.comment.CommentStatusPayload;
import edu.nju.sehw.takeout.model.comment.CommentSubject;
import edu.nju.sehw.takeout.model.comment.CommentView;
import edu.nju.sehw.takeout.model.comment.CommentViewPayload;
import edu.nju.sehw.takeout.model.comment.HideCommentPayload;
import edu.nju.sehw.takeout.model.comment.ListCommentPayload;
import edu.nju.sehw.takeout.model.comment.PublishCommentPayload;
import edu.nju.sehw.takeout.model.comment.PurchaseMark;
import edu.nju.sehw.takeout.model.comment.ReplyCommentPayload;
import edu.nju.sehw.takeout.model.comment.ReplyView;
import edu.nju.sehw.takeout.model.comment.ReplyViewPayload;
import edu.nju.sehw.takeout.model.comment.SubjectKind;
import edu.nju.sehw.takeout.model.comment.WithdrawCommentPayload;
import edu.nju.sehw.takeout.model.order.OrderCommandPayload;
import edu.nju.sehw.takeout.model.order.PlaceOrderPayload;
import edu.nju.sehw.takeout.model.order.ResolveCasePayload;
import edu.nju.sehw.takeout.model.resource.AttachmentUploadPayload;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceKind;
import edu.nju.sehw.takeout.model.resource.ResourcePayload;
import edu.nju.sehw.takeout.model.resource.ResourceRefPayload;
import edu.nju.sehw.takeout.model.resource.ResourceRequestPayload;
import edu.nju.sehw.takeout.model.resource.UploadResourcePayload;
import edu.nju.sehw.takeout.model.ui.BooleanValue;
import edu.nju.sehw.takeout.model.ui.DecimalValue;
import edu.nju.sehw.takeout.model.ui.TextValue;
import edu.nju.sehw.takeout.model.ui.UiValue;
import edu.nju.sehw.takeout.model.ui.UiValuesPayload;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/** Versioned deterministic binary codec for remote operations and their results. */
public final class BinaryMessageCodec implements MessageCodec {
  /** Maximum size of an encoded request or response. */
  public static final int MAX_BYTES = 8 * 1024 * 1024;

  private static final int VERSION = 1;
  private static final int MAX_ITEMS = 10_000;

  /** Creates the shared version-one message codec. */
  public BinaryMessageCodec() {}

  @Override
  public byte[] encode(Message message) {
    return pack(
        out -> {
          out.writeInt(VERSION);
          writeUuid(out, message.requestId());
          writeUuid(out, message.correlationId());
          out.writeInt(message.senderId());
          out.writeInt(message.targetId());
          out.writeInt(message.operationCode());
          writePayload(out, message.payload());
        });
  }

  @Override
  public Message decode(byte[] encoded) {
    return unpack(
        encoded,
        in -> {
          if (in.readInt() != VERSION) {
            throw new IllegalArgumentException("Unsupported message version");
          }
          return new Message(
              readUuid(in),
              readUuid(in),
              in.readInt(),
              in.readInt(),
              in.readInt(),
              readPayload(in));
        });
  }

  static void writePayload(DataOutputStream out, MessagePayload payload) throws IOException {
    if (payload instanceof EmptyPayload) {
      out.writeInt(1);

    } else if (payload instanceof IntPayload value) {
      out.writeInt(2);
      writeList(out, value.values(), DataOutputStream::writeInt);
    } else if (payload instanceof ErrorPayload value) {
      out.writeInt(3);
      writeText(out, value.code());
      writeText(out, value.description());
    } else if (payload instanceof ListCommentPayload value) {
      out.writeInt(4);
      writeCommentSubject(out, value.subject());
      writeText(out, value.sort().name());
      writeOptional(out, value.cursor(), BinaryMessageCodec::writeText);
      out.writeInt(value.limit());
    } else if (payload instanceof PublishCommentPayload value) {
      out.writeInt(5);
      writeCommentSubject(out, value.subject());
      writeText(out, value.text());
      writeList(out, value.attachmentIds(), BinaryMessageCodec::writeText);
    } else if (payload instanceof WithdrawCommentPayload value) {
      out.writeInt(6);
      writeUuid(out, value.commentId());
    } else if (payload instanceof ReplyCommentPayload value) {
      out.writeInt(7);
      writeUuid(out, value.commentId());
      writeText(out, value.text());
    } else if (payload instanceof HideCommentPayload value) {
      out.writeInt(8);
      writeUuid(out, value.commentId());
      writeText(out, value.reason());
    } else if (payload instanceof CommentPagePayload value) {
      out.writeInt(9);
      writeCommentPage(out, value.page());
    } else if (payload instanceof CommentViewPayload value) {
      out.writeInt(10);
      writeCommentView(out, value.comment());
    } else if (payload instanceof CommentStatusPayload value) {
      out.writeInt(11);
      writeText(out, value.status().name());
    } else if (payload instanceof ReplyViewPayload value) {
      out.writeInt(12);
      writeReplyView(out, value.reply());
    } else if (payload instanceof OrderCommandPayload value) {
      out.writeInt(13);
      writeUuid(out, value.orderId());
      out.writeLong(value.expectedVersion());
    } else if (payload instanceof PlaceOrderPayload value) {
      out.writeInt(14);
      writeUuid(out, value.shopId());
      writeItems(out, value.items());
    } else if (payload instanceof ResolveCasePayload value) {
      out.writeInt(15);
      writeUuid(out, value.caseId());
      writeText(out, value.resolution());
    } else if (payload instanceof TargetPayload value) {
      out.writeInt(16);
      writeUuid(out, value.targetId());
    } else if (payload instanceof ResourceRequestPayload value) {
      out.writeInt(17);
      writeResourceDescriptor(out, value.descriptor());
    } else if (payload instanceof ResourcePayload value) {
      out.writeInt(18);
      writeResourceBlob(out, value.resource());
    } else if (payload instanceof ResourceRefPayload value) {
      out.writeInt(19);
      writeRemoteResourceRef(out, value.reference());
    } else if (payload instanceof AttachmentUploadPayload value) {
      out.writeInt(20);
      writeText(out, value.mediaType());
      out.writeLong(value.contentLength());
      writeText(out, value.contentDigest());
    } else if (payload instanceof UiValuesPayload value) {
      out.writeInt(21);
      writeValues(out, value.values());
    } else if (payload instanceof UploadResourcePayload value) {
      out.writeInt(22);
      writeLocalResourceRef(out, value.file());
    } else {
      throw new IllegalArgumentException(
          "Unsupported wire payload " + payload.getClass().getSimpleName());
    }
  }

  static MessagePayload readPayload(DataInputStream in) throws IOException {
    return switch (in.readInt()) {
      case 1 -> new EmptyPayload();
      case 2 -> new IntPayload(readList(in, DataInputStream::readInt));
      case 3 -> new ErrorPayload(readText(in), readText(in));
      case 4 ->
          new ListCommentPayload(
              readCommentSubject(in),
              CommentSort.valueOf(readText(in)),
              readOptional(in, BinaryMessageCodec::readText),
              in.readInt());
      case 5 ->
          new PublishCommentPayload(
              readCommentSubject(in), readText(in), readList(in, BinaryMessageCodec::readText));
      case 6 -> new WithdrawCommentPayload(readUuid(in));
      case 7 -> new ReplyCommentPayload(readUuid(in), readText(in));
      case 8 -> new HideCommentPayload(readUuid(in), readText(in));
      case 9 -> new CommentPagePayload(readCommentPage(in));
      case 10 -> new CommentViewPayload(readCommentView(in));
      case 11 -> new CommentStatusPayload(CommentStatus.valueOf(readText(in)));
      case 12 -> new ReplyViewPayload(readReplyView(in));
      case 13 -> new OrderCommandPayload(readUuid(in), in.readLong());
      case 14 -> new PlaceOrderPayload(readUuid(in), readItems(in));
      case 15 -> new ResolveCasePayload(readUuid(in), readText(in));
      case 16 -> new TargetPayload(readUuid(in));
      case 17 -> new ResourceRequestPayload(readResourceDescriptor(in));
      case 18 -> new ResourcePayload(readResourceBlob(in));
      case 19 -> new ResourceRefPayload(readRemoteResourceRef(in));
      case 20 -> new AttachmentUploadPayload(readText(in), in.readLong(), readText(in));
      case 21 -> new UiValuesPayload(readValues(in));
      case 22 -> new UploadResourcePayload(readLocalResourceRef(in));
      default -> throw new IllegalArgumentException("Unknown wire payload type");
    };
  }

  private static void writeCommentSubject(DataOutputStream out, CommentSubject value)
      throws IOException {
    writeText(out, value.kind().name());
    writeUuid(out, value.targetId());
    writeOptional(out, value.shopId(), BinaryMessageCodec::writeUuid);
    writeOptional(out, value.orderId(), BinaryMessageCodec::writeUuid);
  }

  private static CommentSubject readCommentSubject(DataInputStream in) throws IOException {
    return new CommentSubject(
        SubjectKind.valueOf(readText(in)),
        readUuid(in),
        readOptional(in, BinaryMessageCodec::readUuid),
        readOptional(in, BinaryMessageCodec::readUuid));
  }

  private static void writeCommentPage(DataOutputStream out, CommentPage value) throws IOException {
    writeList(out, value.items(), BinaryMessageCodec::writeCommentView);
    writeOptional(out, value.nextCursor(), BinaryMessageCodec::writeText);
  }

  private static CommentPage readCommentPage(DataInputStream in) throws IOException {
    return new CommentPage(
        readList(in, BinaryMessageCodec::readCommentView),
        readOptional(in, BinaryMessageCodec::readText));
  }

  private static void writeCommentView(DataOutputStream out, CommentView value) throws IOException {
    writeUuid(out, value.commentId());
    writeCommentSubject(out, value.subject());
    writeText(out, value.authorLabel());
    writeText(out, value.text());
    writeText(out, value.purchaseMark().name());
    writeText(out, value.status().name());
    writeInstant(out, value.createdAt());
    writeList(out, value.images(), BinaryMessageCodec::writeRemoteResourceRef);
    writeList(out, value.replies(), BinaryMessageCodec::writeReplyView);
  }

  private static CommentView readCommentView(DataInputStream in) throws IOException {
    return new CommentView(
        readUuid(in),
        readCommentSubject(in),
        readText(in),
        readText(in),
        PurchaseMark.valueOf(readText(in)),
        CommentStatus.valueOf(readText(in)),
        readInstant(in),
        readList(in, BinaryMessageCodec::readRemoteResourceRef),
        readList(in, BinaryMessageCodec::readReplyView));
  }

  private static void writeReplyView(DataOutputStream out, ReplyView value) throws IOException {
    writeUuid(out, value.replyId());
    writeText(out, value.merchantLabel());
    writeText(out, value.text());
    writeInstant(out, value.createdAt());
  }

  private static ReplyView readReplyView(DataInputStream in) throws IOException {
    return new ReplyView(readUuid(in), readText(in), readText(in), readInstant(in));
  }

  private static void writeRemoteResourceRef(DataOutputStream out, RemoteResourceRef value)
      throws IOException {
    writeText(out, value.resourceId());
    writeText(out, value.mediaType());
    out.writeLong(value.contentLength());
  }

  private static RemoteResourceRef readRemoteResourceRef(DataInputStream in) throws IOException {
    return new RemoteResourceRef(readText(in), readText(in), in.readLong());
  }

  private static void writeResourceBlob(DataOutputStream out, ResourceBlob value)
      throws IOException {
    writeRemoteResourceRef(out, value.reference());
    writeBytes(out, value.content());
  }

  private static ResourceBlob readResourceBlob(DataInputStream in) throws IOException {
    return new ResourceBlob(readRemoteResourceRef(in), readBytes(in));
  }

  private static void writeLocalResourceRef(DataOutputStream out, LocalResourceRef value)
      throws IOException {
    writeList(out, value.folderIndices(), DataOutputStream::writeInt);
    out.writeInt(value.entryIndex());
    writeText(out, value.kind().name());
  }

  private static LocalResourceRef readLocalResourceRef(DataInputStream in) throws IOException {
    return new LocalResourceRef(
        readList(in, DataInputStream::readInt), in.readInt(), ResourceKind.valueOf(readText(in)));
  }

  private static void writeResourceDescriptor(DataOutputStream out, ResourceDescriptor value)
      throws IOException {
    out.writeInt(value.elementId());
    writeOptional(out, value.localRef(), BinaryMessageCodec::writeLocalResourceRef);
    writeOptional(out, value.remoteResourceId(), BinaryMessageCodec::writeText);
    writeText(out, value.kind().name());
  }

  private static ResourceDescriptor readResourceDescriptor(DataInputStream in) throws IOException {
    return new ResourceDescriptor(
        in.readInt(),
        readOptional(in, BinaryMessageCodec::readLocalResourceRef),
        readOptional(in, BinaryMessageCodec::readText),
        ResourceKind.valueOf(readText(in)));
  }

  private static void writeItems(DataOutputStream out, Map<UUID, Integer> items)
      throws IOException {
    writeCount(out, items.size());
    for (var entry : new TreeMap<>(items).entrySet()) {
      writeUuid(out, entry.getKey());
      out.writeInt(entry.getValue());
    }
  }

  private static Map<UUID, Integer> readItems(DataInputStream in) throws IOException {
    Map<UUID, Integer> items = new TreeMap<>();
    int count = readCount(in);
    for (int i = 0; i < count; i++) {
      if (items.put(readUuid(in), in.readInt()) != null) {
        throw new IllegalArgumentException("Duplicate dish identifier");
      }
    }
    return items;
  }

  private static void writeValues(DataOutputStream out, Map<Integer, UiValue> values)
      throws IOException {
    writeCount(out, values.size());
    for (var entry : new TreeMap<>(values).entrySet()) {
      out.writeInt(entry.getKey());
      UiValue value = entry.getValue();
      if (value instanceof TextValue text) {
        out.writeByte(1);
        writeText(out, text.value());
      } else if (value instanceof DecimalValue decimal) {
        out.writeByte(2);
        writeText(out, decimal.value().toString());
      } else if (value instanceof BooleanValue bool) {
        out.writeByte(3);
        out.writeBoolean(bool.value());
      } else {
        throw new IllegalArgumentException("Unknown UI value type");
      }
    }
  }

  private static Map<Integer, UiValue> readValues(DataInputStream in) throws IOException {
    Map<Integer, UiValue> values = new TreeMap<>();
    int count = readCount(in);
    for (int i = 0; i < count; i++) {
      int key = in.readInt();
      UiValue value =
          switch (in.readUnsignedByte()) {
            case 1 -> new TextValue(readText(in));
            case 2 -> new DecimalValue(new BigDecimal(readText(in)));
            case 3 -> new BooleanValue(in.readBoolean());
            default -> throw new IllegalArgumentException("Unknown UI value type");
          };
      if (values.put(key, value) != null) {
        throw new IllegalArgumentException("Duplicate UI value identifier");
      }
    }
    return values;
  }

  static void writeUuid(DataOutputStream out, UUID value) throws IOException {
    out.writeLong(value.getMostSignificantBits());
    out.writeLong(value.getLeastSignificantBits());
  }

  static UUID readUuid(DataInputStream in) throws IOException {
    return new UUID(in.readLong(), in.readLong());
  }

  static void writeInstant(DataOutputStream out, Instant value) throws IOException {
    out.writeLong(value.getEpochSecond());
    out.writeInt(value.getNano());
  }

  static Instant readInstant(DataInputStream in) throws IOException {
    return Instant.ofEpochSecond(in.readLong(), in.readInt());
  }

  static void writeText(DataOutputStream out, String value) throws IOException {
    writeBytes(out, value.getBytes(StandardCharsets.UTF_8));
  }

  static String readText(DataInputStream in) throws IOException {
    return StandardCharsets.UTF_8
        .newDecoder()
        .decode(java.nio.ByteBuffer.wrap(readBytes(in)))
        .toString();
  }

  static void writeBytes(DataOutputStream out, byte[] value) throws IOException {
    if (value.length > MAX_BYTES) {
      throw new IllegalArgumentException("Wire field exceeds size limit");
    }
    out.writeInt(value.length);
    out.write(value);
  }

  static byte[] readBytes(DataInputStream in) throws IOException {
    int size = in.readInt();
    if (size < 0 || size > MAX_BYTES || size > in.available()) {
      throw new EOFException("Invalid wire field length");
    }
    return in.readNBytes(size);
  }

  private static <T> void writeOptional(DataOutputStream out, Optional<T> value, Writer<T> writer)
      throws IOException {
    out.writeBoolean(value.isPresent());
    if (value.isPresent()) {
      writer.write(out, value.orElseThrow());
    }
  }

  private static <T> Optional<T> readOptional(DataInputStream in, Reader<T> reader)
      throws IOException {
    return in.readBoolean() ? Optional.of(reader.read(in)) : Optional.empty();
  }

  private static <T> void writeList(DataOutputStream out, List<T> values, Writer<T> writer)
      throws IOException {
    writeCount(out, values.size());
    for (T value : values) {
      writer.write(out, value);
    }
  }

  private static <T> List<T> readList(DataInputStream in, Reader<T> reader) throws IOException {
    int count = readCount(in);
    List<T> values = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      values.add(reader.read(in));
    }
    return values;
  }

  private static void writeCount(DataOutputStream out, int count) throws IOException {
    if (count > MAX_ITEMS) {
      throw new IllegalArgumentException("Too many collection entries");
    }
    out.writeInt(count);
  }

  private static int readCount(DataInputStream in) throws IOException {
    int count = in.readInt();
    if (count < 0 || count > MAX_ITEMS) {
      throw new IllegalArgumentException("Invalid collection size");
    }
    return count;
  }

  static byte[] pack(Output output) {
    try (var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes)) {
      output.write(out);
      if (bytes.size() > MAX_BYTES) {
        throw new IllegalArgumentException("Wire datagram exceeds size limit");
      }
      return bytes.toByteArray();
    } catch (IOException ex) {
      throw new IllegalArgumentException("Cannot encode wire datagram", ex);
    }
  }

  static <T> T unpack(byte[] encoded, Reader<T> reader) {
    if (encoded.length > MAX_BYTES) {
      throw new IllegalArgumentException("Wire datagram exceeds size limit");
    }
    try (var in = new DataInputStream(new ByteArrayInputStream(encoded))) {
      T value = reader.read(in);
      if (in.read() != -1) {
        throw new IllegalArgumentException("Unexpected trailing bytes");
      }
      return value;
    } catch (IOException ex) {
      throw new IllegalArgumentException("Invalid wire datagram", ex);
    }
  }

  @FunctionalInterface
  interface Output {
    void write(DataOutputStream out) throws IOException;
  }

  @FunctionalInterface
  private interface Writer<T> {
    void write(DataOutputStream out, T value) throws IOException;
  }

  @FunctionalInterface
  interface Reader<T> {
    T read(DataInputStream in) throws IOException;
  }
}
