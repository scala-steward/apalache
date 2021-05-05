package at.forsyte.apalache.tla.script

sealed trait RequireEnsureKind {
  val name: String
}

case class RequireConst(name: String) extends RequireEnsureKind

case class RequireState(name: String) extends RequireEnsureKind

case class RequireTemporal(name: String) extends RequireEnsureKind

case class EnsureAction(name: String) extends RequireEnsureKind

case class EnsureTemporal(name: String) extends RequireEnsureKind
