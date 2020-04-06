import network
import binascii


WIFI_SSID = 'Home-Keeper'
WIFI_PASSWORD = '8CUAgjwyuaJu'

BLE_INTERVAL_US = 1000000
BLE_WINDOW_US = 500000

MQTT_CLIENT_ID = 'umqtt_client'
MQTT_SERVER_ADDRESS = '192.168.30.250'
MQTT_PORT = 1883
MQTT_USER = b'homekeeper'
MQTT_PASSWORD = b'8CUAgjwyuaJu'
MQTT_KEEPALIVE = 30


ROOT_PUBLISH_TOPIC = 'scanner/' + binascii.hexlify(network.WLAN(network.STA_IF).config('mac')).decode()
BLE_PUBLISH_TOPIC = ROOT_PUBLISH_TOPIC + '/ble'
RF433_PUBLISH_TOPIC = ROOT_PUBLISH_TOPIC + '/433'