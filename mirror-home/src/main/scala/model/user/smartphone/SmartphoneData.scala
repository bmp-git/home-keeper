package model.user.smartphone

case class SmartphoneData(id: String,
                          pictureUrl: String,
                          fullName: String,
                          nickname: String,
                          latitude: Double,
                          longitude: Double,
                          timestamp: Long,
                          accuracy: Int,
                          address: String,
                          countryCode: String,
                          charging: Boolean,
                          batteryLevel: Int)
