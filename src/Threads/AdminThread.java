package Threads;

import ServerClasses.EventInfo;
import ServerClasses.SeatPrice;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class AdminThread extends Thread{



    @Override
    public void run() {
        Scanner stdin = new Scanner(System.in);
        String path = stdin.nextLine();

        try {
            addEvent(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
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
        eventInfo.setDesignation(line.next().replaceAll("\"",""));

        //2ª linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(":");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Tipo"))
            return false;
        if(!line.hasNext())
            return false;
        eventInfo.setType(line.next().replaceAll("\"",""));

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



        eventInfo.setDate(String.format("%d-%d-%d %d:%d:0",year,month,day,hour,minutes));
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


        eventInfo.setDuration(duration);


        //6 linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Local"))
            return false;


        if(!line.hasNext())
            return false;
        eventInfo.setLocation(line.next().replaceAll("\"", ""));



        //7 linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Localidade"))
            return false;


        if(!line.hasNext())
            return false;
        eventInfo.setCounty(line.next().replaceAll("\"", ""));

        //8 linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("País"))
            return false;


        if(!line.hasNext())
            return false;
        eventInfo.setCountry(line.next().replaceAll("\"", ""));

        //9 linha
        if(!scanner.hasNextLine())
            return false;
        line=new Scanner(scanner.nextLine()).useDelimiter(";");
        bookmark= line.next();
        if(!bookmark.replaceAll("\"","").equals("Classificação etária"))
            return false;


        if(!line.hasNext())
            return false;
        eventInfo.setAgeRestriction(line.next().replaceAll("\"", ""));


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
                SeatPrice seatPrice= new SeatPrice(row.charAt(0),seat,price);
                if(seatInfo.contains(seatPrice))
                    return false;
                seatInfo.add(seatPrice);

            }
        }

        System.out.println(eventInfo);
        return true;

    }

}
