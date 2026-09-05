package com.apptolast.organization;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test
  void hexagonalBoundariesAndInputPort() {
    var classes =
        new ClassFileImporter()
            .withImportOption(
                com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.apptolast.organization");
    classes()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage("java..", "..domain..")
        .check(classes);
    classes()
        .that()
        .resideInAPackage("..application..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage("java..", "..domain..", "..application..")
        .check(classes);
    noClasses()
        .that()
        .resideInAPackage("..adapter.http..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("com.apptolast.organization.application.CreateProject")
        .check(classes);
  }
}
