//https://www.cse.chalmers.se/edu/course/TDA384_LP3/trainmonitoring/
import TSim.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Lab1Extra {

    TSimInterface tsi = TSimInterface.getInstance();

    final int trainId1 = 1;
    final int trainId2 = 2;

    // Track monitors for critical sections
    private final TrackMonitor black = new TrackMonitor();
    private final TrackMonitor blue = new TrackMonitor();
    private final TrackMonitor red = new TrackMonitor();
    private final TrackMonitor orange = new TrackMonitor();
    private final TrackMonitor green = new TrackMonitor();
    private final TrackMonitor darkGreen = new TrackMonitor();
    private final TrackMonitor pink = new TrackMonitor();
    private final TrackMonitor grey = new TrackMonitor();
    private final TrackMonitor purple = new TrackMonitor();

    public Lab1Extra(int speed1, int speed2) {
        ThreadTrain train1 = new ThreadTrain(trainId1, speed1);
        ThreadTrain train2 = new ThreadTrain(trainId2, speed2);
        train1.start();
        train2.start();
    }

    // Monitor class to control access to critical sections
    class TrackMonitor {
        private final Lock lock = new ReentrantLock();
        private final Condition trackAvailable = lock.newCondition();
        private boolean isOccupied = false;

        // Called when a train wants to enter the track section
        public void enter() {
            // guarantee that at most one thread is active on a monitor at any time
            lock.lock();
            try {
                while (isOccupied) {
                    trackAvailable.await(); // Wait if the track is occupied
                }
                isOccupied = true; // Occupy the track section
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        // Called when a train leaves the track section
        public void leave() {
            // guarantee that at most one thread is active on a monitor at any time
            lock.lock();
            try {
                isOccupied = false; // Free the track section
                trackAvailable.signal(); // Notify any waiting train
            } finally {
                lock.unlock();
            }
        }
    }

    class ThreadTrain extends Thread {

        int trainId;
        public int speed; // Train speed is stored here, including after reversing direction

        // Constructor to initialize each train with its ID and speed
        public ThreadTrain(int trainId, int speed) {
            this.trainId = trainId;
            this.speed = speed;
        }

        // Main control method for each train
        @Override
        public void run() {
            Set<TrackMonitor> acquiredMonitors = new HashSet<>(); // To track monitors acquired by the train

            try {
                tsi.setSpeed(trainId, speed); // Set the train's initial speed
                System.out.println("Train " + trainId + " started with speed " + speed);

                if (trainId == 1) {
                    // Train 1 starts at blue section
                    initial(trainId, speed, tsi, blue, acquiredMonitors);
                } else {
                    // Train 2 starts at pink section
                    initial(trainId, speed, tsi, pink, acquiredMonitors);
                }

                // Continuously listen for sensor events
                while (true) {
                    SensorEvent sensor = tsi.getSensor(trainId); // Wait for a sensor event
                    if (sensor.getStatus() == SensorEvent.ACTIVE) { // Only handle active events
                        System.out.println("Train " + trainId + " handleSensorEvent " + sensor);
                        handleSensorEvent(sensor, trainId, acquiredMonitors);
                    }
                }

            } catch (CommandException | InterruptedException e) {
                // In case of Exeption, exit the program
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Handles each sensor event triggered by the train
        private void handleSensorEvent(SensorEvent sensor, int trainId, Set<TrackMonitor> acquiredMonitors)
                throws CommandException, InterruptedException {
            // We extract the X and the Y coordinates from the sensor event.
            int sensorX = sensor.getXpos();
            int sensorY = sensor.getYpos();

            // Handle semaphore acquisition and release for track sections
            handleTrackSections(sensorX, sensorY, trainId, acquiredMonitors);
        }

        // Manages train's entry and exit from critical track sections (protected by
        // monitors)
        private void handleTrackSections(int sensorX, int sensorY, int trainId, Set<TrackMonitor> acquiredMonitors)
                throws CommandException, InterruptedException {
            TSimInterface tsi = TSimInterface.getInstance();

            // Handle station sensors (end-of-line): Train stops, reverses direction, and
            // continues
            if ((sensorX == 16 && sensorY == 3) || (sensorX == 16 && sensorY == 5) || (sensorX == 16 && sensorY == 11)
                    || (sensorX == 16 && sensorY == 13)) {
                manageStation(trainId, acquiredMonitors);
            }

            // Handle the black section (6,5), (8,5), (11,7), (10,8)
            if ((sensorX == 6 && sensorY == 5) || (sensorX == 8 && sensorY == 5) ||
                    (sensorX == 11 && sensorY == 7) || (sensorX == 10 && sensorY == 8)) {
                if (acquiredMonitors.contains(black)) {
                    black.leave();
                    acquiredMonitors.remove(black);
                } else {
                    black.enter();
                    acquiredMonitors.add(black);
                }
            }

            // Orange section: Sensors 7, 8
            if ((sensorX == 14 && sensorY == 7) || (sensorX == 14 && sensorY == 8)) {
                if (acquiredMonitors.contains(orange)) {
                    orange.leave();
                    acquiredMonitors.remove(orange);
                } else {
                    // manageMonitors(orange, trainId, speed, tsi, acquiredMonitors);
                    manageMonitor(orange, trainId, speed, tsi, acquiredMonitors);
                    tsi.setSwitch(17, 7,
                            acquiredMonitors.contains(blue) ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
                }
            }

            // Red/Blue/Green/DarkGreen section: Sensor 9/10
            if (sensorX == 19 && sensorY == 9) {
                if (acquiredMonitors.contains(green) || acquiredMonitors.contains(darkGreen)) {
                    if (acquiredMonitors.contains(green)) {
                        acquiredMonitors.remove(green);
                        green.leave();
                    } else {
                        acquiredMonitors.remove(darkGreen);
                        darkGreen.leave();
                    }
                    acquireEither(red, blue, tsi, acquiredMonitors);
                    tsi.setSwitch(17, 7,
                            acquiredMonitors.contains(red) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
                } else {
                    if (acquiredMonitors.contains(red)) {
                        acquiredMonitors.remove(red);
                        red.leave();
                    } else {
                        acquiredMonitors.remove(blue);
                        blue.leave();
                    }
                    acquireEither(darkGreen, green, tsi, acquiredMonitors);

                    tsi.setSwitch(15, 9,
                            acquiredMonitors.contains(green) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
                }
            }

            // Orange/Purple section: Sensors 11, 12
            if ((sensorX == 9 && sensorY == 9) || (sensorX == 9 && sensorY == 10)) {
                if (acquiredMonitors.contains(orange)) {
                    acquiredMonitors.remove(orange);
                    orange.leave();
                    manageMonitor(purple, trainId, speed, tsi, acquiredMonitors);
                    tsi.setSwitch(4, 9,
                            acquiredMonitors.contains(green) ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
                } else {
                    acquiredMonitors.remove(purple);
                    purple.leave();
                    manageMonitor(orange, trainId, speed, tsi, acquiredMonitors);
                    tsi.setSwitch(15, 9,
                            acquiredMonitors.contains(green) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
                }
            }

            // Pink/Gray section: Sensor 13
            if (sensorX == 1 && sensorY == 10) {
                if (acquiredMonitors.contains(pink) || acquiredMonitors.contains(grey)) {
                    if (acquiredMonitors.contains(pink)) {
                        acquiredMonitors.remove(pink);
                        pink.leave();
                    } else {
                        acquiredMonitors.remove(grey);
                        grey.leave();
                    }
                    acquireEither(darkGreen, green, tsi, acquiredMonitors);
                    tsi.setSwitch(4, 9,
                            acquiredMonitors.contains(green) ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
                } else {
                    if (acquiredMonitors.contains(green)) {
                        acquiredMonitors.remove(green);
                        green.leave();
                    } else {
                        acquiredMonitors.remove(darkGreen);
                        darkGreen.leave();
                    }
                    acquireEither(pink, grey, tsi, acquiredMonitors);
                    tsi.setSwitch(3, 11,
                            acquiredMonitors.contains(pink) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
                }
            }

            // Purple section: Sensors 14, 15
            if ((sensorX == 6 && sensorY == 11) || (sensorX == 6 && sensorY == 13)) {
                if (acquiredMonitors.contains(purple)) {
                    acquiredMonitors.remove(purple);
                    purple.leave();
                } else {
                    manageMonitor(purple, trainId, speed, tsi, acquiredMonitors);
                    tsi.setSwitch(3, 11,
                            acquiredMonitors.contains(pink) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
                }
            }
        }

        // Helper function to handle initial movement at the start of the program
        private void initial(int trainId, int speed, TSimInterface tsi, TrackMonitor monitor,
                Set<TrackMonitor> acquiredMonitors) throws CommandException {
            tsi.setSpeed(trainId, speed); // Start the train at the given speed
            monitor.enter(); // Acquire the initial section (blue or pink)
            acquiredMonitors.add(monitor); // Track the acquired monitor
        }

        // Manages train behavior when arriving at the station
        private void manageStation(int trainId, Set<TrackMonitor> acquiredMonitors)
                throws CommandException, InterruptedException {
            tsi.setSpeed(trainId, 0); // Stop the train at the station
            int timeToWait = 1000 + (20 * Math.abs(speed));

            Thread.sleep(timeToWait); // Wait for 2 seconds

            // Reverse the direction by inverting the speed and updating the global 'speed'
            // variable
            this.speed = -this.speed; // Update the global speed variable
            tsi.setSpeed(trainId, this.speed); // Set the new reversed speed

            System.out.println("Train " + trainId + " reversed direction, new speed: " + this.speed);
        }

        private void manageMonitor(TrackMonitor monitor, int trainId, int speed, TSimInterface tsi,
                Set<TrackMonitor> acquiredMonitors)
                throws CommandException, InterruptedException {
            monitor.lock.lock(); // Try to acquire the lock before accessing the critical section
            try {
                if (monitor.isOccupied) {
                    tsi.setSpeed(trainId, 0); // Stop the train if the track is occupied
                    while (monitor.isOccupied) {
                        monitor.trackAvailable.await(); // Wait until the monitor is free
                    }
                    monitor.isOccupied = true; // Acquire the monitor (track section)
                    acquiredMonitors.add(monitor); // Track acquired monitor
                    tsi.setSpeed(trainId, speed); // Resume the train after acquiring the monitor
                } else {
                    monitor.isOccupied = true; // Immediately acquire the monitor if free
                    acquiredMonitors.add(monitor); // Track the acquired monitor
                }
            } finally {
                monitor.lock.unlock(); // Always release the lock in the finally block
            }
        }

        private void acquireEither(TrackMonitor track1, TrackMonitor track2, TSimInterface tsi,
                Set<TrackMonitor> acquiredMonitors) {
            boolean acquiredTrack1, acquiredTrack2;
            while (true) {
                acquiredTrack1 = false;
                acquiredTrack2 = false;
                // Attempt to acquire the first track lock
                track1.lock.lock();
                try {
                    if (!track1.isOccupied) {
                        track1.isOccupied = true;
                        acquiredMonitors.add(track1);
                        acquiredTrack1 = true; // Mark that track1 was acquired
                        break; // Successfully acquired track1, exit the loop
                    }
                } finally {
                    if (!acquiredTrack1) { // Only unlock if the track was not acquired
                        track1.lock.unlock();
                    }
                }
                // Attempt to acquire the second track lock
                track2.lock.lock();
                try {
                    if (!track2.isOccupied) {
                        track2.isOccupied = true;
                        acquiredMonitors.add(track2);
                        acquiredTrack2 = true; // Mark that track2 was acquired
                        break; // Successfully acquired track2, exit the loop
                    }
                } finally {
                    if (!acquiredTrack2) { // Only unlock if the track was not acquired
                        track2.lock.unlock();
                    }
                }
            }
            // If track1 was acquired, unlock the track1 lock after usage
            if (acquiredTrack1) {
                track1.lock.unlock();
            }
            // If track2 was acquired, unlock the track2 lock after usage
            if (acquiredTrack2) {
                track2.lock.unlock();
            }
        }

    }
}
