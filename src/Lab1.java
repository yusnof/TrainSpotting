import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {

  Thread train1, train2; 
  TSimInterface tsi; 
  Semaphore blue, red, green, pink, grey; 
  SensorEvent sensorEvent; 

    // constructor for the lab1 where we will be .. 
    public Lab1(int speed1, int speed2) throws InterruptedException {
      tsi = TSimInterface.getInstance();
      blue = new Semaphore(1); 

      train1 = new Thread(() -> runTrain(1,speed1)); 
      train2 = new Thread(() -> runTrain(2,speed2)); 

      train1.start();
      train2.start();
    }

    // method that will be handling running of the train. 
    private void runTrain(int trainId, int speed){
      try {
        tsi.setSpeed(trainId,speed);
        while (true) {
          sensorEvent = tsi.getSensor(trainId);
          handleSensorEvent(sensorEvent, trainId, speed);  
        

        }
      }
      catch (CommandException e) {
        e.printStackTrace();    // or only e.getMessage() for the error
        System.exit(1);
      }
      catch (InterruptedException e){
        e.printStackTrace();    // or only e.getMessage() for the error
        System.exit(1);

      }
    }
    private void handleSensorEvent(SensorEvent sensor, int trainId, int speed) throws CommandException, InterruptedException{
      int x = sensor.getXpos(); 
      int y = sensor.getYpos();
      if (x == 5 && y == 11){
        handleTrainAtStation(trainId, speed);
      } 

    }
    private void handleTrainAtStation(int trainId, int speed) throws CommandException, InterruptedException{
      tsi.setSpeed(trainId, 0);
      Thread.sleep(2000);
      tsi.setSpeed(trainId, -speed);
    }

}
