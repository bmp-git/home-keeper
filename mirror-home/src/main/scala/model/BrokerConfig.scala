package model

case class BrokerConfig(address: String, auth: Option[(String, String)] = None) {
  def auth(username: String, password: String): BrokerConfig = this.copy(auth = Some((username, password)))
}
