import bluetooth
import binascii
import mqtt
import ujson
from config import BLE_PUBLISH_TOPIC, BLE_INTERVAL_US, BLE_WINDOW_US
from micropython import const
import uasyncio as asyncio

_IRQ_SCAN_RESULT = const(1 << 4)


def bths(array):
    return binascii.hexlify(array)

async def publish_task(data):
    addr_type, addr, adv_type, rssi, adv_data = data
    print(addr_type, bths(addr), adv_type, rssi, bths(adv_data))
    payload = {
        'addr' : bths(addr).decode(),
        'rssi' : rssi,
        'adv_data' : bths(adv_data).decode()
    }
    await mqtt.mqtt_publish(BLE_PUBLISH_TOPIC, ujson.dumps(payload))

def bt_irq(event, data):
    if event == _IRQ_SCAN_RESULT:
        asyncio.get_event_loop().create_task(publish_task(data))

async def ble_scan_start():
    ble = bluetooth.BLE()
    ble.irq(bt_irq)
    ble.active(True)
    print('BLE address:', ble.config('mac'))
    ble.gap_scan(0, BLE_INTERVAL_US, BLE_WINDOW_US)

