package shared;

import java.io.Serializable;
import java.util.List;

public class EventInfo implements Serializable {
    public int id;
    public String designation;
    public String type;
    public String date;
    public int duration;
    public String location;
    public String county;
    public String country;
    public String age_Restriction;
    public List<SeatPrice> price_by_seat;

    public EventInfo(){}

    public EventInfo(int id, String designation, String type, String date, int duration, String location, String county, String country, String age_Restriction, List<SeatPrice> price_by_seat) {
        this.id = id;
        this.designation = designation;
        this.type = type;
        this.date = date;
        this.duration = duration;
        this.location = location;
        this.county = county;
        this.country = country;
        this.age_Restriction = age_Restriction;
        this.price_by_seat = price_by_seat;
    }

    @Override
    public String toString() {
        return String.format("Designação:%s\nTipo:%s\nData/Hora:%s\nDuração:%d\nLocal:%s\nLocalidade:%s\nPais:%s\nClassificação etária:%s",
                designation,type,date, duration,location, county, country,age_Restriction);
    }
}