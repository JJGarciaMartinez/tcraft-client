package hopsync.platform;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Adapters talk to the outside world (network, disk, OS) but never to the user interface.
 */
@AnalyzeClasses(packages = "hopsync.platform", importOptions = ImportOption.DoNotIncludeTests.class)
class PlatformArchitectureTest {

    @ArchTest
    static final ArchRule noUserInterface = noClasses().should().dependOnClassesThat()
        .resideInAnyPackage("java.awt..", "javax.swing..", "hopsync.desktop..");
}
