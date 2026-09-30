package com.ryanm.fraudruleengine.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static final String BASE = "com.ryanm.fraudruleengine";
    private final JavaClasses imported = new ClassFileImporter().importPackages(BASE);

    @Test
    void controllersShouldOnlyDependOnServices() {
        final ArchRule rule = classes()
                .that().resideInAPackage(BASE + ".controller..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".controller..",
                        BASE + ".service..",
                        BASE + ".exception..",
                        BASE + ".type..",
                        "io.swagger..",
                        "org.springframework..",
                        "jakarta..",
                        "java..");
        rule.check(imported);
    }

    @Test
    void servicesShouldNotDependOnControllers() {
        final ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".service..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE + ".controller..");
        rule.check(imported);
    }

    @Test
    void persistenceLayerShouldNotDependOnServices() {
        final ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".persistence..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE + ".service..");
        rule.check(imported);
    }

    @Test
    void ruleImplementationsShouldImplementFraudRuleInterface() {
        final ArchRule rule = classes()
                .that().resideInAPackage(BASE + ".rule.impl")
                .should().implement(com.ryanm.fraudruleengine.rule.FraudRule.class);
        rule.check(imported);
    }

    @Test
    void repositoriesShouldResideInPersistencePackage() {
        final ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().resideInAPackage(BASE + ".persistence.repository");
        rule.check(imported);
    }

    @Test
    void entitiesShouldResideInPersistencePackage() {
        final ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Entity")
                .should().resideInAPackage(BASE + ".persistence.entity");
        rule.check(imported);
    }
}
