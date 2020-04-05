import bluetooth
import binascii
from micropython import const
from umqtt.robust import MQTTClient

_IRQ_SCAN_RESULT = const(1 << 4)

mqtt_client = MQTTClient(client_id="umqtt_client", server="192.168.30.250", user=b'homekeeper', password=b'8CUAgjwyuaJu', keepalive=10)

def bths(array):
    return binascii.hexlify(array)

def bt_irq(event, data):
    if event == _IRQ_SCAN_RESULT:
        addr_type, addr, adv_type, rssi, adv_data = data
        if bths(addr) == b'74daeaac2a2d':
            print(addr_type, bths(addr), adv_type, rssi, bths(adv_data))
            mqtt_client.publish(topic="esp32scanner/", msg=bths(adv_data), retain=False, qos=0)


def mqtt_connect():
    print("Mqtt connecting...")
    mqtt_client.DEBUG = True
    mqtt_client.connect(clean_session=True)

def ble_scan_start():
    mqtt_connect()
    ble = bluetooth.BLE()
    ble.irq(bt_irq)
    ble.active(True)
    ble.gap_scan(0, 250, 200)

