package userinfo.reducers

import jason.asSyntax.{Literal, NumberTerm, Term}

class maxRssi extends reduce {
  override def reducer(args: Array[Term]): (Term, Term) => Term = (t1, t2) => {
    val rssi1 = t1.asInstanceOf[Literal].getTerm(2).asInstanceOf[NumberTerm].solve()
    val rssi2 = t2.asInstanceOf[Literal].getTerm(2).asInstanceOf[NumberTerm].solve()
    if (rssi1 > rssi2) t1 else t2
  }
}
