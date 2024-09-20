import TSim.*;
import java.util.concurrent.Semaphore;
import java.util.HashSet;
import java.util.Set;

public class Lab1 {
    
    TSimInterface tsi = TSimInterface.getInstance();
    
    final int trainId1 = 1;
    final int trainId2 = 2;

    // Semaphores for critical sections
    private final Semaphore black = new Semaphore(1);
    private final Semaphore blue = new Semaphore(1);
    private final Semaphore red = new Semaphore(1);
    private final Semaphore orange = new Semaphore(1);
    private final Semaphore green = new Semaphore(1);
    private final Semaphore darkGreen = new Semaphore(1);
    private final Semaphore pink = new Semaphore(1);
    private final Semaphore grey = new Semaphore(1);
    private final Semaphore purple = new Semaphore(1);
    
    public Lab1(int speed1, int speed2) {
        ThreadTrain train1 = new ThreadTrain(trainId1, speed1);
        ThreadTrain train2 = new ThreadTrain(trainId2, speed2);
        train1.start();
        train2.start();
    }
    //Class that represent two trains as threads
    class ThreadTrain extends Thread{
        int trainId;
        int speed; // Train speed is stored here, including after reversing direction
        Set<Semaphore> acquiredSemaphores; // To track semaphores acquired by the train
        

        // Constructor to initialize each train with its ID and speed
        public ThreadTrain(int trainId, int speed) {
            this.trainId = trainId;
            this.speed = speed;
        }
    
        // Main control method for each train
        @Override
        public void run(){
            acquiredSemaphores = new HashSet<>(); 

            try {
                tsi.setSpeed(trainId, speed); // Set the train's initial speed
                System.out.println("Train " + trainId + " started with speed " + speed);
                if (trainId == 1) {
                    // Train 1 starts at blue section
                    initial(trainId, speed, tsi, blue, acquiredSemaphores);
                } else {
                    // Train 2 starts at pink section
                    initial(trainId, speed, tsi, pink, acquiredSemaphores);
                }

                // Continuously listen for sensor events
                while (true) {
                    SensorEvent sensor = tsi.getSensor(trainId); // Wait for a sensor event
                    if (sensor.getStatus() == SensorEvent.ACTIVE) { // Only handle active events
                        System.out.println("Train " + trainId + " handleSensorEvent " + sensor);
                        System.out.println("TRAIN: " + trainId + " SPEED: " + speed);
                        handleSensorEvent(sensor, trainId, speed, acquiredSemaphores);
                    }
                }

            } catch (CommandException | InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Handles each sensor event triggered by the train
        private void handleSensorEvent(SensorEvent sensor, int trainId, int speed, Set<Semaphore> acquiredSemaphores) 
                throws CommandException, InterruptedException {
            int sensorX = sensor.getXpos();
            int sensorY = sensor.getYpos();

            // Handle semaphore acquisition and release for track sections
            handleTrackSections(sensorX, sensorY, trainId, speed, acquiredSemaphores);
        }

        // Manages train's entry and exit from critical track sections (protected by semaphores)
        private void handleTrackSections(int sensorX, int sensorY, int trainId, int speed, Set<Semaphore> acquiredSemaphores) 
                throws CommandException, InterruptedException {
            
            // Handle station sensors (end-of-line): Train stops, reverses direction, and continues
            if ((sensorX == 16 && sensorY == 3) || (sensorX == 16 && sensorY == 5) || (sensorX == 16 && sensorY == 11) || (sensorX == 16 && sensorY == 13)) {
                manageStation(trainId, acquiredSemaphores);
            }
	        
	        // Black section: Sensors 3, 4, 5, 6
	        if ((sensorX == 6 && sensorY == 5) || (sensorX == 8 && sensorY == 5) || (sensorX == 11 && sensorY == 7) || (sensorX == 10 && sensorY == 8)) {
	            if (acquiredSemaphores.contains(black)) {
	            	acquiredSemaphores.remove(black);
		            black.release();
		        } else {
		        	manageSemaphore(black, trainId, speed, tsi, acquiredSemaphores);
	        	}
	        }
	
	        // Orange section: Sensors 7, 8
	        if ((sensorX == 14 && sensorY == 7) || (sensorX == 14 && sensorY == 8)) {
	            if (acquiredSemaphores.contains(orange)) {
	            	orange.release();
	            	acquiredSemaphores.remove(orange);
	            } else {
	            	manageSemaphore(orange, trainId, speed, tsi, acquiredSemaphores);
		            tsi.setSwitch(17, 7, acquiredSemaphores.contains(blue) ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
	        	}
	        }
	        
	        // Red/Blue/Green/DarkGreen section: Sensor 9/10
	        if (sensorX == 19 && sensorY == 9) {
	        	if (acquiredSemaphores.contains(green) || acquiredSemaphores.contains(darkGreen)) {
	        		if (acquiredSemaphores.contains(green)) {
	        			acquiredSemaphores.remove(green);
	        			green.release();
	        		}else {
	        			acquiredSemaphores.remove(darkGreen);
	        			darkGreen.release();
	        		}
		            acquireEither(red, blue, trainId, speed, tsi, acquiredSemaphores);
		            tsi.setSwitch(17, 7, acquiredSemaphores.contains(red) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
	        	} else {
	        		if (acquiredSemaphores.contains(red)) {
	        			acquiredSemaphores.remove(red);
	        			red.release();
	        		}else {
	        			acquiredSemaphores.remove(blue);
	        			blue.release();
	        		}
		            acquireEither(darkGreen, green, trainId, speed, tsi, acquiredSemaphores);
		            tsi.setSwitch(15, 9, acquiredSemaphores.contains(green) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
	        	}
	        }
	        
	        // Orange/Purple section: Sensors 11, 12
	        if ((sensorX == 9 && sensorY == 9) || (sensorX == 9 && sensorY == 10)) {
	            if (acquiredSemaphores.contains(orange)) {
	            	acquiredSemaphores.remove(orange);
	            	orange.release();
		            manageSemaphore(purple, trainId, speed, tsi, acquiredSemaphores);
		            tsi.setSwitch(4, 9, acquiredSemaphores.contains(green) ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
		            } else {
		            acquiredSemaphores.remove(purple);
	        		purple.release();
		            manageSemaphore(orange, trainId, speed, tsi, acquiredSemaphores);
		            tsi.setSwitch(15, 9, acquiredSemaphores.contains(green) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
	        	}
	        }
	
	        // Pink/Gray section: Sensor 13
	        if (sensorX == 1 && sensorY == 10) {
	        	if (acquiredSemaphores.contains(pink) || acquiredSemaphores.contains(grey)) {
	        		if (acquiredSemaphores.contains(pink)) {
	        			acquiredSemaphores.remove(pink);
	        			pink.release();
	        		}else {
	        			acquiredSemaphores.remove(grey);
	        			grey.release();
	        		}
		            acquireEither(darkGreen, green, trainId, speed, tsi, acquiredSemaphores);
		            tsi.setSwitch(4, 9, acquiredSemaphores.contains(green) ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
	        	} else {
	        		if (acquiredSemaphores.contains(green)) {
	        			acquiredSemaphores.remove(green);
	        			green.release();
	        		}else {
	        			acquiredSemaphores.remove(darkGreen);
	        			darkGreen.release();
	        		}
		            acquireEither(pink, grey, trainId, speed, tsi, acquiredSemaphores);
		            tsi.setSwitch(3, 11, acquiredSemaphores.contains(pink) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
	        	}
	        }
	
	        // Purple section: Sensors 14, 15
	        if ((sensorX == 6 && sensorY == 11) || (sensorX == 6 && sensorY == 13)) {
	        	if (acquiredSemaphores.contains(purple)) {
	        		acquiredSemaphores.remove(purple);
	        		purple.release();
	        	} else {
		            manageSemaphore(purple, trainId, speed, tsi, acquiredSemaphores);
		            tsi.setSwitch(3, 11, acquiredSemaphores.contains(pink) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
		        }
		    }
	    }
	    
	    
        // Initializes train by acquiring the initial semaphore
        private void initial(int trainId, int speed, TSimInterface tsi, Semaphore semaphore, Set<Semaphore> acquiredSemaphores) 
                throws CommandException, InterruptedException {
            acquiredSemaphores.add(semaphore);
            semaphore.acquire();
        }

        // Manages train behavior when arriving at the station
        private void manageStation(int trainId, Set<Semaphore> acquiredSemaphores) 
                throws CommandException, InterruptedException {
            tsi.setSpeed(trainId, 0); // Stop the train at the station
            this.sleep(2000);       // Wait for 2 seconds

            // Reverse the direction by inverting the speed and updating the global 'speed' variable
            this.speed = -this.speed; // Update the global speed variable
            tsi.setSpeed(trainId, this.speed); // Set the new reversed speed
            
            System.out.println("Train " + trainId + " reversed direction, new speed: " + this.speed);
        }

        // Acquires a semaphore for a train if available; otherwise, the train stops
        private void manageSemaphore(Semaphore semaphore, int trainId, int speed, TSimInterface tsi, Set<Semaphore> acquiredSemaphores) 
                throws CommandException, InterruptedException {
            if (!semaphore.tryAcquire()) {
                tsi.setSpeed(trainId, 0); // Stop the train if semaphore is not available
                semaphore.acquire(); // Wait until the semaphore can be acquired
                acquiredSemaphores.add(semaphore); // Track acquired semaphore
                tsi.setSpeed(trainId, this.speed); // Resume the train after acquiring semaphore
            } else {
                acquiredSemaphores.add(semaphore); // Track acquired semaphore
            }
        }

        // Acquires one of two semaphores (whichever is available); otherwise, the train stops
        private void acquireEither(Semaphore semaphore1, Semaphore semaphore2, int trainId, int speed, TSimInterface tsi, Set<Semaphore> acquiredSemaphores) 
                throws CommandException, InterruptedException {
            if (semaphore1.tryAcquire()) {
                acquiredSemaphores.add(semaphore1);
            } else {
                semaphore2.acquire();
                acquiredSemaphores.add(semaphore2);
            }
        }
    }
}
