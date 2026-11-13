package hopsync.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The core holds the domain and must stay usable from any front end (desktop today, a CLI later).
 */
@AnalyzeClasses(packages = "hopsync.core", importOptions = ImportOption.DoNotIncludeTests.class)
class CoreArchitectureTest {

    @ArchTest
    static final ArchRule noUserInterface = noClasses().should().dependOnClassesThat()
        .resideInAnyPackage("java.awt..", "javax.swing..")
        .because("the core is shared by every front end");

    @ArchTest
    static final ArchRule noHttpClient = noClasses().should().dependOnClassesThat()
        .resideInAnyPackage("java.net.http..")
        .orShould().dependOnClassesThat().haveFullyQualifiedName("java.net.HttpURLConnection")
        .because("network access goes through ports implemented in the platform module");

    @ArchTest
    static final ArchRule noOuterModules = noClasses().should().dependOnClassesThat()
        .resideInAnyPackage("hopsync.platform..", "hopsync.desktop..");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes().that().resideInAPackage("hopsync.core.port..")
        .should().beInterfaces();
}
