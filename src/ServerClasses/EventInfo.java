package ServerClasses;

import java.util.ArrayList;

public class EventInfo {
    private String designation;

    private String type;
    private String date;
    private int duration;
    private String location;
    private String county;
    private String country;
    private String age_Restriction;
    private ArrayList<SeatPrice> price_by_seat;

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAgeRestriction() {
        return age_Restriction;
    }

    public void setAgeRestriction(String age_Restriction) {
        this.age_Restriction = age_Restriction;
    }

    public ArrayList<SeatPrice> getPrice_by_seat() {
        return price_by_seat;
    }

    public void setPriceBySeat(ArrayList<SeatPrice> price_by_seat) {
        this.price_by_seat = price_by_seat;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("Designação:%s\nTipo:%s\nData/Hora:%s\nDuração:%d\nLocal:%s\nLocalidade:%s\nPais:%s\nClassificação etária:%s",
               designation,type,date, duration,location, county, country,age_Restriction);
    }
}
