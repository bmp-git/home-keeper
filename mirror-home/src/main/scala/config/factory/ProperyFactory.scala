package config.factory

trait StaticPropertyFactory[O]  {
  def name:String
  def output: Iterable[O]
  def errors: Iterable[Exception]
  def outs:Iterable[Either[O,Exception]] = ??? //merge output and errors
}
trait DynamicPropertyFactory[I,O] extends StaticPropertyFactory[O]{
  def map(i:I):O
  def input: Iterable[I]
  def output: Iterable[O] = input.map(map)
}
