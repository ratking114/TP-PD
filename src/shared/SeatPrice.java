package shared;

public  class SeatPrice {
    public char row;
    public int seat;
    public double price;

    public SeatPrice(char row, int seat, double price) {
        row = row;
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


}
