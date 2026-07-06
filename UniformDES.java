import java.util.*;

/**
 * Discrete Event Simulation with Uniform Distribution.
 * Stops after serving a fixed number of customers.
 */
public class UniformDES {

    private double minArrival, maxArrival;
    private double minService, maxService;
    private int targetCustomers;
    
    private double simClock = 0.0;
    private String serverStatus = "IDLE";
    private Queue<Double> queue = new LinkedList<>();
    private PriorityQueue<Event> eventList = new PriorityQueue<>(Comparator.comparingDouble(e -> e.time));
    
    private int numCustomersServed = 0;
    private double totalWaitingTime = 0.0;
    private double areaUnderQ = 0.0, areaUnderB = 0.0, lastEventTime = 0.0;

    static class Event {
        double time;
        String type;
        Event(double time, String type) { this.time = time; this.type = type; }
    }

    public UniformDES(double minA, double maxA, double minS, double maxS, int target) {
        this.minArrival = minA; this.maxArrival = maxA;
        this.minService = minS; this.maxService = maxS;
        this.targetCustomers = target;
    }

    private double generateUniform(double min, double max) {
        return min + (new Random().nextDouble() * (max - min));
    }

    public void runSimulation() {
        // Schedule first arrival
        eventList.add(new Event(generateUniform(minArrival, maxArrival), "ARRIVAL"));

        // Loop until target number of customers are served
        while (numCustomersServed < targetCustomers && !eventList.isEmpty()) {
            Event currentEvent = eventList.poll();
            simClock = currentEvent.time;
            updateStatistics();

            if (currentEvent.type.equals("ARRIVAL")) {
                handleArrival();
            } else {
                handleDeparture();
            }
        }
        printResults();
    }

    private void updateStatistics() {
        double timeElapsed = simClock - lastEventTime;
        lastEventTime = simClock;
        areaUnderQ += queue.size() * timeElapsed;
        if (serverStatus.equals("BUSY")) areaUnderB += 1.0 * timeElapsed;
    }

    private void handleArrival() {
        // Schedule next arrival
        eventList.add(new Event(simClock + generateUniform(minArrival, maxArrival), "ARRIVAL"));

        if (serverStatus.equals("IDLE")) {
            serverStatus = "BUSY";
            eventList.add(new Event(simClock + generateUniform(minService, maxService), "DEPARTURE"));
            numCustomersServed++;
        } else {
            queue.add(simClock);
        }
    }

    private void handleDeparture() {
        if (!queue.isEmpty()) {
            totalWaitingTime += (simClock - queue.poll());
            eventList.add(new Event(simClock + generateUniform(minService, maxService), "DEPARTURE"));
            numCustomersServed++;
        } else {
            serverStatus = "IDLE";
        }
    }

    private void printResults() {
        System.out.println("--- Results for " + numCustomersServed + " customers ---");
        System.out.printf("Avg Queue Length: %.4f%n", areaUnderQ / simClock);
        System.out.printf("Avg Waiting Time: %.4f%n", totalWaitingTime / numCustomersServed);
        System.out.printf("Server Utilization: %.2f%%%n", (areaUnderB / simClock) * 100);
    }

    public static void main(String[] args) {
        // Example: Arrival [0.5,3], Service [1,4], Target 100
        UniformDES sim = new UniformDES(0.5, 3.0, 1.0, 4.0, 100);
        sim.runSimulation();
    }
}