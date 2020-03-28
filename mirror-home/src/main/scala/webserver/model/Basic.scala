package webserver.model

import spray.json.DefaultJsonProtocol

// domain model
final case class LoginRequest(name:String, password:String)
final case class LoginResponse(token:String)
final case class JwtClaimData(user: String)

final case class Item(name: String, id: Long)
final case class Order(items: List[Item])

// formats for unmarshalling and marshalling
object ModelJsonProtocol extends DefaultJsonProtocol {
  implicit val credentialsFormat = jsonFormat2(LoginRequest)
  implicit val jwtClaimDataFormat = jsonFormat1(JwtClaimData)
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)
  implicit val postResultString = jsonFormat1(LoginResponse)
}