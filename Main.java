import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.lang.*;
import java.io.*;


public class Main 
{
  
  public Semaphore t_Buy= new Semaphore(2);//Ticket Buy
  public Semaphore t_Service= new Semaphore(0);//Ticket Service
  public Semaphore t_Sell = new Semaphore(0); //Ticket Sell
  
  
  public Semaphore c_Buy= new Semaphore(1);//Concession Buy
  public Semaphore c_Service= new Semaphore(0);//Concession Service
  public Semaphore c_Sell= new Semaphore(0);//Concession Sell
  
  public Semaphore t_Give= new Semaphore(1);//Ticket give to taker
  public Semaphore t_Take= new Semaphore(0);//Ticket take from customer
  public Semaphore go_In= new Semaphore(0);//Go in 
  
  public Semaphore[] finish= new Semaphore[50];//Customer thread is done running

  Queue<Integer> boxLine = new LinkedList<>(); //line for box office with customer id
  Queue<String> boxTicket = new LinkedList<>(); //line for box office with customer movie ticket
  
  Queue<Integer> concessionLine = new LinkedList<>();//line for concession stand with customer id
  Queue<Integer> concessionOrder = new LinkedList<>(); //line for conession stand with customer meal/drink
  Queue<Integer> TicketLine = new LinkedList<>();//line for ticket taker with customer id
  
  List<Integer> availableTickets = new ArrayList<Integer>();//available tickets for the movies
  List<String> movieName = new ArrayList<String>();//the name for the available tickets

  public static void main(String args[]) throws FileNotFoundException
  {
    Main run = new Main();
    run.theatre();//runs the program
  }
  
  public void theatre() throws FileNotFoundException {
  //Gets the movie name and the available tickets
    Scanner Scan = new Scanner(new File("movies.txt"));
    while (Scan.hasNextLine()){
      String name = Scan.next();
      while(Scan.hasNext()){
        String moreName = Scan.next();
        if(moreName.matches(".*\\d+.*")){
          movieName.add(name);
          availableTickets.add(Integer.parseInt(moreName));
          moreName="";
          name="";
        }
        else
          name+=" "+moreName;
      }
    }
    //Initiallizes box office, concession stand, and ticket taker threads 
    Thread[] customers = new Thread[50];
    Thread[] office = new Thread[2];
    office[0] =new BoxOffice(0);
    office[1] =new BoxOffice(1);
    //Run the office thread
    office[0].start();
    office[1].start();
    
    Thread stand = new ConcessionWorker();
    Thread taker = new TicketTaker();
    stand.start();//run the concession stand thread
    taker.start();//run the ticket taker stand    

    System.out.println("Theatre is open");

    for(int i = 0; i < 50; i++){//initializes the customer thread and finish semaphores
      customers[i] = new Customer(i);
      finish[i] = new Semaphore(0);
    }
    
    for(int i = 0; i < 50; i++){//runs the customer thread
      customers[i].start(); 
    }
    
    for(int i =0; i < 50;i++){//checks if each customer thread is done running
      try{
        finish[i].acquire();
      }catch (InterruptedException e){}
    }
    System.exit(0);//Ends the program
  }
  
  
  
  class Customer extends Thread{
    private int cusNum; //Customer Number
    private String ticket;//Movie customer wants to see
    public int order;//Order meal/drinks for the concession stand
    
    Customer(int i)
    {
      cusNum = i;
    }
    
    public void run()
    {
      try{
        //Buying tickets
        t_Buy.acquire();
        ticket();       
        boxLine.add(cusNum);
        boxTicket.add(ticket);
        t_Service.release();
        t_Sell.acquire();
        
        //Going to the concession
        if(Math.random()>.5){
          concession();
          c_Buy.acquire();
          concessionLine.add(cusNum);
          concessionOrder.add(order);
          c_Service.release();
          c_Sell.acquire();
          if(order == 0)
            System.out.println("Customer "+cusNum+" receives popcorn");
          else if(order ==1)
            System.out.println("Customer "+cusNum+" receives soda");
          else
            System.out.println("Customer "+cusNum+" receives popcorn and soda");
        }
        
        //Giving the ticket taker the ticket
        t_Give.acquire();
        System.out.println("Customer "+cusNum+" in line to see ticket taker");
        TicketLine.add(cusNum);
        t_Take.release();
        
	//Ends the customer thread
        go_In.acquire();
        System.out.println("Customer "+cusNum+" enters theater to see "+ticket);
        System.out.println("Joined Customer "+cusNum);
        finish[cusNum].release();
        join();
        
      } catch(InterruptedException e){}
    }
    public void concession(){
      order = (int)(Math.random()*3);
      //if order = 0, it is popcorn; order = 1, soda; order = 2, both
      if(order == 0)
        System.out.println("Customer "+cusNum+" is in line to buy popcorn");
      else if(order ==1)
        System.out.println("Customer "+cusNum+" is in line to buy soda");
      else
        System.out.println("Customer "+cusNum+" is in line to buy popcorn and soda");
    }
    public void ticket(){
      int x = (int)(Math.random()*5);
      ticket = movieName.get(x);
      if(availableTickets.get(x)==0){//Checks if there are no tickets available
        try{
	//Since there are none, thread ends and prints it has left the theater
          System.out.println(movieName.get(x)+" is not available for customer "+cusNum);
          System.out.println("Customer "+cusNum+" is leaving the theater");
          System.out.println("Joined customer "+cusNum);
          t_Buy.release();
          finish[cusNum].release();
          join();} catch (InterruptedException e){}
      }
      else{//Since there are tickets available, buy them and decrement the availablitity of tickets
        System.out.println("Customer "+cusNum+" created, buying ticket to " + ticket);
        availableTickets.set(x,availableTickets.get(x)-1);
      }
    }
  }
  
  class BoxOffice extends Thread{
    private int officeId;//ID of the box office
    BoxOffice(int i)
    {
      officeId=i;
    }
    
    public void run()
    {
      System.out.println("Box office agent "+officeId+" created");
      while(true){
        int customerNum;//Customer id box office is serving
        String movie;//movie name customer wants to have a ticket of
        try{
          t_Service.acquire();
          customerNum = boxLine.poll();
          movie = boxTicket.poll();
          TimeUnit.SECONDS.sleep(90/60);
          System.out.println("Box office agent " +officeId+" servicing customer "+customerNum);
          System.out.println("Box office agent "+officeId+" sold ticket for "+movie+" to customer "+customerNum);
          t_Buy.release();
          t_Sell.release();
        } catch(InterruptedException e){}
      }
      
    }
  }
  
  class ConcessionWorker extends Thread{
    ConcessionWorker(){}
    public void run()
    {
      System.out.println("Concessions stand worker is created");
      while(true){
        int cusNum;//Customer id the worker is serving
        int order;//Order number the worker needs to fulfill
        try{
          c_Service.acquire();
          TimeUnit.SECONDS.sleep(180/60);
          cusNum=concessionLine.poll();
          order = concessionOrder.poll();
          takeOrder(order,cusNum);
          c_Sell.release();
          c_Buy.release();
        } catch(InterruptedException e){}
      }
    }
    public void takeOrder(int order,int cusNum){
      if(order==0){
        System.out.println("Order taken for Popcorn for Customer "+cusNum);
        System.out.println("Popcorn given to customer "+cusNum);
      }
      else if(order==1){
        System.out.println("Order taken for Soda for Customer "+cusNum);
        System.out.println("Soda given to customer "+cusNum);
      }
      else{  
        System.out.println("Order taken for Popcorn and Soda for Customer "+cusNum);
        System.out.println("Popcorn and Soda given to customer "+cusNum);
      }
    }
  }
  
  class TicketTaker extends Thread{
    TicketTaker(){}
    public void run()
    {
      System.out.println("Ticket taker created");
      while(true){
        try{
          t_Take.acquire();
          TimeUnit.SECONDS.sleep(15/60);
          System.out.println("Ticket taken from customer "+TicketLine.poll());
          t_Give.release();
          go_In.release();
        } catch(InterruptedException e){}
      }
    }
  }
}
