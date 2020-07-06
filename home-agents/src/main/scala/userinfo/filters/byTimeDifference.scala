package userinfo.filters

import jason.asSyntax.{Literal, NumberTerm, Term}
import utils.filter

private class byTimeDifference extends filter {
  override def predicate(args: Array[Term]): Term => Boolean = t => {
    val now = args(2).asInstanceOf[NumberTerm].solve()
    val timeDifference = args(3).asInstanceOf[NumberTerm].solve()
    val infoTime = t.asInstanceOf[Literal].getTerm(1).asInstanceOf[NumberTerm].solve()
    now - infoTime < timeDifference
  }
}
