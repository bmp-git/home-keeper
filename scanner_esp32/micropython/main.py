import wifi
import ble
import rf433


wifi.do_connect()
ble.ble_scan_start()
rf433.start_receiving(gpio=27)
