package ServerClasses;

public  class SeatPrice {
    private char Row;
    private int seat;
    private double price;

    public SeatPrice(char row, int seat, double price) {
        Row = row;
        this.seat = seat;
        this.price = price;
    }

    public char getRow() {
        return Row;
    }

    public int getSeat() {
        return seat;
    }

    public double getPrice() {
        return price;
    }

    public void setRow(char row) {
        Row = row;
    }

    public void setSeat(int seat) {
        this.seat = seat;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }

        if(obj.getClass() != SeatPrice.class)
            return false;



        return Row == ((SeatPrice) obj).getRow() && seat == ((SeatPrice) obj).getSeat();
    }

    @Override
    public int hashCode() {
        return Row*3 + seat*7;
    }


}
