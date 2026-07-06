import java.util.*;

public class UniformDES {
    private double minA, maxA, minS, maxS;
    private int target;
    
    private List<CustomerData> log = new ArrayList<>();
    private double[] timeInState = new double[100]; 
    private int currentCustomers = 0; 
    private double lastEventTime = 0.0;
    private int waitCount = 0; 
    
    static class CustomerData {
        int id; double arrival, service, startService, departure, waitTime, systemTime;
    }

    public UniformDES(double minA, double maxA, double minS, double maxS, int target) {
        this.minA = minA; this.maxA = maxA;
        this.minS = minS; this.maxS = maxS;
        this.target = target;
    }

    private double generateUniform(double min, double max) {
        return min + (new Random().nextDouble() * (max - min));
    }

    private void updateStatistics(double currentTime) {
        double timeElapsed = currentTime - lastEventTime;
        if (currentCustomers < timeInState.length) {
            timeInState[currentCustomers] += timeElapsed;
        }
        lastEventTime = currentTime;
    }

    public void runSimulation() {
        double nextArrival = generateUniform(minA, maxA);
        double serverFreeAt = 0.0;
        
        double totalWait = 0.0, totalSystem = 0.0;
        double serverBusyTime = 0.0;

        for (int i = 1; i <= target; i++) {
            CustomerData c = new CustomerData();
            c.id = i;
            c.arrival = (i == 1) ? nextArrival : log.get(i-2).arrival + generateUniform(minA, maxA);
            c.service = generateUniform(minS, maxS);
            
            updateStatistics(c.arrival);
            currentCustomers++; 
            
            c.startService = Math.max(c.arrival, serverFreeAt);
            if (c.startService > c.arrival) waitCount++;
            
            c.departure = c.startService + c.service;
            c.waitTime = c.startService - c.arrival;
            c.systemTime = c.departure - c.arrival;
            
            serverFreeAt = c.departure;
            log.add(c);
            totalWait += c.waitTime;
            totalSystem += c.systemTime;
            serverBusyTime += c.service;
            
            updateStatistics(c.departure);
            currentCustomers--;
        }

        printTable();
        printMetrics(totalWait, totalSystem, serverBusyTime, lastEventTime);
        printFinalStats(); 
    }

    private void printTable() {
        System.out.printf("%-5s | %-10s | %-10s | %-10s | %-10s%n", "ID", "Arrival", "Service", "Wait", "SysTime");
        for (CustomerData c : log) {
            System.out.printf("%-5d | %-10.2f | %-10.2f | %-10.2f | %-10.2f%n", 
                               c.id, c.arrival, c.service, c.waitTime, c.systemTime);
        }
    }

    private void printMetrics(double tw, double ts, double busy, double totalTime) {
        System.out.println("\n--- Queuing Characteristics ---");
        System.out.printf("Avg Time in Queue (Wq): %.4f%n", tw / target);
        System.out.printf("Avg Time in System (Ws): %.4f%n", ts / target);
        System.out.printf("Server Utilization: %.2f%%%n", (busy / totalTime) * 100);
    }

    public void printFinalStats() {
        double totalTime = lastEventTime;
        System.out.println("\n--- Detailed System Probabilities ---");
        
        double[] pN = new double[100];
        for(int i = 0; i < 100; i++) pN[i] = timeInState[i] / totalTime;

        double Lq = 0;
        for(int n = 2; n < 100; n++) Lq += (n - 1) * pN[n];

        System.out.printf("Expected Number in Queue (Lq): %.4f%n", Lq);
        System.out.printf("Probability Server is Busy: %.4f%n", (1.0 - pN[0]));
        System.out.printf("Probability Customer Waits: %.4f%n", (double)waitCount / target);
        
        System.out.println("\nState | P(n)    | P(<=n)  | P(>=n)");
        double cumulativeLess = 0;
        for (int n = 0; n < 10; n++) { // Display first 10 states
            cumulativeLess += pN[n];
            double cumulativeMore = 1.0 - (n > 0 ? (cumulativeLess - pN[n]) : 0);
            System.out.printf("%-5d | %-7.4f | %-7.4f | %-7.4f%n", n, pN[n], cumulativeLess, cumulativeMore);
        }
    }

    public static void main(String[] args) {
        new UniformDES(2.0, 5.0, 1.0, 3.0, 100).runSimulation();
    }
}