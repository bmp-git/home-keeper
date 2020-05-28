package utils

import jason.asSemantics.{DefaultInternalAction, TransitionSystem, Unifier}
import jason.asSyntax.{ListTerm, ListTermImpl, Term}

abstract class filter extends DefaultInternalAction {

  def predicate(args: Array[Term]): Term => Boolean

  override def execute(ts: TransitionSystem, un: Unifier, args: Array[Term]): Object = {
    val list = args(0).asInstanceOf[ListTerm]
    val terms = new ListTermImpl()

    list.forEach(t => {
      if (predicate(args)(t)) {
        terms.add(t)
      }
    })
    Boolean.box(un.unifies(args(1), terms))

  }
}
