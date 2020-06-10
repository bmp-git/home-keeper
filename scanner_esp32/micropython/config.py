import network
import binascii


WIFI_SSID = 'Home-Keeper'
WIFI_PASSWORD = '8CUAgjwyuaJu'

BLE_INTERVAL_US = 1000000
BLE_WINDOW_US = 500000
BLE_RX_BUFFER = 1024

MQTT_CLIENT_ID = 'umqtt_client'
MQTT_SERVER_ADDRESS = '10.0.0.2'
MQTT_PORT = 1883
MQTT_USER = b'homekeeper'
MQTT_PASSWORD = b'8CUAgjwyuaJu'
MQTT_KEEPALIVE = 30

MQTT_ONLINE_STATUS_PAYLOAD = 'Online'
MQTT_OFFLINE_STATUS_PAYLOAD = 'Offline'

RF_433_RX_GPIO = 36

RF_433_RX_QUEUE_SIZE = 10
BLE_SCAN_RESULT_QUEUE_SIZE = 10

ROOT_PUBLISH_TOPIC = 'scanner/' + binascii.hexlify(network.WLAN(network.STA_IF).config('mac')).decode()
BLE_PUBLISH_TOPIC = ROOT_PUBLISH_TOPIC + '/ble'
RF433_PUBLISH_TOPIC = ROOT_PUBLISH_TOPIC + '/433'
WIFI_PUBLISH_TOPIC = ROOT_PUBLISH_TOPIC + '/wifi'
STATUS_PUBLISH_TOPIC = ROOT_PUBLISH_TOPIC + '/status'