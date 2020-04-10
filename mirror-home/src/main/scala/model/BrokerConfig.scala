package model

case class BrokerConfig(address: String, auth: Option[(String, String)] = None) {
  def withAuth(auth: Option[(String, String)]): BrokerConfig = this.copy(auth = auth)
}
