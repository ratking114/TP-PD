package server.Threads;

import server.ServerClasses.PrepareRequest;
import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

/**
 * TransactionHandler is the Thread that is listening to Prepare send requests form threads
 * that service clients and this thread communicates to the network such prepares and handles
 * all the communication with the other peer servers.
 */
public class TransactionHandler extends Thread {
    private final ServerModel _servermodel;

    public TransactionHandler(ServerModel serverModel)
    {
        this._servermodel=serverModel;
    }

    @Override
    public void run() {
        while(true) {
            System.out.println("About to take a request!");


            //take out a request from the buffer
            PrepareRequest request = _servermodel.getProducerConsumerBuffer().takeOutRequest();

            System.out.println("Got a request!");

            //dont forget that we already have the database lock because no one else exept our requester
            //can be making chnges to the database
            System.out.println("We got the server.database lock!");

            //before sending the request lets see if there are other servers because if there arent then
            //we dont need to send the PREPARE
            if(_servermodel.getAliveServerList().size() != 0) {
                //process the request
                DatagramSocket receive_answer = null;
                try {
                    //create the socket to receive the answer from our peers
                    receive_answer = new DatagramSocket();
                    receive_answer.setSoTimeout(1000);

                    //place the response port in the Prepare message
                    request.prepare.answer_udp_port = receive_answer.getLocalPort();


                    //send the prepare message to the network
                    System.out.println("Sending PREPARE to other Servers!");
                    _servermodel.sendMessage(
                            new Message(Message.TYPE_OF_MESSAGE.PREPARE, request.prepare)
                    );
                } catch (IOException e) {
                    System.out.println(e);
                }

                //wait for each response, we must have one response per alive server
                int number_of_tries = 2;
                while (number_of_tries-- != 0) {
                    try {
                        int number_of_alive_servers = _servermodel.getAliveServerList().size();
                        int confirmed_servers = 0;
                        if (number_of_alive_servers != 0) {
                            do {
                                //wait 1sec for a response
                                DatagramPacket answer = new DatagramPacket(new byte[256], 256);
                                receive_answer.receive(answer);
                                ++confirmed_servers;
                            } while (number_of_alive_servers != confirmed_servers);

                            //all servers answered so we can leave the while(wait for confirmations)
                            System.out.println("Received all the confirmations! Sending the COMMIT");
                            _servermodel.sendMessage(
                                    new Message(Message.TYPE_OF_MESSAGE.COMMIT, request.prepare.prepare_number)
                            );

                            //signal to the requester that everything went fine
                            request.answer = Message.TYPE_OF_MESSAGE.COMMIT;
                            _servermodel.getProducerConsumerBuffer().finishRequestProcessing(request);
                            break;
                        }
                    } catch (SocketTimeoutException timeout_exception) {
                        System.out.println(timeout_exception);
                        if (number_of_tries == 1)
                            continue;
                        try {
                            //a Server did not answer so we must send an abort message to the network
                            System.out.println("Sending ABORT");
                            _servermodel.sendMessage(
                                    new Message(Message.TYPE_OF_MESSAGE.ABORT, request.prepare.prepare_number)
                            );

                            //notify our requester that the operation failed
                            request.answer = Message.TYPE_OF_MESSAGE.ABORT;
                            _servermodel.getProducerConsumerBuffer().finishRequestProcessing(request);
                        } catch (IOException ignored) {
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
            //we are the only server and we can update the server.database directly
            else{
                //signal to the requester that everything went fine
                request.answer = Message.TYPE_OF_MESSAGE.COMMIT;
                _servermodel.getProducerConsumerBuffer().finishRequestProcessing(request);
            }
        }

    }
}

