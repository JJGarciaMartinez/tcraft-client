package hopsync.desktop;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * One-way data flow: screens dispatch actions and render state, only effects call the core, the reducer
 * stays pure, and only the composition root builds services.
 */
@AnalyzeClasses(packages = "hopsync.desktop", importOptions = ImportOption.DoNotIncludeTests.class)
class DesktopArchitectureTest {

    @ArchTest
    static final ArchRule screensOnlyTalkToTheStore = noClasses().that().resideInAPackage("hopsync.desktop.ui..")
        .should().dependOnClassesThat().resideInAnyPackage("hopsync.core.session..", "hopsync.core.usecase..",
            "hopsync.desktop.effects..")
        .because("screens dispatch actions; only Effects starts, confirms and cancels updates");

    @ArchTest
    static final ArchRule stateDoesNotKnowSwing = noClasses().that().resideInAPackage("hopsync.desktop.store..")
        .and().doNotHaveSimpleName("Store")
        .should().dependOnClassesThat().resideInAnyPackage("javax.swing..", "java.awt..")
        .because("the reducer and the UI state are pure and tested without a display");

    @ArchTest
    static final ArchRule storeDoesNotCallTheCore = noClasses().that().resideInAPackage("hopsync.desktop.store..")
        .should().dependOnClassesThat().resideInAnyPackage("hopsync.core.session..", "hopsync.core.usecase..",
            "hopsync.desktop.effects..", "hopsync.desktop.ui..")
        .because("side effects belong to Effects, drawing to the screens");

    @ArchTest
    static final ArchRule onlyTheContainerBuildsAdapters = noClasses().that().resideOutsideOfPackage("hopsync.desktop")
        .should().callConstructor(hopsync.platform.http.DownloadService.class, hopsync.platform.http.NetworkConfig.class)
        .because("services are created once, in AppContainer");
}
