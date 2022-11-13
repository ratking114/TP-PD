package client;

import shared.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * This Thread is a fundamental part of the client as this thread is the one
 * that reads updates from the Server and that prints them to the console.
 * This Thread will also try to connect to another Server when the connection goes down.
 */
public class ReadFromKeyboardThread extends Thread {
    private Client _client;


    public ReadFromKeyboardThread(Client client){
        _client = client;
    }

    @Override
    public void run() {
        //create the Scanner to read from the keyboard
        Scanner stdin = new Scanner(System.in);

        //loop indefinitely
        while(true){
            try {
                //print a prompt
                System.out.print("Client>");

                //read a line from the keyboard and write it to the Pipe
                _client.getPipeSinkChannelToWriteKeyboardInformation().write(
                        ByteBuffer.wrap(stdin.nextLine().getBytes())
                );
            } catch (IOException e) {
                System.out.println(e);
                e.printStackTrace();
            }
        }
    }
}
