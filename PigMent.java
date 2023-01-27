import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class PigMent{
/* Global Constants */
static final int TIC_MIN = 50; // Tickrate minimum time (ms)
static final int TIC_MAX = 200; // Tickrate maximum time (ms)
static final int FEED = 20; // Mass gained through photosynthesis
static final int BMR = 10; // Mass lost due to basal metabolic rate
static final int MAX_POP = 10; // Maximum number of concurent pigs
static final int INIT_POP = 3; // size of initial pig population
static final int INIT_MASS = 20; // starting mass of initial pigs
static AtomicInteger globalId = new AtomicInteger();
static ExecutorService pigPool = Executors.newFixedThreadPool(MAX_POP);
static PriorityBlockingQueue<PhotoPig> openArea = new PriorityBlockingQueue<>(MAX_POP, (a, b) -> a.mass - b.mass);
static final ReentrantLock lock = new ReentrantLock();
static final Condition openAreaNotEmpty = lock.newCondition();

/* Implementing Awesomeness (a.k.a. the pigs) */
static class PhotoPig implements Runnable {
    final int id;
    int mass;

    void pigSleep() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(TIC_MIN, TIC_MAX + 1));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void pigSay(String msg) {
        System.out.println("<Pig#"+id + ","+mass+"kg> : " +msg);
    }

    boolean eatLight() {
        pigSleep();
        mass += FEED;
        mass -= mass / BMR;
        pigSay("Holy crap, I just ate light!");

        lock.lock();
        try {
            openArea.add(this);
            openAreaNotEmpty.signalAll();
        } finally {
            lock.unlock();
        }

        pigSleep();

        lock.lock();
        try {
            openArea.remove(this);
        } finally {
            lock.unlock();
        }

        if (mass / BMR > FEED / 2 && globalId.get() < MAX_POP) {
            PhotoPig newPig = new PhotoPig(mass / 2);
            mass = mass / 2;
            pigSay("This vessel can no longer hold the both of us!");
            pigPool.execute(newPig);
        }
        return true;
    }

    boolean aTerribleThingToDo() {
        PhotoPig prey = null;
        lock.lock();
        try {
        while (openArea.isEmpty()) {
        openAreaNotEmpty.await();
        }
        prey = openArea.poll();
        } catch (InterruptedException e) {
        e.printStackTrace();
        } finally {
        lock.unlock();
        }
        if (prey != null) {
        pigSleep();
        mass += prey.mass;
        pigSay("I just ate Pig#" + prey.id + " and gained " + prey.mass + "kg!");
        return true;
        }
        return false;
    }
    PhotoPig(int startingMass) {
        id = globalId.incrementAndGet();
        mass = startingMass;
    }

    public void run() {
        while (eatLight()) {
            if (ThreadLocalRandom.current().nextInt(100) > 75) {
                aTerribleThingToDo();
            }
        pigSay("Look on my works, ye Mighty, and despair!");
    }
}
}

public static void main(String[] args) {
     for(int i = 0; i < INIT_POP; i++) {
        PhotoPig pig = new PhotoPig(INIT_MASS);
        pigPool.execute(pig);
    }
    // try{
    //     Thread.sleep(10000);
    // }
    // catch(InterruptedException e)
    // {
    //     e.printStackTrace();
    // }
    pigPool.shutdown();
    try {
        pigPool.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
        pigPool.shutdownNow();
        e.printStackTrace();
    }
}
}
