package hopsync.desktop;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Screens drive updates through the coordinator; only the composition root builds services.
 */
@AnalyzeClasses(packages = "hopsync.desktop", importOptions = ImportOption.DoNotIncludeTests.class)
class DesktopArchitectureTest {

    @ArchTest
    static final ArchRule uiDoesNotRunUseCasesDirectly = noClasses().that().resideInAPackage("hopsync.desktop.ui..")
        .should().dependOnClassesThat().resideInAnyPackage("hopsync.core.usecase..")
        .because("the UI starts, confirms and cancels updates through UpdateCoordinator");

    @ArchTest
    static final ArchRule onlyTheContainerBuildsAdapters = noClasses().that().resideOutsideOfPackage("hopsync.desktop")
        .should().callConstructor(hopsync.platform.http.DownloadService.class, hopsync.platform.http.NetworkConfig.class)
        .because("services are created once, in AppContainer");
}
