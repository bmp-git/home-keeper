import micropython
from utils.irqueue import IRQueue
from utils.ble_scan_record import BLEScanRecord


q = IRQueue(BLEScanRecord, 10)
v1 = b'\x01\x01\x01\x01\x01\x01'
v2 = b'\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01'
v3 = -50
out = BLEScanRecord()

micropython.heap_lock()
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
#q.enqueue(v1)
micropython.heap_unlock()

q.dequeue(out)

print(out.adv_data)