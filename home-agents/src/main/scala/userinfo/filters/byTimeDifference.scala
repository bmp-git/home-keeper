package userinfo.filters

import jason.asSyntax.{Literal, NumberTerm, Term}
import utils.filter

private class byTimeDifference extends filter {
  override def predicate(args: Array[Term]): Term => Boolean = t => {
    val timeDifference = args(2).asInstanceOf[NumberTerm].solve()
    val infoTime = t.asInstanceOf[Literal].getTerm(1).asInstanceOf[NumberTerm].solve()
    System.currentTimeMillis() - infoTime < timeDifference
  }
}
