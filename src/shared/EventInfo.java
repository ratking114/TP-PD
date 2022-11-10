package shared;

import java.util.ArrayList;

public class EventInfo {
    public int id;
    public String designation;
    public String type;
    public String date;
    public int duration;
    public String location;
    public String county;
    public String country;
    public String age_Restriction;
    public ArrayList<SeatPrice> price_by_seat;

    @Override
    public String toString() {
        return String.format("Designação:%s\nTipo:%s\nData/Hora:%s\nDuração:%d\nLocal:%s\nLocalidade:%s\nPais:%s\nClassificação etária:%s",
               designation,type,date, duration,location, county, country,age_Restriction);
    }
}
