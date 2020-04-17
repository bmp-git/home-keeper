import time
import uasyncio as asyncio
import mqtt
import ujson
import micropython
from rpi_rf.rpi_rf import RFDevice
from config import RF433_PUBLISH_TOPIC, RF_433_RX_QUEUE_SIZE, RF_433_RX_GPIO
from irqueue.irqueue import IRQueue
from irqueue.rf433.rf433_rx_packet import RF433RXRecord

rf433_rx_queue = None

async def rf433_mqtt_publisher():
    global rf433_rx_queue
    item = RF433RXRecord()
    while True:
        item_present = rf433_rx_queue.dequeue(item)
        if item_present:
            print("[RF433] Received: ", item.code, item.pulselength, item.proto)
            payload = {
                'code' : item.code,
                'pulselength' : item.pulselength,
                'proto' : item.proto
            }
            await mqtt.mqtt_publish(RF433_PUBLISH_TOPIC, ujson.dumps(payload))
            await asyncio.sleep_ms(0)
        else:
            await asyncio.sleep_ms(50)


def rf433_irq(rx_code, rx_pulselength, rx_proto):
    global rf433_rx_queue
    #print("[RF433 IRQ] Received: ", rx_code, rx_pulselength, rx_proto)
    rf433_rx_queue.enqueue(rx_code, rx_pulselength, rx_proto)

async def rf433_start_receiving(loop):
    global rf433_rx_queue
    rf433_rx_queue = IRQueue(RF433RXRecord, RF_433_RX_QUEUE_SIZE)
    rfdevice = RFDevice(RF_433_RX_GPIO)
    rfdevice.enable_rx(rf433_irq)
    print("[RF433] Listening for codes on GPIO ", RF_433_RX_GPIO)
    loop.create_task(rf433_mqtt_publisher())
    #rfdevice.cleanup()
