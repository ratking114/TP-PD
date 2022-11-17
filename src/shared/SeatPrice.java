package shared;

import java.io.Serializable;

public  class SeatPrice implements Serializable {
    public int id;
    public char row;
    public int seat;
    public double price;
    public int event_id;

    public SeatPrice(int id, char row, int seat, double price, int event_id) {
        this.id = id;
        this.row = row;
        this.seat = seat;
        this.price = price;
        this.event_id = event_id;
    }

    public SeatPrice(char row, int seat, double price){
        this.row = row;
        this.seat = seat;
        this.price = price;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }

        if(obj.getClass() != SeatPrice.class)
            return false;
        return row == ((SeatPrice) obj).row && seat == ((SeatPrice) obj).seat;
    }

    @Override
    public int hashCode() {
        return row*3 + seat*7;
    }

    @Override
    public String toString() {
        return String.format("%c%d",row,seat);
    }
}