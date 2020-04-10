import bluetooth
import binascii
import mqtt
import ujson
from config import BLE_PUBLISH_TOPIC, BLE_INTERVAL_US, BLE_WINDOW_US, BLE_SCAN_RESULT_QUEUE_SIZE, BLE_RX_BUFFER
from micropython import const
import uasyncio as asyncio
from irqueue.irqueue import IRQueue
from irqueue.ble.ble_scan_record import BLEScanRecord

_IRQ_SCAN_RESULT = const(1 << 4)
_IRQ_SCAN_COMPLETE = const(1 << 5)

scan_result_queue = None

loop = asyncio.get_event_loop()

def bths(array):
    return binascii.hexlify(array)

def bt_irq(event, data):
    if event == _IRQ_SCAN_RESULT:
        global scan_result_queue
        addr_type, addr, adv_type, rssi, adv_data = data
        scan_result_queue.enqueue(addr, adv_data, rssi)
    elif event == _IRQ_SCAN_COMPLETE:
        print("Scan completed!!")

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
            print("[BLE] Found:", payload['addr'], payload['rssi'], payload['adv_data'])
            await mqtt.mqtt_publish(BLE_PUBLISH_TOPIC, ujson.dumps(payload))
            await asyncio.sleep_ms(0)
        else:
            await asyncio.sleep_ms(50)

async def ble_scan_start(loop):
    global scan_result_queue
    scan_result_queue = IRQueue(BLEScanRecord, BLE_SCAN_RESULT_QUEUE_SIZE)
    ble = bluetooth.BLE()
    ble.config(rxbuf=BLE_RX_BUFFER)
    ble.irq(bt_irq)
    ble.active(True)
    print('[BLE] rxbuf:', ble.config('rxbuf'))
    ble.gap_scan(0, BLE_INTERVAL_US, BLE_WINDOW_US)
    loop.create_task(ble_mqtt_publisher())

