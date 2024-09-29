import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.IntStream;
 
class Customer {
    private final long arrivalTime;
    private final long serviceTime;

    public Customer(long arrivalTime, long serviceTime) {
        this.arrivalTime = arrivalTime;
        this.serviceTime = serviceTime;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public long getServiceTime() {
        return serviceTime;
    }
}

class Server extends Thread {
    private final Queue<Customer> queue;
    private final int maxQueueSize;
    private final Lock lock;
    private int servedCount;
    private long totalServiceTime;
    private volatile boolean running = true;

    public Server(int maxQueueSize) {
        this.queue = new LinkedList<>();
        this.maxQueueSize = maxQueueSize;
        this.lock = new ReentrantLock();
        this.servedCount = 0;
        this.totalServiceTime = 0;
    }

    public int getQueueSize() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    public boolean addCustomer(Customer customer) {
        lock.lock();
        try {
            if (queue.size() < maxQueueSize) {
                queue.add(customer);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    public int getServedCount() {
        return servedCount;
    }

    public long getTotalServiceTime() {
        return totalServiceTime;
    }

    public void stopServer() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            Customer customer = null;
            lock.lock();
            try {
                if (!queue.isEmpty()) {
                    customer = queue.poll();
                }
            } finally {
                lock.unlock();
            }

            if (customer != null) {
                lock.lock();
                try{
                    servedCount++;
                    totalServiceTime += customer.getServiceTime();
                    try {
                        Thread.sleep(customer.getServiceTime());
                    } catch (InterruptedException e) {
                        if (!running) {
                            break;
                        }
                    }
                }finally {
                    lock.unlock();
                }
            }
        }
    }
}

public class Main {
    private final List<Server> servers;
    private final int maxQueueSize;
    private final List<Customer> allCustomers;
    private final List<Customer> servedCustomers;
    private final List<Customer> unservedCustomers;

    public Main(int n, int m) {// n= no of servers m= maximum size of queue
        this.servers = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            servers.add(new Server(m));
        }
        this.maxQueueSize = m;
        this.allCustomers = Collections.synchronizedList(new ArrayList<>());
        this.servedCustomers = Collections.synchronizedList(new ArrayList<>());
        this.unservedCustomers = Collections.synchronizedList(new ArrayList<>());
    }

    public void openShop() {
        for (Server server : servers) {
            server.start();
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 2 * 60 * 1000; // 2 minutes in milliseconds

        while (System.currentTimeMillis() < endTime) {
            try {
                int arrivalDelay = ThreadLocalRandom.current().nextInt(5, 11)  * 1000; // Convert to milliseconds
                Thread.sleep(arrivalDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long arrivalTime = System.currentTimeMillis();
            int serviceTime = ThreadLocalRandom.current().nextInt(10, 21) * 1000; // Convert to milliseconds
            Customer customer = new Customer(arrivalTime, serviceTime);
            allCustomers.add(customer);

            Server chosenServer = servers.stream().min(Comparator.comparingInt(Server::getQueueSize)).orElse(null);

            if (chosenServer != null && chosenServer.addCustomer(customer)) {
                servedCustomers.add(customer);
             
            } else {
                try {
                    Thread.sleep(10000); // Wait for 10 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                chosenServer = servers.stream().min(Comparator.comparingInt(Server::getQueueSize)).orElse(null);

                if (chosenServer != null && chosenServer.addCustomer(customer)) {
                    servedCustomers.add(customer);
                } else {
                    unservedCustomers.add(customer);
                }
            }
        }

        for (Server server : servers) {
            server.stopServer();
        }

        for (Server server : servers) {
            try {
                server.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getTotalServedCustomers() {
        return servedCustomers.size();
    }

    public int getTotalUnservedCustomers() {
        return unservedCustomers.size();
    }

    public double getAverageServiceTime() {
        return servedCustomers.stream().mapToLong(Customer::getServiceTime).average().orElse(0) / 1000.0; // Convert to seconds
    }

    public double getAverageArrivalTime() {
        if (allCustomers.size() < 2) return 0;
        long firstArrival = allCustomers.get(0).getArrivalTime();
        long lastArrival = allCustomers.get(allCustomers.size() - 1).getArrivalTime();
        return ((lastArrival - firstArrival) / (double) (allCustomers.size() - 1)) / 1000.0; // Convert to seconds
    }

    public double getAverageWaitingTime() {
        if (servedCustomers.isEmpty()) return 0;
        long totalWaitTime = servedCustomers.stream().mapToLong(c -> System.currentTimeMillis() - c.getArrivalTime()).sum();
        return (totalWaitTime / (double) servedCustomers.size()) / 1000.0; // Convert to seconds
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the number of servers: ");
        int n = scanner.nextInt();

        System.out.print("Enter the max queue size: ");
        int m = scanner.nextInt();

        Main shop = new Main(n, m);
        shop.openShop();

        System.out.println("Total customers served: " + shop.getTotalServedCustomers());
        System.out.println("Total customers left unserved: " + shop.getTotalUnservedCustomers());
        System.out.println("Average service time: " + shop.getAverageServiceTime() + " seconds");
        System.out.println("Average arrival time: " + shop.getAverageArrivalTime() + " seconds");
        System.out.println("Average waiting time: " + shop.getAverageWaitingTime() + " seconds");
    }
}
