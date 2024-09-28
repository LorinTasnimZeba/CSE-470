import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

public class DiningPhilosophers {

    // Constants
    private static final int NUM_PHILOSOPHERS = 5;
    private static final int THINK_TIME_MAX = 10; // max think time in seconds
    private static final int EAT_TIME_MAX = 5;    // max eat time in seconds
    private static final int WAIT_TIME = 4;       // wait time in seconds for forks

    // Philosopher class extending Thread
    static class Philosopher extends Thread {
        private final int id;
        private final Lock leftFork;
        private final Lock rightFork;
        private Random random = new Random();

        public Philosopher(int id, Lock leftFork, Lock rightFork) {
            this.id = id;
            this.leftFork = leftFork;
            this.rightFork = rightFork;
        }

        private void think() throws InterruptedException {
            int thinkTime = random.nextInt(THINK_TIME_MAX + 1);
            System.out.println("Philosopher " + id + " is thinking for " + thinkTime + " seconds.");
            Thread.sleep(thinkTime * 1000);
        }

        private void eat() throws InterruptedException {
            int eatTime = random.nextInt(EAT_TIME_MAX + 1);
            System.out.println("Philosopher " + id + " is eating for " + eatTime + " seconds.");
            Thread.sleep(eatTime * 1000);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    think();
                    if (leftFork.tryLock()) {
                        try {
                            System.out.println("Philosopher " + id + " picked up left fork.");
                            if (rightFork.tryLock()) {
                                try {
                                    System.out.println("Philosopher " + id + " picked up right fork.");
                                    eat();
                                } finally {
                                    rightFork.unlock();
                                    System.out.println("Philosopher " + id + " put down right fork.");
                                }
                            } else {
                                System.out.println("Philosopher " + id + " couldn't pick up right fork.");
                            }
                        } finally {
                            leftFork.unlock();
                            System.out.println("Philosopher " + id + " put down left fork.");
                        }
                    } else {
                        System.out.println("Philosopher " + id + " is waiting for left fork.");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        Lock[] forks = new ReentrantLock[NUM_PHILOSOPHERS];
        Philosopher[] philosophers = new Philosopher[NUM_PHILOSOPHERS];

        // Initialize forks
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks[i] = new ReentrantLock();
        }

        // Initialize philosophers and assign forks
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            Lock leftFork = forks[i];
            Lock rightFork = forks[(i + 1) % NUM_PHILOSOPHERS];
            philosophers[i] = new Philosopher(i + 1, leftFork, rightFork);
            philosophers[i].start();
        }

        // Let philosophers run indefinitely
        for (Philosopher philosopher : philosophers) {
            try {
                philosopher.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
