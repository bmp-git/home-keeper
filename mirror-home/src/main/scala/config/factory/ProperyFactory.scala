package config.factory

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import model.Property


trait PropertyFactory[I, O] extends OneTimeFactory[Property[O]] {
  def actorSystem: ActorSystem

  def name: String

  def input: Source[I, NotUsed]

  def map(i: I): O

  def errors: Source[Exception, NotUsed]

  def outs: Source[Either[O, Exception], NotUsed] = {
    val o = output.map(v => Left(v))
    val e = errors.map(v => Right(v))
    e.merge(o)
  }

  def output: Source[O, NotUsed] = input.map(map)

  override def oneTimeBuild(): Property[O] = new Property[O] {
    implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
    private var v: O = _
    output.runForeach(x => v = x)

    override def name: String = PropertyFactory.this.name

    override def value: O = v
  }
}

trait StaticPropertyFactory[O] extends PropertyFactory[O, O] {
  def map(i: O): O = i
}
