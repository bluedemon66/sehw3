package edu.nju.sehw.takeout.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nju.sehw.takeout.model.comment.CommentOperation;
import edu.nju.sehw.takeout.model.common.Permission;
import edu.nju.sehw.takeout.model.common.PermissionSet;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.network.AccessToken;
import edu.nju.sehw.takeout.model.network.SecureRequest;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceKind;
import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import edu.nju.sehw.takeout.server.operation.ParameterSchema;
import edu.nju.sehw.takeout.server.operation.PreparedOperation;
import edu.nju.sehw.takeout.server.operation.ServerOperationCatalog;
import edu.nju.sehw.takeout.server.operation.TransactionPolicy;
import edu.nju.sehw.takeout.server.task.VerifiedTask;
import edu.nju.sehw.takeout.server.task.VerifiedUpload;
import edu.nju.sehw.takeout.testsupport.Fixtures;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProtocolContractsTest {
  @Test
  void resourceBytesAreIsolatedAtBothBoundaries() {
    byte[] bytes = {1, 2};
    ResourceBlob resource =
        new ResourceBlob(new RemoteResourceRef("image-1", "image/png", 2), bytes);
    bytes[0] = 9;
    byte[] returned = resource.content();
    returned[1] = 8;
    assertArrayEquals(new byte[] {1, 2}, resource.content());
  }

  @Test
  void envelopeDigestIsIsolatedAndRequestIdentityMustMatch() {
    UUID requestId = UUID.randomUUID();
    Message message = Fixtures.message(requestId, 1, 2);
    byte[] digest = new byte[32];
    SecureRequest request =
        new SecureRequest(
            requestId, new AccessToken("test-only-token"), Instant.EPOCH, digest, message);
    digest[0] = 9;
    request.bodyDigest()[1] = 8;
    assertArrayEquals(new byte[32], request.bodyDigest());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SecureRequest(
                UUID.randomUUID(),
                new AccessToken("test-only-token"),
                Instant.EPOCH,
                new byte[32],
                message));
  }

  @Test
  void logRepresentationRedactsCredentials() {
    AccessToken token = new AccessToken("test-only-secret-value");
    assertFalse(token.toString().contains(token.value()));
  }

  @Test
  void callerCannotMutateVerifiedPermissionSnapshot() {
    Set<Permission> mutable = new HashSet<>(Set.of(Permission.READ_COMMENT));
    PermissionSet permissions = new PermissionSet(mutable);
    mutable.add(Permission.HIDE_COMMENT);
    assertFalse(permissions.contains(Permission.HIDE_COMMENT));
    assertThrows(
        UnsupportedOperationException.class,
        () -> permissions.values().add(Permission.HIDE_COMMENT));
  }

  @Test
  void mismatchedTaskContextIsRejected() {
    VerifiedTask task = Fixtures.task(UUID.randomUUID());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new VerifiedTask(
                task.context(),
                Fixtures.message(UUID.randomUUID(), 1, 2),
                task.replyChannel(),
                Optional.empty()));
  }

  @Test
  void anUploadFromAnotherAuthenticatedRequestCannotBeAttached() {
    VerifiedTask current = Fixtures.task(UUID.randomUUID());
    VerifiedTask other = Fixtures.task(UUID.randomUUID());
    VerifiedUpload upload =
        new VerifiedUpload(other.context(), Path.of("test-staging"), "image/png", 1, Instant.EPOCH);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new VerifiedTask(
                current.context(), current.message(), current.replyChannel(), Optional.of(upload)));
  }

  @Test
  void layoutIsTheFirstManifestResource() {
    ResourceDescriptor image =
        new ResourceDescriptor(
            1,
            Optional.of(new LocalResourceRef(List.of(0), 1, ResourceKind.IMAGE)),
            Optional.empty(),
            ResourceKind.IMAGE);
    assertThrows(IllegalArgumentException.class, () -> new PageResourceManifest(List.of(image)));
    ResourceDescriptor layout =
        new ResourceDescriptor(
            0,
            Optional.of(new LocalResourceRef(List.of(0), 0, ResourceKind.UI_LAYOUT)),
            Optional.empty(),
            ResourceKind.UI_LAYOUT);
    assertEquals(layout, new PageResourceManifest(List.of(layout, image)).uiLayout());
  }

  @Test
  void operationNumbersAreUniqueAndPreserveTheLabTwoCommentProtocol() {
    assertEquals(
        OperationCode.values().length,
        Arrays.stream(OperationCode.values()).map(OperationCode::code).distinct().count());
    assertEquals(
        List.of(4101, 4102, 4103, 4104, 4105, 4106),
        Arrays.stream(CommentOperation.values()).map(CommentOperation::code).toList());
    assertThrows(IllegalArgumentException.class, () -> OperationCode.fromCode(-1));
  }

  @Test
  void catalogRejectsDuplicatesAndExposesAnExplicitUnknownOperation() {
    PreparedOperation definition =
        new PreparedOperation(
            4101,
            Permission.READ_COMMENT,
            new ParameterSchema(Map.of(), Set.of()),
            TransactionPolicy.READ_ONLY);
    assertThrows(
        IllegalArgumentException.class,
        () -> new ServerOperationCatalog(List.of(definition, definition)));
    ServerOperationCatalog catalog = new ServerOperationCatalog(List.of(definition));
    assertEquals(definition, catalog.find(4101).orElseThrow());
    assertTrue(catalog.find(9999).isEmpty());
  }

  @Test
  void schemaRequiresATypeDefinitionForEachRequiredField() {
    assertThrows(
        IllegalArgumentException.class, () -> new ParameterSchema(Map.of(), Set.of("orderId")));
  }

  @Test
  void manifestRejectsAmbiguousElementBindings() {
    ResourceDescriptor layout =
        new ResourceDescriptor(
            0,
            Optional.of(new LocalResourceRef(List.of(0), 0, ResourceKind.UI_LAYOUT)),
            Optional.empty(),
            ResourceKind.UI_LAYOUT);
    assertThrows(
        IllegalArgumentException.class, () -> new PageResourceManifest(List.of(layout, layout)));
  }
}
