import bluetooth
import binascii
import wifi
import mqtt
import ujson
from config import BLE_PUBLISH_TOPIC, BLE_INTERVAL_US, BLE_WINDOW_US, BLE_SCAN_RESULT_QUEUE_SIZE, BLE_RX_BUFFER
from micropython import const
from micropython import schedule
import uasyncio as asyncio
from irqueue.irqueue import IRQueue
from irqueue.ble.ble_scan_record import BLEScanRecord

_IRQ_SCAN_RESULT = const(1 << 4)
_IRQ_SCAN_COMPLETE = const(1 << 5)

scan_result_queue = None
ble = bluetooth.BLE()

def bths(array):
    return binascii.hexlify(array)

def bt_irq(event, data):
    if event == _IRQ_SCAN_RESULT:
        global scan_result_queue
        addr_type, addr, adv_type, rssi, adv_data = data
        #print("[BLE IRQ]", addr_type, addr, adv_type, rssi, adv_data)
        scan_result_queue.enqueue(addr, adv_data, rssi)
    #elif event == _IRQ_SCAN_COMPLETE:
    #    print("[BLE] Scan completed. Restarting.")
    #    schedule(ble_scan_restart, None)


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

def ble_setup(loop):
    global scan_result_queue
    global ble
    scan_result_queue = IRQueue(BLEScanRecord, BLE_SCAN_RESULT_QUEUE_SIZE)
    loop.create_task(ble_mqtt_publisher())
    ble.config(rxbuf=BLE_RX_BUFFER)
    ble.irq(bt_irq)
    ble.active(True)
    print("[BLE] Setup comleted!")

def ble_scan_start():
    global ble
    ble.gap_scan(0, BLE_INTERVAL_US, BLE_WINDOW_US)
    print("[BLE] Scan started!")

def ble_scan_stop():
    global ble
    ble.gap_scan(None)
    print("[BLE] Scan stopped!")

def ble_scan_restart(_):
    global ble
    ble_scan_stop()
    ble_scan_start()

async def ble_daemon_start(loop):
    print("[BLE] Awaiting wifi.")
    await wifi.await_wifi_connected()
    ble_setup(loop)
    ble_scan_start()
    #loop.create_task(ble_watchdog())

async def ble_watchdog():
    ble_scan_start()
    while True:
        await asyncio.sleep(30)
        ble_scan_restart(None)

