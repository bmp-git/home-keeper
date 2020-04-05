import bluetooth
import binascii
import mqtt
from config import BLE_PUBLISH_TOPIC
from micropython import const
import uasyncio as asyncio

_IRQ_SCAN_RESULT = const(1 << 4)


def bths(array):
    return binascii.hexlify(array)

def bt_irq(event, data):
    if event == _IRQ_SCAN_RESULT:
        addr_type, addr, adv_type, rssi, adv_data = data
        print(addr_type, bths(addr), adv_type, rssi, bths(adv_data))
        mqtt.mqtt_publish(BLE_PUBLISH_TOPIC, bths(adv_data))
        #if bths(addr) == b'74daeaac2a2d':
            #mqtt_client.publish(topic="esp32scanner/", msg=bths(adv_data), retain=False, qos=0)

async def ble_scan_start():
    ble = bluetooth.BLE()
    ble.irq(bt_irq)
    ble.active(True)
    print('BLE address:', ble.config('mac'))
    ble.gap_scan(0, 250, 200)

