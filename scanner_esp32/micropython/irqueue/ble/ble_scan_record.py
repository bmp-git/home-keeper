from irqueue.funs import bytearrayCopy
from irqueue.padbytearray import PADbytearray
class BLEScanRecord:
    def __init__(self):
        self.addr = bytearray(6)
        self.adv_data = PADbytearray(31)
        self.rssi = int(0)
    
    def setValues(self, v1, v2, v3, v4, v5):
        bytearrayCopy(v1, self.addr)
        self.adv_data.set_bytearray(v2)
        self.rssi = v3

    def copyTo(self, dest):
        bytearrayCopy(self.addr, dest.addr)
        self.adv_data.copyTo(dest.adv_data)
        dest.rssi = self.rssi