public class Train {

    private double maxV;
    private double acceleration;
    private double breaks;
    private double length;
    private double rang;
    private boolean passenger;
    private double probabilityDelay;
    private double averageDelay;
    private String name;
    private double actualSpeed;
    private double numberOf;
    private double tailLength;
    private boolean stop = false;
    private double stopTime;

    public Train(double maxV, double acceleration, double breaks, double length, double numberOf, double rang, boolean passenger, double probabilityDelay, double averageDelay, double stopTime, String name){
        this.maxV = maxV / 3.6;
        this.acceleration = acceleration;
        this.breaks = breaks;
        this.length = length;
        this.numberOf = numberOf;
        this.rang = rang;
        this.passenger = passenger;
        this.probabilityDelay = probabilityDelay;
        this.averageDelay = averageDelay * 60;
        this.name = name;
        this.tailLength = this.length;
        this.stopTime = stopTime;

        if (this.stopTime > 0)
            this.stop = true;
    }

    public double getMaxV() {
        return maxV;
    }

    public void setMaxV(double maxV) {
        this.maxV = maxV;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public double getBreaks() {
        return breaks;
    }

    public double getLength() {
        return this.length;
    }

    public String getName() {
        return name;
    }

    public double getRang() {
        return rang;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getActualSpeed() {
        return actualSpeed;
    }

    public void setActualSpeed(double actualSpeed) {
        this.actualSpeed = actualSpeed;
    }

    public double getNumberOf() {
        return numberOf;
    }

    public double getTailLength() {
        return tailLength;
    }

    public boolean isPassenger() {
        return passenger;
    }

    public double getAverageDelay() {
        return averageDelay;
    }

    public double getProbabilityDelay() {
        return probabilityDelay;
    }

    public void reduceTailLength(double trackLength){
        this.tailLength = this.tailLength - trackLength;
    }

    public void resetTailLength(){
        this.tailLength = this.length;
    }

    public boolean isStop() {
        return this.stop;
    }

    public void setStop() {
        this.stop = true;
    }

    public double getStopTime() {
        return stopTime;
    }

    public String toString(){
        return this.name + " - " + this.maxV + " - " + this.acceleration + " - " + this.stop;
    }

}