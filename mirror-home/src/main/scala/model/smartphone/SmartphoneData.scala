package model.smartphone

case class SmartphoneData(id: String,
                          picture_url: String,
                          full_name: String,
                          nickname: String,
                          latitude: Double,
                          longitude: Double,
                          timestamp: Long,
                          accuracy: Int,
                          address: String,
                          country_code: String,
                          charging: Boolean,
                          battery_level: Int)
