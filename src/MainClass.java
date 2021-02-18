import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainClass {
    public static final int CARS_COUNT = 4;
    static CyclicBarrier cb = new CyclicBarrier(CARS_COUNT, new Runnable() {
        @Override
        public void run() {
            System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка началась!!!");
        }
    });
    static CountDownLatch cdl = new CountDownLatch(CARS_COUNT);
public static volatile String winner;

    public static void main(String[] args) {
        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Подготовка!!!");
        Race race = new Race(new Road(60), new Tunnel(), new Road(40));
        Car[] cars = new Car[CARS_COUNT];

        for (int i = 0; i < CARS_COUNT; i++) {
            cars[i] = new Car(race, 20 + (int) (Math.random() * 10), cb, cdl);
        }


            for (int i = 0; i < CARS_COUNT; i++) {
                new Thread(cars[i]).start();
            }




        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка закончилась!!!");
        System.out.println("Победитель " + winner);
    }
}

class Car implements Runnable {
    public static int CARS_COUNT;

    static {
        CARS_COUNT = 0;
    }

    private Race race;
    private int speed;
    private String name;
    private CyclicBarrier cb;
    private CountDownLatch cdl;


    public String getName() {
        return name;
    }

    public int getSpeed() {
        return speed;
    }


    public Car(Race race, int speed, CyclicBarrier cb, CountDownLatch cdl) {
        this.race = race;
        this.speed = speed;
        this.cb = cb;
        this.cdl = cdl;
        CARS_COUNT++;
        this.name = "Участник #" + CARS_COUNT;

    }


    @Override

    public void run() {

        try {
            System.out.println(this.name + " готовится");
            Thread.sleep(500 + (int) (Math.random() * 800));
            System.out.println(this.name + " готов");
            cb.await();

        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }

        for (int y = 0; y < race.getStages().size(); y++) {
            race.getStages().get(y).go(this);
        }
        String localWinner = MainClass.winner;
        if(localWinner == null) {
            synchronized (MainClass.class) {
                localWinner = MainClass.winner;
                if (localWinner == null) {
                    MainClass.winner = this.name;
                }
            }
        }
        cdl.countDown();
    }

}


abstract class Stage {
    protected int length;
    protected String description;

    public String getDescription() {
        return description;
    }

    public abstract void go(Car c);
}

class Road extends Stage {
    public Road(int length) {
        this.length = length;
        this.description = "Дорога " + length + " метров";
    }

    @Override
    public void go(Car c) {
        try {
            System.out.println(c.getName() + " начал этап: " + description);
            Thread.sleep(length / c.getSpeed() * 1000);
            System.out.println(c.getName() + " закончил этап: " + description);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Tunnel extends Stage {
    public Tunnel() {
        this.length = 80;
        this.description = "Тоннель " + length + " метров";
    }

    Semaphore smp = new Semaphore(MainClass.CARS_COUNT / 2);

    @Override
    public void go(Car c) {
        try {


            try {
                System.out.println(c.getName() + " готовится к этапу(ждет): " + description);
                smp.acquire();
                System.out.println(c.getName() + " начал этап: " + description);
                Thread.sleep(length / c.getSpeed() * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println(c.getName() + " закончил этап: " + description);
                smp.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Race {
    private ArrayList<Stage> stages;

    public ArrayList<Stage> getStages() {
        return stages;
    }

    public Race(Stage... stages) {
        this.stages = new ArrayList<>(Arrays.asList(stages));
    }
}