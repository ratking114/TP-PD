package shared;

public class EventFilter {
    public String designation;
    public String type;
    public String date;
    public Integer duration;
    public String location;
    public String county;
    public String country;
    public String age_Restriction;

    public EventFilter(String designation, String type, String date, Integer duration, String location, String county, String country, String age_Restriction) {
        this.designation = designation;
        this.type = type;
        this.date = date;
        this.duration = duration;
        this.location = location;
        this.county = county;
        this.country = country;
        this.age_Restriction = age_Restriction;
    }
}
