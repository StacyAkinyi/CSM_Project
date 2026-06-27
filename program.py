import random
import math

class DiscreteEventSimulation:
    def __init__(self, arrival_rate, service_rate, max_simulation_time):
        # Simulation parameters
        self.arrival_rate = arrival_rate        # Lambda (λ)
        self.service_rate = service_rate        # Mu (μ)
        self.max_simulation_time = max_simulation_time
        
        # System state variables
        self.sim_clock = 0.0
        self.server_status = "IDLE"  # Can be "IDLE" or "BUSY"
        self.queue = []              # Stores arrival times of waiting customers
        
        # Event list structure: list of tuples (event_time, event_type)
        self.event_list = []
        
        # Statistical counters
        self.num_customer_served = 0
        self.total_waiting_time = 0.0
        self.total_system_time = 0.0
        self.area_under_q = 0.0      # For calculating average queue length
        self.area_under_b = 0.0      # For calculating server utilization
        
        # Track last event time to compute time-dependent statistics
        self.last_event_time = 0.0

    def generate_exponential(self, rate):
        # Inverse transform sampling to generate exponential random variables
        return -math.log(1.0 - random.random()) / rate

    def initialize(self):
        # Set clock and states to zero
        self.sim_clock = 0.0
        self.server_status = "IDLE"
        self.queue = []
        self.event_list = []
        
        self.num_customer_served = 0
        self.total_waiting_time = 0.0
        self.total_system_time = 0.0
        self.area_under_q = 0.0
        self.area_under_b = 0.0
        self.last_event_time = 0.0
        
        # Schedule the very first arrival event
        first_arrival_time = self.generate_exponential(self.arrival_rate)
        self.event_list.append((first_arrival_time, "ARRIVAL"))

    def update_statistics(self):
        # Compute time elapsed since the last event occurred
        time_elapsed = self.sim_clock - self.last_event_time
        self.last_event_time = self.sim_clock
        
        # Update area under the queue length curve
        self.area_under_q += len(self.queue) * time_elapsed
        
        # Update area under the server busy curve
        if self.server_status == "BUSY":
            self.area_under_b += 1.0 * time_elapsed

    def handle_arrival(self):
        # Schedule the next arrival event to keep the process moving
        next_arrival_time = self.sim_clock + self.generate_exponential(self.arrival_rate)
        self.event_list.append((next_arrival_time, "ARRIVAL"))
        
        # Check server availability
        if self.server_status == "IDLE":
            # Server is free; customer goes straight into service
            self.server_status = "BUSY"
            # Schedule the departure event for this customer
            service_duration = self.generate_exponential(self.service_rate)
            departure_time = self.sim_clock + service_duration
            self.event_list.append((departure_time, "DEPARTURE"))
            
            # Waiting time is 0 for this customer
            self.total_system_time += service_duration
            self.num_customer_served += 1
        else:
            # Server is busy; customer joins the back of the queue
            self.queue.append(self.sim_clock)

    def handle_departure(self):
        if len(self.queue) > 0:
            # If customers are waiting, pull the next one out of the queue
            arrival_time = self.queue.pop(0)
            waiting_time = self.sim_clock - arrival_time
            self.total_waiting_time += waiting_time
            
            # Schedule departure for the new customer entering service
            service_duration = self.generate_exponential(self.service_rate)
            departure_time = self.sim_clock + service_duration
            self.event_list.append((departure_time, "DEPARTURE"))
            
            self.total_system_time += (waiting_time + service_duration)
            self.num_customer_served += 1
        else:
            # No one is waiting in line; set server to idle
            self.server_status = "IDLE"

    def run_simulation(self):
        self.initialize()
        
        # Main simulation engine loop
        while self.sim_clock < self.max_simulation_time and self.event_list:
            # Sort the event list so the earliest event is always first
            self.event_list.sort(key=lambda x: x[0])
            
            # Extract the next scheduled event
            current_event = self.event_list.pop(0)
            event_time = current_event[0]
            event_type = current_event[1]
            
            # Terminate if the next event exceeds our simulation runtime limits
            if event_time > self.max_simulation_time:
                break
                
            # Advance simulation clock to the event time
            self.sim_clock = event_time
            
            # Track statistical areas before altering state variables
            self.update_statistics()
            
            # Delegate event logic execution based on event type
            if event_type == "ARRIVAL":
                self.handle_arrival()
            elif event_type == "DEPARTURE":
                self.handle_departure()
                
        self.print_results()

    def print_results(self):
        # Calculate final performance measures
        avg_queue_length = self.area_under_q / self.sim_clock
        server_utilization = (self.area_under_b / self.sim_clock) * 100
        avg_waiting_time = self.total_waiting_time / max(1, self.num_customer_served)
        avg_system_time = self.total_system_time / max(1, self.num_customer_served)
        
        print("="*45)
        print("      DISCRETE EVENT SIMULATION RESULTS      ")
        print("="*45)
        print(f"Total Simulation Run Time   : {self.sim_clock:.2f} time units")
        print(f"Total Customers Served       : {self.num_customer_served}")
        print(f"Server Utilization Factor    : {server_utilization:.2f}%")
        print(f"Average Queue Length (Lq)    : {avg_queue_length:.4f} customers")
        print(f"Average Time in Queue (Wq)   : {avg_waiting_time:.4f} time units")
        print(f"Average Time in System (Ws)  : {avg_system_time:.4f} time units")
        print("="*45)

# Entry point execution parameters
if __name__ == "__main__":
    # Example setup: Arrival rate = 15/hr (0.25/min), Service rate = 20/hr (0.333/min)
    # Run simulation for a continuous block of 480 minutes (an 8-hour shift)
    sim = DiscreteEventSimulation(arrival_rate=0.25, service_rate=0.333, max_simulation_time=480.0)
    sim.run_simulation()