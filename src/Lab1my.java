import java.util.concurrent.Semaphore;
import java.awt.Point; 

import TSim.*;

public class Lab1my {
  
  
  final Thread train1, train2; 
  final TSimInterface tsi; 
  Semaphore blue, red, green, pink, grey; 
  SensorEvent sensorEvent; 
  // will calculating from top top down ToDo::declaring the hardcoded part of the Point. 
  final Point switch1 = new Point(0, 0);
  final Point switch2 = new Point(0, 0); 
  final Point switch3 = new Point(0, 0);
  final Point switch4 = new Point(0, 0);  
  Point sensorPoint;   
  

    // constructor for the lab1 where we will be .. 
    public void Lab1(int speed1, int speed2) throws InterruptedException {
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
          if(sensorEvent.getStatus() == sensorEvent.ACTIVE){
          handleSensorEvent(sensorEvent, trainId, speed);  
          };
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
      sensorPoint = new Point(sensor.getXpos(), sensor.getYpos()); 
      int x = sensor.getXpos(); 
      int y = sensor.getYpos();
      
      if (x == 5 && y == 11){
        handleTrainAtStation(trainId, speed);
      } 


    }
    private void handleTrainAtStation(int trainId, int speed) throws CommandException, InterruptedException{
      tsi.setSpeed(trainId, 0);
      // this solution is not very correct, because i am not sure what thread will sleep. 
      if(trainId == 1){
        //train1.sleep(3000);
        Thread.sleep(3000);
      }
      else if(trainId == 2){
        // train2.sleep(3000); 
        Thread.sleep(3000);
      }
      else{
        throw new InterruptedException ("not valid id");
      }
       tsi.setSpeed(trainId, -speed);
    }
    private void handleSwitches(int xPos, int yPos, int switchDir ) throws CommandException, InterruptedException{
      //introduce the logic behind the Semaphore and to switch 
      tsi.setSwitch(0, 0, 0);
    }
}


