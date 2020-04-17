import micropython
micropython.alloc_emergency_exception_buf(100)

import uasyncio as asyncio
loop = asyncio.get_event_loop()

import wifi
import ble
import mqtt
import rf433


loop.create_task(wifi.wifi_daemon_start())
loop.create_task(mqtt.mqtt_daemon_start(loop))
loop.create_task(ble.ble_daemon_start(loop))
loop.create_task(rf433.rf433_start_receiving(loop))
loop.run_forever()
