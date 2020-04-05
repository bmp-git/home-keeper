import time
from rpi_rf.rpi_rf import RFDevice

def on_data_received(rx_code, rx_pulselength, rx_proto):
    print("Rf433 received: ", rx_code, rx_pulselength, rx_proto)

def start_receiving(gpio):
    rfdevice = RFDevice(gpio)
    rfdevice.enable_rx(on_data_received)
    print("Listening for codes on GPIO ", gpio)
    #rfdevice.cleanup()
