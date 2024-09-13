import java.util.concurrent.Semaphore;
import java.awt.Point; 

import TSim.*;

public class Lab1 {
  
  TSimInterface tsi = TSimInterface.getInstance();

  final int trainId1 = 1; 
  final int trainId2 = 2; 
  final int MAX_SPEED = 20; 
  int xPos; 
  int yPos; 
  Semaphore orange = new Semaphore(1); 
  Semaphore red = new Semaphore(1); 

  
  
  // Semaphore blue, red, green, pink, grey; 
  SensorEvent sensorEvent; 
  // will calculating from top top down ToDo::declaring the hardcoded part of the Point. 
  final Point switch1 = new Point(3, 11);
  final Point switch2 = new Point(4, 9); 
  final Point switch3 = new Point(15, 9);
  final Point switch4 = new Point(17, 7);  
  
  Point sensorPoint;   
    // constructor for the lab1 where we will be .. 
    public Lab1 (int speed1, int speed2) throws InterruptedException {
    
      // two object of our train thread. 
      ThreadTrain train1 = new ThreadTrain(trainId1, speed1); 
      ThreadTrain train2 = new ThreadTrain(trainId2, speed2); 

      //starting of the thread which forces the thread to execute the run() method. 
      train1.start();
      train2.start();
      
      
    }

    // method that will be handling running of the train. 
    
    private void handleSensorEvent(SensorEvent sensor, int trainId, int speed) throws CommandException, InterruptedException{
      xPos = sensor.getXpos(); 
      yPos = sensor.getYpos();
      System.out.println("I was at sensorEven ");
    
      handleTrainAtStation(xPos, yPos, trainId, speed);
      handleTrainSemaphore(xPos, yPos, trainId, speed);
      
    }


    private void handleTrainAtStation(int xPos, int yPos, int trainId, int speed) throws CommandException, InterruptedException{
      if((xPos == 16 && yPos == 11) || (xPos == 16 && yPos == 13)){
        System.out.println("I was at the station ");
      tsi.setSpeed(trainId, 0);
       Thread.sleep(2000); 
       tsi.setSpeed(trainId, -speed);
      }
    }

    private void handleTrainSemaphore(int xPos, int yPos, int trainId, int speed) throws CommandException, InterruptedException{

      
      if((xPos == 4 && yPos == 11) || (xPos == 4 && yPos == 13)){ //first and second signal
          if(!orange.tryAcquire()){
            tsi.setSpeed(trainId, 0);

            orange.acquire();
            tsi.setSpeed(trainId, speed);
          }
          if (yPos == 11){tsi.setSwitch(3, 11, tsi.SWITCH_LEFT);}
          else{tsi.setSwitch(3, 11, tsi.SWITCH_RIGHT);}
          orange.release();
      }
    
    
      }
      
    
      class ThreadTrain extends Thread implements Runnable{
        int trainId; 
        int speed; 
        dir direction; 
  
        public ThreadTrain(int trainId, int speed){
          this.trainId = trainId; 
          this.speed = speed;
          // debatable
          if(trainId == 1){
            direction = dir.SOUTH;
          };
          if(trainId == 2){
            direction = dir.NORTH;
          };
        }
  
        @Override
        public void run() {
          try {
            tsi.setSpeed(trainId,speed);
  
            while (true) {
              sensorEvent = tsi.getSensor(trainId);
              if(sensorEvent.getStatus() == sensorEvent.ACTIVE){
              handleSensorEvent(sensorEvent, trainId, speed); };
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
        }
        enum dir{
          NORTH,SOUTH
        }

    }



    




