package br.com.garage;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "br.com.garage", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] FRAMEWORKS = {
        "org.springframework..", "jakarta..", "java.sql..", "javax.sql..", "tools.jackson..", "com.fasterxml..",
        "io.micrometer..", "org.slf4j..", "com.zaxxer.."
    };

    @ArchTest
    static final ArchRule dominioNaoConheceFrameworkNemCamadasExternas = noClasses()
            .that()
            .resideInAPackage("br.com.garage.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FRAMEWORKS)
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "br.com.garage.application..",
                    "br.com.garage.inbound..",
                    "br.com.garage.outbound..",
                    "br.com.garage.config..",
                    "br.com.garage.observability..");

    @ArchTest
    static final ArchRule aplicacaoNaoConheceFrameworkNemAdapters = noClasses()
            .that()
            .resideInAPackage("br.com.garage.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FRAMEWORKS)
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "br.com.garage.inbound..",
                    "br.com.garage.outbound..",
                    "br.com.garage.config..",
                    "br.com.garage.observability..");

    @ArchTest
    static final ArchRule casosDeUsoSoDependemDePortasEDominio = noClasses()
            .that()
            .resideInAPackage("br.com.garage.application.usecase..")
            .should()
            .dependOnClassesThat()
            .resideOutsideOfPackages(
                    "br.com.garage.application..", "br.com.garage.domain..", "java..");

    @ArchTest
    static final ArchRule adaptersDeEntradaNaoConhecemAdaptersDeSaida = noClasses()
            .that()
            .resideInAPackage("br.com.garage.inbound..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("br.com.garage.outbound..");

    @ArchTest
    static final ArchRule adaptersDeSaidaNaoConhecemAdaptersDeEntrada = noClasses()
            .that()
            .resideInAPackage("br.com.garage.outbound..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("br.com.garage.inbound..", "br.com.garage.observability..");

    @ArchTest
    static final ArchRule ninguemDependeDaConfiguracaoDeMontagem = noClasses()
            .that()
            .resideOutsideOfPackage("br.com.garage.config..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("br.com.garage.config..");

    @ArchTest
    static final ArchRule persistenciaEOUnicoLugarComSql = noClasses()
            .that()
            .resideOutsideOfPackages(
                    "br.com.garage.outbound.persistence..", "br.com.garage.observability..", "br.com.garage.config..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.jdbc..");
}
