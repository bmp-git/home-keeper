package userinfo.reducers

import jason.asSyntax.{Literal, NumberTerm, Term}

class maxDate extends reduce {
  override def reducer(args: Array[Term]): (Term, Term) => Term = (t1, t2) => {
    val date1 = t1.asInstanceOf[Literal].getTerm(1).asInstanceOf[NumberTerm].solve()
    val date2 = t2.asInstanceOf[Literal].getTerm(1).asInstanceOf[NumberTerm].solve()
    if (date1 > date2) t1 else t2
  }
}
