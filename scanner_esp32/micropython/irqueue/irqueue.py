import machine
from irqueue.funs import inc
from irqmutex.irqmutex import IRQMutex

class IRQueue:
    def __init__(self, recordType, size):
        self.size = size
        self.array = [recordType() for x in range(self.size)]
        self.start = None
        self.end = 0
        self.mutex = IRQMutex()
    
    def enqueue(self, v1 = None, v2 = None, v3 = None, v4 = None, v5 = None):
        if self.mutex.tryaquire():
            # Overwrite oldest item if full
            if self.end == self.start:
                self.start = inc(self.start, self.size)

            if self.start is None:
                self.start = self.end
            
            self.array[self.end].setValues(v1, v2, v3, v4, v5)
            self.end = inc(self.end, self.size)
            self.mutex.release()
        else:
            pass
    

    def dequeue(self, item):
        isEmpty = self.start is None
        if not isEmpty:
            with self.mutex:
                self.array[self.start].copyTo(item)
                self.start = inc(self.start, self.size)
                if self.start == self.end:
                    self.start = None
        return not isEmpty