import bluetooth
import binascii
import mqtt
import ujson
from config import BLE_PUBLISH_TOPIC, BLE_INTERVAL_US, BLE_WINDOW_US
from micropython import const
import uasyncio as asyncio
import gc
from utils.irqueue import IRQueue
from utils.ble_scan_record import BLEScanRecord

_IRQ_SCAN_RESULT = const(1 << 4)

scan_result_queue = None

loop = asyncio.get_event_loop()

def bths(array):
    return binascii.hexlify(array)

def bt_irq(event, data):
    global scan_result_queue
    addr_type, addr, adv_type, rssi, adv_data = data
    scan_result_queue.enqueue(addr, adv_data, rssi)
    print("INT:", addr_type, bths(addr), adv_type, rssi, bths(adv_data))

async def ble_mqtt_publisher():
    global scan_result_queue
    item = BLEScanRecord()
    while True:
        item_present = scan_result_queue.dequeue(item)
        if item_present:
            payload = {
                'addr' : bths(item.addr).decode(),
                'rssi' : item.rssi,
                'adv_data' : bths(item.adv_data).decode()
            }
            await mqtt.mqtt_publish(BLE_PUBLISH_TOPIC, ujson.dumps(payload))
            await asyncio.sleep_ms(0)
        else:
            await asyncio.sleep_ms(50)

async def ble_scan_start(loop):
    global scan_result_queue
    scan_result_queue = IRQueue(BLEScanRecord, 10)
    ble = bluetooth.BLE()
    ble.irq(bt_irq, _IRQ_SCAN_RESULT)
    ble.active(True)
    print('BLE address:', ble.config('mac'))
    ble.gap_scan(0, BLE_INTERVAL_US, BLE_WINDOW_US)
    loop.create_task(ble_mqtt_publisher())

