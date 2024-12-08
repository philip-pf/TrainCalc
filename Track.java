public class Track {
    private double length;
    private String element;
    private double speed = 0;
    private double buildTime = 0;
    private double resetTime = 0;


    public Track(double length, double speed, String element) {
        this.length = length;
        this.element = element;
        this.speed = speed / 3.6;
    }

    public Track(double length, double speed, String element, double buildTime, double resetTime) {
        this.length = length;
        this.element = element;
        this.speed = speed / 3.6;
        this.buildTime = buildTime;
        this.resetTime = resetTime;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getResetTime() {
        return resetTime;
    }

    public double getBuildTime() {
        return buildTime;
    }

    @Override
    public String toString() {
        return "Element: " + this.element + " Länge: " + this.length + " Speed: " + this.speed * 3.6;
    }

}