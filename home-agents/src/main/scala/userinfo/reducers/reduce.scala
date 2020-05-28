package userinfo.reducers

import jason.asSemantics.{DefaultInternalAction, TransitionSystem, Unifier}
import jason.asSyntax.{ListTerm, ListTermImpl, LiteralImpl, Term}

abstract class reduce extends DefaultInternalAction {

  def reducer(args: Array[Term]): (Term, Term) => Term

  override def execute(ts: TransitionSystem, un: Unifier, args: Array[Term]): Object = {
    val list = args(0).asInstanceOf[ListTerm]
    if (list.isEmpty) {
      return Boolean.box(false)
    }
    val out: Term = list.toArray.map(l => l.asInstanceOf[Term]).reduce(reducer(args))
    Boolean.box(un.unifies(args(1), out))
  }
}
