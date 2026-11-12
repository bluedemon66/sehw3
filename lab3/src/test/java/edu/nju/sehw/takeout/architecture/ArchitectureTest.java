package edu.nju.sehw.takeout.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import edu.nju.sehw.takeout.client.core.CoreEndpoint;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private static final JavaClasses PRODUCTION =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("edu.nju.sehw.takeout");

  @Test
  void modelContractsRemainIndependentOfClientAndServer() {
    noClasses()
        .that()
        .resideInAPackage("..model..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..client..", "..server..", "..scaffold..")
        .check(PRODUCTION);
  }

  @Test
  void clientUsesProtocolContractsToReachTheServer() {
    noClasses()
        .that()
        .resideInAPackage("..client..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..server..")
        .check(PRODUCTION);
  }

  @Test
  void serverUsesProtocolContractsToReplyToTheClient() {
    noClasses()
        .that()
        .resideInAPackage("..server..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..client..")
        .check(PRODUCTION);
  }

  @Test
  void coreStaysIndependentOfBusinessStrategies() {
    noClasses()
        .that()
        .resideInAPackage("..client.core..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..client.processing..",
            "..client.logic..",
            "..client.query..",
            "..client.comment..",
            "..server..")
        .check(PRODUCTION);
  }

  @Test
  void productionEndpointsShareTheSnapshotAndSendContract() {
    classes()
        .that()
        .implement(CoreEndpoint.class)
        .should()
        .beAssignableTo(MessageParticipant.class)
        .check(PRODUCTION);
  }
}
