import bluetooth
import binascii

ble = bluetooth.BLE()
ble.active(True)

WIFI_SSID = 'Home-Keeper'
WIFI_PASSWORD = '8CUAgjwyuaJu'

MQTT_CLIENT_ID = 'umqtt_client'
MQTT_SERVER_ADDRESS = '192.168.30.250'
MQTT_PORT = 1883
MQTT_USER = b'homekeeper'
MQTT_PASSWORD = b'8CUAgjwyuaJu'
MQTT_KEEPALIVE = 30


BLE_PUBLISH_TOPIC = 'scanner/' + binascii.hexlify(ble.config('mac')).decode() + '/BLE'
