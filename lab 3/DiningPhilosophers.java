import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DiningPhilosophers {

    // Constants
    private static final int NUM_PHILOSOPHERS = 5;
    private static final int NUM_TABLES = 5;
    private static final int NUM_TOTAL_PHILOSOPHERS = NUM_PHILOSOPHERS * NUM_TABLES;
    private static final int THINK_TIME_MAX = 10;
    private static final int EAT_TIME_MAX = 5;
    private static final int RIGHT_FORK_DELAY = 4;

    // Fork class as a Lock
    static class Fork {
        private final Lock lock = new ReentrantLock();

        public boolean pickUp() {
            return lock.tryLock();
        }

        public void putDown() {
            lock.unlock();
        }
    }

    // Philosopher class (extends Thread)
    static class Philosopher extends Thread {
        private final Fork leftFork;
        private final Fork rightFork;
        private final Random random;
        private boolean isAtSixthTable = false;

        public Philosopher(String name, Fork leftFork, Fork rightFork) {
            super(name); // Set the name of the thread (philosopher)
            this.leftFork = leftFork;
            this.rightFork = rightFork;
            this.random = new Random();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    think();
                    if (pickUpForks()) {
                        eat();
                        putDownForks();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void think() throws InterruptedException {
            int thinkTime = random.nextInt(THINK_TIME_MAX + 1);
            System.out.println("Philosopher " + getName() + " is thinking for " + thinkTime + " seconds.");
            Thread.sleep(thinkTime * 1000);
        }

        private boolean pickUpForks() throws InterruptedException {
            // Try to pick up left fork
            if (leftFork.pickUp()) {
                System.out.println("Philosopher " + getName() + " picked up left fork.");
                Thread.sleep(RIGHT_FORK_DELAY * 1000);

                // Try to pick up right fork
                if (rightFork.pickUp()) {
                    System.out.println("Philosopher " + getName() + " picked up right fork.");
                    return true;
                } else {
                    // Couldn't pick up right fork, put down left fork
                    leftFork.putDown();
                }
            }
            return false;
        }

        private void eat() throws InterruptedException {
            int eatTime = random.nextInt(EAT_TIME_MAX + 1);
            System.out.println("Philosopher " + getName() + " is eating for " + eatTime + " seconds.");
            Thread.sleep(eatTime * 1000);
        }

        private void putDownForks() {
            leftFork.putDown();
            rightFork.putDown();
            System.out.println("Philosopher " + getName() + " has put down both forks.");
        }

        public void moveToSixthTable() {
            isAtSixthTable = true;
            System.out.println("Philosopher " + getName() + " moved to the sixth table.");
        }

        public boolean isAtSixthTable() {
            return isAtSixthTable;
        }
    }

    // Create philosophers and forks
    public static Philosopher[] createPhilosophers(int numPhilosophers, Fork[] forks, char nameOffset) {
        Philosopher[] philosophers = new Philosopher[numPhilosophers];
        for (int i = 0; i < numPhilosophers; i++) {
            Fork leftFork = forks[i];
            Fork rightFork = forks[(i + 1) % numPhilosophers];
            // Create philosopher with a unique name
            String name = Character.toString((char) (nameOffset + i));
            philosophers[i] = new Philosopher(name, leftFork, rightFork);
        }
        return philosophers;
    }

    // Deadlock detection (simulated, simple approach)
    public static void detectDeadlock(Philosopher[] philosophers, Fork[] forks, Philosopher[] sixthTable, int maxSixthTablePhilosophers) throws InterruptedException {
        while (true) {
            // Simple simulation: if all philosophers are hungry and can't eat
            boolean deadlock = true;
            for (Philosopher p : philosophers) {
                if (!p.isAlive()) {
                    deadlock = false;
                    break;
                }
            }
            if (deadlock) {
                // Move one philosopher to the sixth table
                Philosopher p = philosophers[0];
                p.moveToSixthTable();
                System.out.println("Deadlock detected! Moving philosopher " + p.getName() + " to the sixth table.");
                return;
            }
            Thread.sleep(1000); // Check every second
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Fork[] forks = new Fork[NUM_PHILOSOPHERS];
        Philosopher[][] tables = new Philosopher[NUM_TABLES][NUM_PHILOSOPHERS];

        // Create forks
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks[i] = new Fork();
        }

        // Create philosophers for each of the 5 tables, assigning names from 'A' to 'Y'
        char nameOffset = 'A'; // Starting philosopher names from 'A'
        for (int i = 0; i < NUM_TABLES; i++) {
            tables[i] = createPhilosophers(NUM_PHILOSOPHERS, forks, nameOffset);
            nameOffset += NUM_PHILOSOPHERS; // Offset the name for the next table
        }

        // Start philosophers at each table
        for (Philosopher[] philosophers : tables) {
            for (Philosopher philosopher : philosophers) {
                philosopher.start();
            }
        }

        // Simulate deadlock detection at each table
        Philosopher[] sixthTable = new Philosopher[NUM_PHILOSOPHERS];
        for (int i = 0; i < NUM_TABLES; i++) {
            detectDeadlock(tables[i], forks, sixthTable, NUM_PHILOSOPHERS);
        }

        // Finally, detect deadlock at the sixth table
        detectDeadlock(sixthTable, forks, sixthTable, NUM_PHILOSOPHERS);
    }
}
