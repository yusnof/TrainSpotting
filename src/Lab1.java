import TSim.*;
import java.util.concurrent.Semaphore;

public class Lab1 {
  

    // Semaphores for controlling access to critical sections (shared track areas)
    private final Semaphore blue = new Semaphore(1);
    private final Semaphore red = new Semaphore(1);
    private final Semaphore black = new Semaphore(1);
    private final Semaphore orange = new Semaphore(1);
    private final Semaphore green = new Semaphore(1);
    private final Semaphore darkGreen = new Semaphore(1);
    private final Semaphore purple = new Semaphore(1);
    private final Semaphore pink = new Semaphore(1);
    private final Semaphore grey = new Semaphore(1);

    final TSimInterface tsi; 

    // Train IDs for northbound and southbound trains
    private final int trainNorthId = 2; // Northbound train ID
    private final int trainSouthId = 1; // Southbound train ID

    Thread train1, train2; 

    // Constructor to initialize and start both trains
    public Lab1(int speed1, int speed2) {

       tsi = TSimInterface.getInstance();

      train1 =  new Train(2,2); //new Thread(() -> controlTrain(trainSouthId,speed1)); 
      train2 = new Train (2,2); //new Thread(() -> controlTrain(trainNorthId,speed2)); 

      train1.start();
      train2.start();

      
    }

    // Main control method for handling each train's behavior
    private void controlTrain(int trainId, int speed) {
  
        try {
            // Set the initial speed of the train
            tsi.setSpeed(trainId, speed);

            // Continuously listen for sensor events for the train
            while (true) {
                SensorEvent sensor = tsi.getSensor(trainId); // Wait for a sensor event
                if (sensor.getStatus() == SensorEvent.ACTIVE) { // If sensor is activated
                    handleSensorEvent(sensor, trainId, speed); // Handle the sensor event
                }
            }
        } catch (CommandException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1); // Exit if an exception occurs
        }
    }

    // Determine if the train is coming from the north (based on its ID and speed)
    private boolean isComingFromNorth(int trainId, int speed) {
        return (trainId == trainNorthId && speed > 0) || (trainId == trainSouthId && speed < 0);
    }

    // Determine if the train is coming from the south (based on its ID and speed)
    private boolean isComingFromSouth(int trainId, int speed) {
        return (trainId == trainNorthId && speed < 0) || (trainId == trainSouthId && speed > 0);
    }

    // Handle sensor events triggered by trains
    private void handleSensorEvent(SensorEvent sensor, int trainId, int speed) throws CommandException, InterruptedException {
        int sensorX = sensor.getXpos(); // Get sensor X position
        int sensorY = sensor.getYpos(); // Get sensor Y position

        // Check the direction of the train (coming from north or south)
        boolean comingFromNorth = isComingFromNorth(trainId, speed);
        boolean comingFromSouth = isComingFromSouth(trainId, speed);

        System.out.println ("Call the event"+ comingFromNorth + " " + comingFromSouth); 
        System.out.println(sensorX + " " + sensorY ); 

        // End of track sensors: When train reaches end, stop and reverse direction
        if ((sensorX == 14 && sensorY == 3) || 
            (sensorX == 14 && sensorY == 5) || 
            (sensorX == 14 && sensorY == 11) || 
            (sensorX == 14 && sensorY == 13)) {
            // Train has reached the end of the track
            tsi.setSpeed(trainId, 0);        // Stop the train
            Thread.sleep(2000);              // Wait for 2 seconds
            tsi.setSpeed(trainId, -speed);   // Reverse the train direction
            return;                          // Exit after reversing
        }
        // Handle other sensor positions that deal with shared critical sections
        handleTrackSections(sensorX, sensorY, trainId, speed, comingFromNorth, comingFromSouth);
    }

    // Handle the logic for trains entering or leaving critical track sections using semaphores
    private void handleTrackSections(int sensorX, int sensorY, int trainId, int speed, boolean comingFromNorth, boolean comingFromSouth) throws CommandException, InterruptedException {
        

        // Section: Black
        if ((sensorX == 6 && sensorY == 5) || (sensorX == 8 && sensorY == 5)) {
          System.out.print("Black"); 
            // Train entering black section from the north
            if (comingFromNorth) {
                if (black.tryAcquire()) {
                    black.acquire();
                    tsi.setSpeed(trainId, speed); // Continue if section is available
                } else {
                    tsi.setSpeed(trainId, 0); // Stop if section is occupied
                    // Wait until section becomes available
                    black.acquire();
                    tsi.setSpeed(trainId, speed); // Resume after acquiring section
                }
            } else if (comingFromSouth) {
                black.release();  // Release semaphore when leaving section
                tsi.setSpeed(trainId, speed);
            }
        }
        if ((sensorX == 11 && sensorY == 7) || (sensorX == 10 && sensorY == 8)) {
            // Train entering black section from the south
            if (comingFromSouth) {
                if (black.tryAcquire()) {
                  black.acquire();
                    tsi.setSpeed(trainId, speed);
                } else {
                    tsi.setSpeed(trainId, 0); // Stop if section is occupied
                    // Wait until section becomes available
                    black.acquire();
                    tsi.setSpeed(trainId, speed); // Resume after acquiring section
                }
            } else if (comingFromNorth) {
                black.release();  // Release semaphore when leaving section
                tsi.setSpeed(trainId, speed);
            }
        }

        // Section: Blue
        if (sensorX == 16 && sensorY == 7) {
            // Train coming from the north
            if (comingFromNorth) {
                if (orange.tryAcquire()) {
                    orange.acquire();
                    tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT); // Set switch to right
                    blue.release();  // Release blue section
                    tsi.setSpeed(trainId, speed);
                } else {
                    tsi.setSpeed(trainId, 0); // Stop if section is occupied
                    orange.acquire();
                    tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT); // Set switch to right
                    blue.release();
                    tsi.setSpeed(trainId, speed); // Resume after acquiring section
                }
            } else if (comingFromSouth) {
                orange.release();  // Release section after passing
                tsi.setSpeed(trainId, speed);
            }
        }

        // Section: Red 
        if (sensorX == 16 && sensorY == 8) {
            if (comingFromNorth) {
                if (orange.tryAcquire()) {
                    orange.acquire();
                    tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT); // Set switch to left
                    red.release();  // Release red section
                    tsi.setSpeed(trainId, speed);
                } else {
                    tsi.setSpeed(trainId, 0); // Stop if section is occupied
                    orange.acquire();
                    tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT); // Set switch to left
                    red.release();
                    tsi.setSpeed(trainId, speed); // Resume after acquiring section
                }
            } else if (comingFromSouth) {
                orange.release();  // Release section after passing
                tsi.setSpeed(trainId, speed);
            }
        }

        // Section: Orange
        if (sensorX == 19 && sensorY == 7) {
            if (comingFromSouth) {
                if (blue.tryAcquire()) {
                    blue.acquire();
                    tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT); // Set switch to right
                    tsi.setSpeed(trainId, speed);
                } else if (red.tryAcquire()) {
                    red.acquire();
                    tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT); // Set switch to left
                    tsi.setSpeed(trainId, speed);
                }
            }
        }

        // Section: Green
        if (sensorX == 9 && sensorY == 9) {
           System.out.println("Green"); 
            if (comingFromNorth) {
                orange.release(); // Release orange section when entering green
                if (purple.tryAcquire()) {
                    purple.acquire();
                    tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT); // Switch to left
                    tsi.setSpeed(trainId, speed);
                    green.release();
                } else {
                    tsi.setSpeed(trainId, 0); // Stop if section is occupied
                    purple.acquire();
                    tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT); // Switch to left
                    tsi.setSpeed(trainId, speed);
                    green.release();
                }
            } else if (comingFromSouth) {
               System.out.print("here"); 
                purple.release(); // Release purple section when entering green
                if (orange.tryAcquire()) {
                    orange.acquire();
                    tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT); // Switch to right
                    tsi.setSpeed(trainId, speed);
                } else {
                    tsi.setSpeed(trainId, 0); // Stop if section is occupied
                    orange.acquire();
                    tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT); // Switch to right
                    tsi.setSpeed(trainId, speed);
                }
            }
        }
        
        // Section: DarkGreen
        if (sensorX == 4 && sensorY == 10) {
            if (comingFromNorth) {
                if (darkGreen.tryAcquire()) {
                    darkGreen.acquire();
                    tsi.setSpeed(trainId, speed);  // Continue if the section is free
                } else {
                    tsi.setSpeed(trainId, 0);  // Stop if section is occupied
                    // Wait until section becomes available
                    darkGreen.acquire();
                    tsi.setSpeed(trainId, speed);  // Resume after acquiring the section
                }
            } else if (comingFromSouth) {
                darkGreen.release();  // Release semaphore after leaving the section
                tsi.setSpeed(trainId, speed);  // Continue
            }
        }

        // Section: Purple 
        if (sensorX == 1 && sensorY == 10) {
            if (comingFromNorth) {
                if (purple.tryAcquire()) {
                    purple.acquire();
                    tsi.setSpeed(trainId, speed);  // Continue if the section is free
                } else {
                    tsi.setSpeed(trainId, 0);  // Stop if section is occupied
                    // Wait until section becomes available
                    purple.acquire();
                    tsi.setSpeed(trainId, speed);  // Resume after acquiring the section
                }
            } else if (comingFromSouth) {
                purple.release();  // Release semaphore after leaving the section
                tsi.setSpeed(trainId, speed);  // Continue
            }
        }

        // Section: Grey 
        if ((sensorX == 6 && sensorY == 11) || (sensorX == 8 && sensorY == 11)) {
            if (comingFromNorth) {
                if (grey.tryAcquire()) {
                    grey.acquire();
                    tsi.setSpeed(trainId, speed);  // Continue if the section is free
                } else {
                    tsi.setSpeed(trainId, 0);  // Stop if section is occupied
                    // Wait until section becomes available
                    grey.acquire();
                    tsi.setSpeed(trainId, speed);  // Resume after acquiring the section
                }
            } else if (comingFromSouth) {
                grey.release();  // Release semaphore after leaving the section
                tsi.setSpeed(trainId, speed);  // Continue
            }
        }

        // Section: Pink 
        if ((sensorX == 6 && sensorY == 13) || (sensorX == 8 && sensorY == 13)) {
            if (comingFromSouth) {
                if (pink.tryAcquire()) {
                    pink.acquire();
                    tsi.setSpeed(trainId, speed);  // Continue if the section is free
                } else {
                    tsi.setSpeed(trainId, 0);  // Stop if section is occupied
                    // Wait until section becomes available
                    pink.acquire();
                    tsi.setSpeed(trainId, speed);  // Resume after acquiring the section
                }
            } else if (comingFromNorth) {
                pink.release();  // Release semaphore after leaving the section
                tsi.setSpeed(trainId, speed);  // Continue
            }
        }
    }
    class Train extends Thread  {
      int id; 
      int speed; 
       
      Train (int id, int speed){
        this.id = id; 
        this.speed = speed; 
      }; 
      
       
      void me(){
    
      }; 
    
    }
}

class Train extends Thread  {
  int id; 
  int speed; 
  TSimInterface tsim; 
   
  Train (int id, int speed, TSimInterface tsim){
    this.id = id; 
    this.speed = speed;
  }; 
  
   
  void me(){

  }; 

}
