package at.forsyte.apalache.tla.script.pass

import at.forsyte.apalache.infra.ExceptionAdapter
import at.forsyte.apalache.infra.passes._
import at.forsyte.apalache.io.annotations.store.AnnotationStore
import at.forsyte.apalache.io.annotations.{AnnotationStoreProvider, PrettyWriterWithAnnotationsFactory}
import at.forsyte.apalache.tla.bmcmt.analyses._
import at.forsyte.apalache.tla.bmcmt.config.{CheckerExceptionAdapter, TransformationTrackerProvider}
import at.forsyte.apalache.tla.imp.passes.{SanyParserPass, SanyParserPassImpl}
import at.forsyte.apalache.tla.lir.io.TlaWriterFactory
import at.forsyte.apalache.tla.lir.storage.ChangeListener
import at.forsyte.apalache.tla.lir.transformations.{TransformationListener, TransformationTracker}
import at.forsyte.apalache.tla.typecheck.passes.EtcTypeCheckerPassImpl
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, TypeLiteral}

/**
 * This is a module for the scripting module.
 * If you are not sure how the binding works, check the tutorial on Google Guice.
 *
 * @author Igor Konnov
 */
class ScriptConfigurationModule extends AbstractModule {
  override def configure(): Unit = {
    // the options singleton
    bind(classOf[PassOptions])
      .to(classOf[WriteablePassOptions])
    // exception handler
    bind(classOf[ExceptionAdapter])
      .to(classOf[CheckerExceptionAdapter])

    // stores
    // Create an annotation store with the custom provider.
    // We have to use TypeLiteral, as otherwise Guice is getting confused by type erasure.
    bind(new TypeLiteral[AnnotationStore]() {})
      .toProvider(classOf[AnnotationStoreProvider])
    bind(classOf[FormulaHintsStore])
      .to(classOf[FormulaHintsStoreImpl])
    bind(classOf[ExprGradeStore])
      .to(classOf[ExprGradeStoreImpl])

    // writers
    bind(classOf[TlaWriterFactory])
      .to(classOf[PrettyWriterWithAnnotationsFactory])

    // transformation tracking
    // TODO: the binding of TransformationListener should disappear in the future
    bind(classOf[TransformationListener])
      .to(classOf[ChangeListener])
    // check TransformationTrackerProvider to find out which listeners the tracker is using
    bind(classOf[TransformationTracker])
      .toProvider(classOf[TransformationTrackerProvider])

    // SanyParserPassImpl is the default implementation of SanyParserPass
    bind(classOf[SanyParserPass])
      .to(classOf[SanyParserPassImpl])
    // and it also the initial pass for PassChainExecutor
    bind(classOf[Pass])
      .annotatedWith(Names.named("InitialPass"))
      .to(classOf[SanyParserPass])

    // The next pass is Snowcat that is called EtcTypeCheckerPassImpl for now.
    // We provide guice with a concrete implementation here, as we also use PostTypeCheckerPassImpl later in the pipeline.
    bind(classOf[Pass])
      .annotatedWith(Names.named("AfterParser"))
      .to(classOf[EtcTypeCheckerPassImpl])

    // the next pass is ConfigurationPass
    bind(classOf[ScriptConfigurationPass])
      .to(classOf[ScriptConfigurationPassImpl])
    bind(classOf[Pass])
      .annotatedWith(Names.named("AfterTypeChecker"))
      .to(classOf[ScriptConfigurationPass])

    // the final pass is TerminalPass
    bind(classOf[Pass])
      .annotatedWith(Names.named("AfterScriptConfiguration"))
      .to(classOf[TerminalPass])
  }

}
