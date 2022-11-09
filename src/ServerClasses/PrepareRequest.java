package ServerClasses;

import shared.Message;

import java.util.concurrent.Semaphore;

/**
 * PrepareRequest is a request that is made to the Thread that controls the ProducerConsumerBuffer.
 */
public class PrepareRequest {
    public PrepareRequest(){
        //create the semaphore to wait for an answer
        wait_for_answer = new Semaphore(0);
    }

    /**
     * The Prepare message that is to send to the other servers.
     */
    public Prepare prepare;

    /**
     * The semaphore where the requester will wait for its answer.
     */
    public Semaphore wait_for_answer;

    /**
     * The answer from the receiver of the request.
     */
    public Message.TYPE_OF_MESSAGE answer;
}
