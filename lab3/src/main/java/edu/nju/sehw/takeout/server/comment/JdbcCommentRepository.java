package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.CommentPage;
import edu.nju.sehw.takeout.model.comment.CommentSort;
import edu.nju.sehw.takeout.model.comment.CommentStatus;
import edu.nju.sehw.takeout.model.comment.CommentSubject;
import edu.nju.sehw.takeout.model.comment.CommentView;
import edu.nju.sehw.takeout.model.comment.ListCommentPayload;
import edu.nju.sehw.takeout.model.comment.PurchaseMark;
import edu.nju.sehw.takeout.model.comment.ReplyView;
import edu.nju.sehw.takeout.model.comment.SubjectKind;
import edu.nju.sehw.takeout.model.common.Permission;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.server.operation.OperationFailure;
import edu.nju.sehw.takeout.server.persistence.DatabaseAdapter;
import edu.nju.sehw.takeout.server.persistence.Jdbc;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Adapts comment repository operations to the configured database access port. */
public final class JdbcCommentRepository implements CommentRepository {
  /**
   * Creates this component with the dependencies documented below.
   *
   * @param database database execution port participating in the current transaction
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public JdbcCommentRepository(DatabaseAdapter database) {
    Objects.requireNonNull(database, "database");
  }

  /**
   * Reads the visible comment projections using the selected target, ordering, and cursor.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param query comment-list parameters together with the authenticated request context
   * @return the requested comment page and its optional next-page cursor
   */
  @Override
  public CommentPage listPage(TransactionContext transaction, CommentQuery query) {
    var queryPayload = query.payload();
    var subject = queryPayload.subject();
    boolean imageFirst = queryPayload.sort() == CommentSort.IMAGE_FIRST;
    String imageKey = imageFirst ? "CASE WHEN image_count > 0 THEN 1 ELSE 0 END" : "0 + 0";
    StringBuilder sql =
        new StringBuilder(
            "SELECT comments.*, "
                + imageKey
                + " AS image_key FROM comments"
                + " WHERE target_kind=? AND target_id=? AND status='PUBLISHED'");
    List<Object> args = new ArrayList<>(List.of(subject.kind().name(), subject.targetId()));
    subject
        .shopId()
        .ifPresent(
            shop -> {
              sql.append(" AND shop_id=?");
              args.add(shop);
            });
    subject
        .orderId()
        .ifPresent(
            order -> {
              sql.append(" AND order_id=?");
              args.add(order);
            });
    queryPayload
        .cursor()
        .ifPresent(
            value -> {
              var cursor = Cursor.decode(queryPayload, value);
              sql.append(" AND (")
                  .append(imageKey)
                  .append(", body_length, created_at, comment_id) < (?, ?, ?, ?)");
              args.addAll(
                  List.of(cursor.images(), cursor.length(), cursor.createdAt(), cursor.id()));
            });
    sql.append(
        " ORDER BY image_key DESC, body_length DESC, created_at DESC, comment_id DESC LIMIT ?");
    args.add(queryPayload.limit() + 1);
    var rows =
        Jdbc.query(
            transaction,
            sql.toString(),
            row ->
                new ListedComment(
                    readView(transaction, row),
                    new Cursor(
                        row.getInt("image_key"),
                        row.getInt("body_length"),
                        row.getObject("created_at", OffsetDateTime.class),
                        row.getObject("comment_id", UUID.class))),
            args.toArray());
    int size = Math.min(queryPayload.limit(), rows.size());
    Optional<String> next =
        rows.size() > size
            ? Optional.of(rows.get(size - 1).cursor().encode(queryPayload))
            : Optional.empty();
    return new CommentPage(rows.subList(0, size).stream().map(ListedComment::view).toList(), next);
  }

  /**
   * Persists the validated comment and its image associations in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param command comment write parameters together with the authenticated request context
   * @return the published comment display projection
   */
  @Override
  public CommentView publish(TransactionContext transaction, PublishCommentCommand command) {
    var payload = command.payload();
    var subject = payload.subject();
    UUID shop =
        switch (subject.kind()) {
          case SHOP ->
              Jdbc.first(
                      transaction,
                      "SELECT shop_id FROM shops WHERE shop_id=?",
                      row -> row.getObject(1, UUID.class),
                      subject.targetId())
                  .orElseThrow(JdbcCommentRepository::missingSubject);
          case DISH ->
              Jdbc.first(
                      transaction,
                      "SELECT shop_id FROM dishes WHERE dish_id=?",
                      row -> row.getObject(1, UUID.class),
                      subject.targetId())
                  .orElseThrow(JdbcCommentRepository::missingSubject);
          case RIDER_SERVICE -> null;
        };
    if (subject.shopId().isPresent()
        && !subject.shopId().orElseThrow().equals(shop)
        && subject.kind() != SubjectKind.RIDER_SERVICE) {
      throw new IllegalArgumentException("Comment shop does not match the subject");
    }

    UUID id = UUID.randomUUID();
    UUID actor = command.context().principalId().value();
    Jdbc.update(
        transaction,
        """
        INSERT INTO comments (comment_id, target_kind, target_id, shop_id, order_id, author_id,
          author_label, body, body_length, image_count, purchase_mark, status, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?)
        """,
        id,
        subject.kind().name(),
        subject.targetId(),
        shop,
        subject.orderId().orElse(null),
        actor,
        label("Customer", actor),
        payload.text(),
        command.bodyLength(),
        payload.attachmentIds().size(),
        command.purchaseMark().name(),
        OffsetDateTime.now(ZoneOffset.UTC));
    for (int i = 0; i < payload.attachmentIds().size(); i++) {
      Jdbc.update(
          transaction,
          "INSERT INTO comment_images (comment_id, resource_id, position) VALUES (?, ?, ?)",
          id,
          UUID.fromString(payload.attachmentIds().get(i)),
          i);
    }
    return Jdbc.first(
            transaction,
            "SELECT * FROM comments WHERE comment_id=?",
            row -> readView(transaction, row),
            id)
        .orElseThrow();
  }

  /**
   * Updates the authored comment to its withdrawn state in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param command comment write parameters together with the authenticated request context
   * @return the resulting comment publication state
   */
  @Override
  public CommentStatus withdraw(TransactionContext transaction, WithdrawCommentCommand command) {
    UUID comment = command.payload().commentId();
    var access = lockComment(transaction, comment);
    if (!access.author().equals(command.context().principalId().value())) {
      throw new OperationFailure(
          ResultStatus.FORBIDDEN, "Only the author can withdraw the comment");
    }
    return changeStatus(transaction, comment, access.status(), CommentStatus.WITHDRAWN, null);
  }

  /**
   * Persists the authorized merchant reply in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param command comment write parameters together with the authenticated request context
   * @return the public display projection of the new reply
   */
  @Override
  public ReplyView reply(TransactionContext transaction, ReplyCommentCommand command) {
    UUID comment = command.payload().commentId();
    var access = lockComment(transaction, comment);
    UUID actor = command.context().principalId().value();
    if (access.shop() == null
        || Jdbc.first(
                transaction,
                "SELECT 1 FROM shops WHERE shop_id=? AND merchant_id=?",
                row -> row.getInt(1),
                access.shop(),
                actor)
            .isEmpty()) {
      throw new OperationFailure(ResultStatus.FORBIDDEN, "Comment is outside the merchant's shop");
    }
    requirePublished(access.status());
    var now = OffsetDateTime.now(ZoneOffset.UTC);
    ReplyView reply =
        new ReplyView(
            UUID.randomUUID(), label("Merchant", actor), command.payload().text(), now.toInstant());
    Jdbc.update(
        transaction,
        """
        INSERT INTO comment_replies (reply_id, comment_id, merchant_label, body, created_at)
        VALUES (?, ?, ?, ?, ?)
        """,
        reply.replyId(),
        comment,
        reply.merchantLabel(),
        reply.text(),
        now);
    return reply;
  }

  /**
   * Records platform moderation of the comment in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param command comment write parameters together with the authenticated request context
   * @return the resulting comment publication state
   */
  @Override
  public CommentStatus hide(TransactionContext transaction, HideCommentCommand command) {
    if (!command.context().verifiedPermissions().contains(Permission.HIDE_COMMENT)) {
      throw new OperationFailure(ResultStatus.FORBIDDEN, "Moderation permission required");
    }
    UUID comment = command.payload().commentId();
    return changeStatus(
        transaction,
        comment,
        lockComment(transaction, comment).status(),
        CommentStatus.HIDDEN_BY_PLATFORM,
        command.payload().reason());
  }

  private static OperationFailure missingSubject() {
    return new OperationFailure(ResultStatus.NOT_FOUND, "Comment subject not found");
  }

  private static String label(String role, UUID user) {
    return role + " " + user.toString().substring(0, 8);
  }

  private static CommentAccess lockComment(TransactionContext transaction, UUID id) {
    return Jdbc.first(
            transaction,
            "SELECT author_id, shop_id, status FROM comments WHERE comment_id=? FOR UPDATE",
            row ->
                new CommentAccess(
                    row.getObject(1, UUID.class),
                    row.getObject(2, UUID.class),
                    CommentStatus.valueOf(row.getString(3))),
            id)
        .orElseThrow(() -> new OperationFailure(ResultStatus.NOT_FOUND, "Comment not found"));
  }

  private static CommentStatus changeStatus(
      TransactionContext transaction,
      UUID id,
      CommentStatus previous,
      CommentStatus next,
      String reason) {
    requirePublished(previous);
    Jdbc.update(
        transaction,
        "UPDATE comments SET status=?, moderation_reason=? WHERE comment_id=?",
        next.name(),
        reason,
        id);
    return next;
  }

  private static void requirePublished(CommentStatus status) {
    if (status != CommentStatus.PUBLISHED) {
      throw new OperationFailure(ResultStatus.CONFLICT, "Comment is no longer published");
    }
  }

  private static CommentView readView(TransactionContext transaction, ResultSet row)
      throws SQLException {
    UUID id = row.getObject("comment_id", UUID.class);
    var subject =
        new CommentSubject(
            SubjectKind.valueOf(row.getString("target_kind")),
            row.getObject("target_id", UUID.class),
            Optional.ofNullable(row.getObject("shop_id", UUID.class)),
            Optional.ofNullable(row.getObject("order_id", UUID.class)));
    var images =
        Jdbc.query(
            transaction,
            """
            SELECT r.resource_id, r.media_type, r.content_length FROM comment_images i
            JOIN resources r ON r.resource_id=i.resource_id WHERE i.comment_id=? ORDER BY i.position
            """,
            image ->
                new RemoteResourceRef(
                    image.getObject(1).toString(), image.getString(2), image.getLong(3)),
            id);
    var replies =
        Jdbc.query(
            transaction,
            """
            SELECT reply_id, merchant_label, body, created_at FROM comment_replies
            WHERE comment_id=? ORDER BY created_at, reply_id
            """,
            reply ->
                new ReplyView(
                    reply.getObject(1, UUID.class),
                    reply.getString(2),
                    reply.getString(3),
                    reply.getObject(4, OffsetDateTime.class).toInstant()),
            id);
    return new CommentView(
        id,
        subject,
        row.getString("author_label"),
        row.getString("body"),
        PurchaseMark.valueOf(row.getString("purchase_mark")),
        CommentStatus.valueOf(row.getString("status")),
        row.getObject("created_at", OffsetDateTime.class).toInstant(),
        images,
        replies);
  }

  private record CommentAccess(UUID author, UUID shop, CommentStatus status) {}

  private record ListedComment(CommentView view, Cursor cursor) {}

  private record Cursor(int images, int length, OffsetDateTime createdAt, UUID id) {
    private String encode(ListCommentPayload query) {
      String data = scope(query) + "|" + images + "|" + length + "|" + createdAt + "|" + id;
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decode(ListCommentPayload query, String token) {
      try {
        String[] parts =
            new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8)
                .split("[|]", -1);
        if (parts.length != 5 || !scope(query).equals(parts[0])) {
          throw new IllegalArgumentException("Cursor belongs to a different query");
        }
        return new Cursor(
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            OffsetDateTime.parse(parts[3]),
            UUID.fromString(parts[4]));
      } catch (RuntimeException ex) {
        throw new IllegalArgumentException("Invalid comment cursor", ex);
      }
    }

    private static String scope(ListCommentPayload query) {
      var subject = query.subject();
      return query.sort()
          + ":"
          + subject.kind()
          + ":"
          + subject.targetId()
          + ":"
          + subject.shopId()
          + ":"
          + subject.orderId();
    }
  }
}
