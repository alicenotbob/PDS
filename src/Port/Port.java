package Port;

import Pier.Pier;
import Ship.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by Tarasevich Vladislav on 09.04.2017.
 * This class used to store the state of the warehouse and work with ships through the piers
 * @version 1.0
 */
public class Port
{
    private final int COUNT_OF_PIERS =  5;
    private final int QUEUE_SIZE     = 20;

    private final Semaphore semaphore = new Semaphore(1);

    private Warehouse warehouse;
    private ArrayBlockingQueue<Ship> shipRequests;
    private ObservableList<Ship> shipRequestsList = FXCollections.observableArrayList();
    private ObservableList<State> statusLog = FXCollections.observableArrayList();
    private Pier pier[];
    private ShipGenerator shipGenerator;

    private boolean processing;
    private boolean suspended;

    public Port()
    {
        processing = false;
        suspended  = false;

        warehouse  = new Warehouse();
        shipRequests = new ArrayBlockingQueue<Ship>(QUEUE_SIZE);
        shipRequestsList.addAll(shipRequests);
        pier = new Pier[COUNT_OF_PIERS];
        shipGenerator = new ShipGenerator(this);
        shipGenerator.setDaemon(true);

        for(int i = 0; i < COUNT_OF_PIERS; ++i)
        {
            pier[i] = new Pier(this);
            pier[i].setDaemon(true);
        }
    }

    public ObservableList<Ship> getShipRequestsData()
    {
        return shipRequestsList;
    }

    public ObservableList<State> getStatusLog()
    {
        return statusLog;
    }

    /**
     * method, used to view current state of queue of requests;
     * @return array of ships (requests)
     */
    public Ship[] getShipRequests()
    {
        return shipRequests.toArray(new Ship[shipRequests.size()]);
    }

    /**
     * This method used to get started Working
     */
    public synchronized void getStarted()
    {
        if(!processing)
        {
            for (int i = 0; i < COUNT_OF_PIERS; ++i)
            {
                pier[i].start();
            }
            shipGenerator.start();
            processing = true;
        }
    }

    /**
     * Used to paused working of all the Piers
     */
    public synchronized void suspendProcess()
    {
        if(processing && !suspended)
        {
            suspended = true;
            shipGenerator.suspend();
            for (int i = 0; i < COUNT_OF_PIERS; ++i)
            {
                pier[i].suspend();
            }
        }
    }

    /**
     * Used to resume working of all the Piers
     */
    public synchronized void resumeProcess()
    {
        if(processing && suspended)
        {
            suspended = false;
            shipGenerator.resume();
            for (int i = 0; i < COUNT_OF_PIERS; ++i)
            {
                pier[i].resume();
            }
        }
    }

    /**
     * Used for stop processing
     */
    public synchronized void stopProcess()
    {
        shipGenerator.stop();
        for(int i = 0; i < COUNT_OF_PIERS; ++i)
        {
            pier[i].stop();
        }
    }

    public synchronized void suspendGenerator()
    {
        shipGenerator.suspend();
    }

    public synchronized void resumeGenerator()
    {
        shipGenerator.resume();
    }

    /**
     * This method used to trying to take some cargo from the warehouse
     * @param cargo type of cargo, needed to take
     * @param count count of type of cargo, needed to take
     * @return true, if you can take such count of such cargo
     */
    public synchronized boolean takeCargo(Cargo cargo, int count)
    {
        return warehouse.takeCargo(cargo, count);
    }

    /**
     * This method used to put some cargo into the warehouse
     * @param cargo type of cargo, wanted to put
     * @param count count of type of cargo, wanted to put
     */
    public synchronized void putCargo(Cargo cargo, int count)
    {
        warehouse.putCargo(cargo, count);
    }

    /**
     * This method used to take first ship request in the queue.
     * @return first ship Request in the queue of ship requests.
     * @throws InterruptedException
     */
    public synchronized Ship takeCurrentRequest() throws InterruptedException
    {
        Ship result = shipRequests.take();
        semaphore.acquire();
        shipRequestsList.remove(0);
        semaphore.release();
        return result;
    }

    /**
     * This method used to putting current request to the queue of requests.
     * @param currentShip putting to the queue of requests.
     * @throws InterruptedException
     */
    public void putCurrentRequest(Ship currentShip) throws InterruptedException
    {
        shipRequests.put(currentShip);
        semaphore.acquire();
        shipRequestsList.add(currentShip);
        semaphore.release();
    }
}
