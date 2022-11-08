package Threads;

import ServerClasses.PrepareRequest;
import ServerClasses.ServerHeartBeatInfo;
import ServerClasses.ServerModel;
import shared.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

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
            //take out a request from the buffer
            PrepareRequest request = _servermodel.getProducerConsumerBuffer().takeOutRequest();

            //see if we can process the request



            //process the request
            DatagramSocket receive_answer = null;
            try {
                //create the socket to receive the answer from our peers
                receive_answer = new DatagramSocket(request.prepare.answer_udp_port);
                receive_answer.setSoTimeout(1000);

                //place the response port in the Prepare message
                request.prepare.answer_udp_port = receive_answer.getLocalPort();

                //change the mode of the UDPMulticastListenerThread to the


                //send the prepare message to the network
                _servermodel.sendMessage(
                        new Message(Message.TYPE_OF_MESSAGE.PREPARE, request.prepare)
                );
            } catch (IOException ignored) {
            }

            //wait for each response, we must have one response per alive server

            int number_of_tries = 2;
            try {
                while (number_of_tries-- != 0) {
                    int number_of_alive_servers = _servermodel.getAliveServerList().size();
                    if (number_of_alive_servers != 0) {
                        do {
                            //wait 1sec for a response
                            DatagramPacket answer = new DatagramPacket(new byte[256], 256);
                            receive_answer.receive(answer);
                        } while (number_of_alive_servers-- != 0);

                        //all servers answered so we can leave the while(wait for confirmations)
                        _servermodel.sendMessage(
                                new Message(Message.TYPE_OF_MESSAGE.COMMIT, request.prepare.prepare_number)
                        );

                        //signal to the requester that everything went fine
                        request.answer = Message.TYPE_OF_MESSAGE.COMMIT;
                        _servermodel.getProducerConsumerBuffer().finishRequestProcessing(request);
                        break;
                    }
                }
            }
            catch(SocketTimeoutException timeout_exception){
                if(number_of_tries==1)
                    continue;
                try {
                    //a Server did not answer so we must send an abort message to the network
                    _servermodel.sendMessage(
                            new Message(Message.TYPE_OF_MESSAGE.ABORT, request.prepare.prepare_number)
                    );

                    //notify our requester that the operation failed
                    request.answer = Message.TYPE_OF_MESSAGE.ABORT;
                    _servermodel.getProducerConsumerBuffer().finishRequestProcessing(request);
                } catch (IOException ignored) {}
            } catch(IOException ignored){}
        }

    }
}

