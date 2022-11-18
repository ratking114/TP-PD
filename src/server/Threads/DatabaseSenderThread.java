package server.Threads;

import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * DatabaseSenderThread is a simple thread that only sends the Database file trough a socket
 */
public class DatabaseSenderThread extends Thread {
    private final SocketChannel _to_send_socket_channel;
    private final ServerModel _server_model;

    public DatabaseSenderThread(ServerModel server_model, SocketChannel to_send_socket_channel){
        _to_send_socket_channel = to_send_socket_channel;
        _server_model = server_model;
    }

    @Override
    public void run() {
        //send the server.database file to the other server
        try {
            _server_model.newTcpConnection();
            _server_model.lockDatabase();
            _server_model.getEventDatabase().closeDatabaseConnection();


            //now send the database file in chunks
            FileInputStream database_file = new FileInputStream(new File(_server_model.getDatabaseFileName().toUri()));
            while(true){
                byte[] bytes_read = database_file.readNBytes(1024);
                if(bytes_read.length == 0){
                    break;
                }

                //send the database file chunk we just read
                ServerModel.sendMessageViaChannel(
                        new Message(Message.TYPE_OF_MESSAGE.TRANSFER_DATABASE, bytes_read),
                        _to_send_socket_channel
                );
            }

            //close the database file
            database_file.close();
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        finally {
            try {
                _to_send_socket_channel.close();
            } catch (IOException e) {
                System.out.println(e);
                e.printStackTrace();
            }
            _server_model.getEventDatabase().openDatabaseConnection();
            _server_model.unlockDatabase();
            _server_model.lostTcpConnection();
            System.out.println("Transfer Done");
        }
    }
}
