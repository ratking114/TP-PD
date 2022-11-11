package server.Threads;

import shared.EventInfo;
import server.ServerClasses.Prepare;
import shared.SeatPrice;
import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class AdminThread extends Thread{
    private final ServerModel _server_model;



    public AdminThread(ServerModel server_model){
        _server_model = server_model;
    }


    @Override
    public void run() {
        Scanner stdin = new Scanner(System.in);//copium
        //String path = stdin.nextLine();

        while(true){
            //print a prompt
            System.out.print("Admin> ");

            //read a command from the keyboard
            String command = stdin.nextLine();

            //send a Prepare
            if(command.equals("prepare")){
                //place a request
                Message.TYPE_OF_MESSAGE answer = _server_model.getProducerConsumerBuffer().placeRequestAndWaitForAnswer(
                        new Prepare("select * from shit;", _server_model.generateNumberFromIPAndPort())
                );


                //print the answer
                System.out.println("We got: " + answer);
            }
        }


    }
    private boolean addEvent(String path) throws FileNotFoundException {
        File newEvent= new File(path);
        Scanner scanner= new Scanner(newEvent).useDelimiter("\r\n");
        EventInfo eventInfo= new EventInfo();
        //1ª Linha
        Scanner line=new Scanner(scanner.nextLine()).useDelimiter(";");
        String bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Designação"))
            return false;
        if(!line.hasNext())
            return false;
        eventInfo.designation = line.next().replaceAll("\"","");

        //2ª linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(":");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Tipo"))
            return false;
        if(!line.hasNext())
            return false;
        eventInfo.type = line.next().replaceAll("\"","");

        //3ª linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Data"))
            return false;
        if(!line.hasNext())
            return false;

        String day_String= line.next();
        int day=0;
        try
        {
             day = Integer.parseInt(day_String.replaceAll("\"", ""));
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }

        if(!line.hasNext())
            return false;
        String month_String= line.next();
        int month=0;
        try
        {
             month = Integer.parseInt(month_String.replaceAll("\"", ""));
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }

        if(!line.hasNext())
            return false;

        String year_String= line.next();
        int year=0;
        try
        {
            year = Integer.parseInt(year_String.replaceAll("\"", ""));
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }

        //4ª linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Hora"))
            return false;

        if(!line.hasNext())
            return false;

        String hour_String= line.next();
        int hour=0;
        try
        {
            hour = Integer.parseInt(hour_String.replaceAll("\"", ""));
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }


        if(!line.hasNext())
            return false;

        String minutes_String= line.next();
        int minutes=0;
        try
        {
            minutes = Integer.parseInt(minutes_String.replaceAll("\"", ""));
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }



        eventInfo.date =  String.format("%d-%d-%d %d:%d:0",year,month,day,hour,minutes);
        //5º Linha

        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Duração"))
            return false;
        if(!line.hasNext())
            return false;

        String duration_string=line.next();
        int duration=0;
        try
        {
            duration = Integer.parseInt(duration_string.replaceAll("\"", ""));
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }


        eventInfo.duration = duration;


        //6 linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Local"))
            return false;


        if(!line.hasNext())
            return false;
        eventInfo.location = line.next().replaceAll("\"", "");



        //7 linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Localidade"))
            return false;


        if(!line.hasNext())
            return false;
        eventInfo.country = line.next().replaceAll("\"", "");

        //8 linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("País"))
            return false;


        if(!line.hasNext())
            return false;
        eventInfo.country = line.next().replaceAll("\"", "");

        //9 linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Classificação etária"))
            return false;


        if(!line.hasNext())
            return false;
        eventInfo.age_Restriction = line.next().replaceAll("\"", "");


        ArrayList<SeatPrice>seatInfo= new ArrayList<>();
        if(!scanner.hasNextLine())
            return false;
        if(!scanner.nextLine().equals(""))
            return false;
        bookmark=scanner.nextLine();

        if(!bookmark.equals("\"Fila\";\"Lugar:Preço\""))
            return false;

        while(scanner.hasNextLine())
        {
            line= new Scanner(scanner.nextLine()).useDelimiter(";");
            String row= line.next().replaceAll("\"","");
            if(row.length()!=1)
                return false;
            while(line.hasNext()) {
                Scanner yetanotherscanner = new Scanner(line.next().replaceAll("\"","").trim());
                yetanotherscanner.useDelimiter(":");
                if(!yetanotherscanner.hasNextInt())
                    return false;
                int seat=yetanotherscanner.nextInt();
                if(!yetanotherscanner.hasNextInt())
                    return false;
                int price=yetanotherscanner.nextInt();
                if(yetanotherscanner.hasNext())
                    return false;
                SeatPrice seatPrice= new SeatPrice(row.charAt(0), seat, price);
                if(seatInfo.contains(seatPrice))
                    return false;
                seatInfo.add(seatPrice);

            }
        }

        System.out.println(eventInfo);
        return true;

    }

}
