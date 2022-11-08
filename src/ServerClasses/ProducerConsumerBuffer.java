package ServerClasses;

import shared.Message;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * ProducerConsumerBuffer is a class intended to hold requests of client threads that wish to send
 * Prepares to the network.
 */
public class ProducerConsumerBuffer {

    /**
     * A Semaphore that controls the number of empty places.
     */
    private final Semaphore empty_places_sem;

    /**
     * A Semaphore that controls the number of full places
     */
    private final Semaphore full_places_sem;


    /**
     * The index where the next producer shall place its request.
     */
    private int current_producer_place;

    /**
     * The Lock that protects the current producer place from concurrent access
     */
    private final Lock current_producer_place_mutex;

    /**
     * The index where the consumer shall take its next request
     */
    private int current_consumer_place;

    /**
     * The buffer where the requests shall be placed.
     */
    private final PrepareRequest[] request_buffer;


    /**
     * Constructs a ProducerConsumerBuffer.
     */
    ProducerConsumerBuffer(){
        //create the request buffer
        this.request_buffer = new PrepareRequest[20];
        for(int i=0 ; i != this.request_buffer.length; ++i){
            this.request_buffer[i] = new PrepareRequest();
        }

        //create the places Semaphores
        empty_places_sem = new Semaphore(this.request_buffer.length);
        full_places_sem = new Semaphore(0);

        //create the indexes of the producers and consumers
        current_consumer_place = 0;
        current_producer_place = 0;

        //create the mutex that protects the current producer place
        current_producer_place_mutex = new ReentrantLock();
    }

    public Message.TYPE_OF_MESSAGE placeRequestAndWaitForAnswer(Prepare request){
        try {
            //take out an empty place
            this.empty_places_sem.acquire();

            //get our place to put our request and increment the current producer place
            current_producer_place_mutex.lock();
            int place_to_put_request = current_producer_place;
            current_producer_place = (current_producer_place + 1) % this.request_buffer.length;
            current_producer_place_mutex.unlock();

            //put our request in that place
            this.request_buffer[place_to_put_request].prepare = request;

            //signal that we put a request by marking a place as full
            full_places_sem.release();

            //wait for our answer
            this.request_buffer[place_to_put_request].wait_for_answer.acquire();

            //store our answer to later return it
            Message.TYPE_OF_MESSAGE answer = this.request_buffer[place_to_put_request].answer;

            //mark the place as empty
            this.empty_places_sem.release();

            //return the answer
            return answer;
        } catch (InterruptedException ignored) {}

        return null;
    }


    public PrepareRequest takeOutRequest(){
        try {
            //take out a full place
            full_places_sem.acquire();

            //return the request that is at the current place to take out requests
            return this.request_buffer[this.current_consumer_place++];
        } catch (InterruptedException ignored) {}

        return null;
    }

    public void finishRequestProcessing(PrepareRequest processed_request){
        //wake up the requester to tell them that their answer is ready
        processed_request.wait_for_answer.release();
    }

    /**
     * Clears the buffer by setting it to the state if was after its constructor was called.
     */
    public void clearBuffer(){
        //clear the semaphores values
        empty_places_sem.drainPermits();
        full_places_sem.drainPermits();

        //clear the current places of the consumers and producers
        current_producer_place = 0;
        current_consumer_place = 0;
    }
}