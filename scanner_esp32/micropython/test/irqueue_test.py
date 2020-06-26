import micropython
from irqueue.irqueue import IRQueue
from irqueue.ble.ble_scan_record import BLEScanRecord


q = IRQueue(BLEScanRecord, 10)
v1 = b'\x01\x01\x01\x01\x01\x01'
v2 = b'\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01'
v3 = -50
out = BLEScanRecord()

v4 = b'\x02\x03'

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
q.enqueue(v1, v2, v3)
q.enqueue(v1, v4, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
q.enqueue(v1, v2, v3)
micropython.heap_unlock()

q.dequeue(out)

# should print bytearray(b'\x02\x03')
print(out.adv_data.to_bytearray())