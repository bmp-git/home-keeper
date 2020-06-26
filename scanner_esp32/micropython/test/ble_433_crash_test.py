import bluetooth
import time
from machine import Pin

_IRQ_SCAN_RESULT = const(1 << 4)

last_timestamp = 0

def bt_irq(event, data):
    if event == _IRQ_SCAN_RESULT:
        addr_type, addr, adv_type, rssi, adv_data = data
        print("[BLE IRQ]", addr_type, addr, adv_type, rssi, adv_data)

def pin_irq(gpio):
    global last_timestamp
    timestamp = int(time.ticks_us())
    duration = timestamp - last_timestamp
    last_timestamp = timestamp
    pass


ble = bluetooth.BLE()
ble.irq(bt_irq)
ble.active(True)
ble.gap_scan(0, 1000000, 500000)

pin = Pin(36, Pin.IN)
pin.irq(handler=pin_irq)