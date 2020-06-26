import machine

class IRQMutex:
    def __init__(self):
         self.lock = False
    
    # Blocks until the mutex is not aquired. Do not use in interrupt handlers.
    def aquire(self):
        while True:
            if self.lock:
                continue
            irq_state = machine.disable_irq()
            if self.lock: # someone aquired the mutex while disabling irq
                machine.enable_irq(irq_state)
                continue
            self.lock = True
            machine.enable_irq(irq_state)
            break
    
    # Tries to get the mutex, returns false if didn't get it. To be used in interrupt handlers.
    def tryaquire(self):
        irq_state = machine.disable_irq()
        locked = self.lock
        if not locked:
            self.lock = True
        machine.enable_irq(irq_state)
        return not locked

    def release(self):
        self.lock = False
    
    def __enter__(self):
        self.aquire()
        return self    
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.release()