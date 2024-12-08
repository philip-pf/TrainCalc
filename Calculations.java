
public class Calculations {

    public static double calcTimeConstant(double length, double speed) {
        return length / speed;
    }

    public static double calcTimeAcceleration(double targetSpeed, Train train) {

        return (targetSpeed - train.getActualSpeed()) / train.getAcceleration(); //Beschleunigungszeit ausrechnen und zurückgeben

    }

    public static double calcDistanceAcceleration(double targetSpeed, Train train) {

        double accTime = calcTimeAcceleration(targetSpeed, train);

        return 0.5 * train.getAcceleration() * Math.pow(accTime, 2) + train.getActualSpeed() * accTime;

    }

    public static double calcTimeBlockAccelerating(Track track, Train train, Track previousSection) {

        double maximumSpeed = Math.min(track.getSpeed(), train.getMaxV()); //Geschwindigkeit auf die Beschleunigt werden soll

        //System.out.println("Beschleunigungsstrecke: " + (calcDistanceAcceleration(maximumSpeed, train) + train.getTailLength()));

        //Wenn der Zug an einem Bahnsteig gehalten hat, darf er direkt auf Streckengeschwindigkeit des nächsten Abschnittes
        //beschleunigen - hier muss eine Bedingung hinzugefügt werden, wenn vor dem Bahnsteig eine Geschwindigkeitseinschränkung ist.
        if (previousSection.getElement().equals("platform")) {
            train.reduceTailLength(train.getTailLength());
        }

        if (previousSection.getSpeed() != track.getSpeed()) {
            //System.out.print("Erster Beschleunigungsabschnitt - ");

            if (calcDistanceAcceleration(maximumSpeed, train) + train.getTailLength() <= track.getLength()) {
                //Wenn der Abschnitt lang genug ist, um in ihm vollständig zu beschleunigen
                //System.out.println("lang genug");

                double constantSpeedTimeLow = calcTimeConstant(train.getTailLength(), train.getActualSpeed()); //damit der Zug noch mit dem Zugschluss den langsamen Bereich verlässt
                double accelerationTime = calcTimeAcceleration(maximumSpeed, train);
                double constantSpeedTimeHigh = calcTimeConstant(track.getLength() - train.getTailLength() - calcDistanceAcceleration(maximumSpeed, train), maximumSpeed);

                train.setActualSpeed(maximumSpeed);

                //System.out.println("noch langsam: " + constantSpeedTimeLow);
                //System.out.println("beschleunigungszeit: " + accelerationTime);
                //System.out.println("schon schnell :" + constantSpeedTimeHigh);

                return constantSpeedTimeLow + accelerationTime + constantSpeedTimeHigh;

            } else {
                //wenn der Abschnitt zu kurz ist, um vollständig zu beschleunigen
                //System.out.println("nicht lang genug");


                //Prüfen, ob in diesem Abschnitt schon beschleunigt werden darf
                if (train.getTailLength() > track.getLength()) {

                    train.reduceTailLength(track.getLength());
                    return calcTimeConstant(track.getLength(), train.getActualSpeed());

                }

                //System.out.println("Geschwindigkeit am Abschnittsende: " + (Math.sqrt(Math.pow(train.getActualSpeed(), 2) + 2 * train.getAcceleration() * (track.getLength() - train.getTailLength()))) * 3.6);

                double speedAtEndOfSection = Math.sqrt(Math.pow(train.getActualSpeed(), 2) + 2 * train.getAcceleration() * (track.getLength() - train.getTailLength()));
                double constantTimeLow = calcTimeConstant(train.getTailLength(), train.getActualSpeed());
                double accelerationTime = calcTimeAcceleration(speedAtEndOfSection, train);

                train.setActualSpeed(speedAtEndOfSection);
                train.reduceTailLength(train.getTailLength());

                return constantTimeLow + accelerationTime;

            }

        } else {
            //System.out.print("Zweiter Beschleunigungsabschnitt - ");

            if (calcDistanceAcceleration(maximumSpeed, train) + train.getTailLength() <= track.getLength()) {
                //Wenn der Abschnitt lang genug ist um in ihm vollständig zu beschleunigen
                //System.out.println("lang genug");

                double constantSpeedTimeLow = calcTimeConstant(train.getTailLength(), train.getActualSpeed());
                double accelerationTime = calcTimeAcceleration(maximumSpeed, train);
                double constantSpeedTimeHigh = calcTimeConstant(track.getLength() - train.getTailLength() - calcDistanceAcceleration(maximumSpeed, train), maximumSpeed);

                train.setActualSpeed(maximumSpeed);
                train.resetTailLength();    //tailLength wieder auf den Anfangswert setzen, damit die nächste
                                            // Beschleunigung wieder mit dem richtigen Wert rechnet

                //System.out.println("beschleunigungszeit: " + accelerationTime);
                //System.out.println("schon schnell :" + constantSpeedTimeHigh);

                return constantSpeedTimeLow + accelerationTime + constantSpeedTimeHigh;

            } else {
                //wenn der Abschnitt zu kurz ist um vollständig zu beschleunigen
                //System.out.println("nicht lang genug");

                //System.out.println("Geschwindigkeit am Abschnittsende: " + (Math.sqrt(Math.pow(train.getActualSpeed(), 2) + 2 * train.getAcceleration() * track.getLength())) * 3.6 + "km/h");

                //Prüfen, ob in diesem Abschnitt schon beschleunigt werden darf
                if (train.getTailLength() > track.getLength()) {

                    train.reduceTailLength(track.getLength());
                    return calcTimeConstant(track.getLength(), train.getActualSpeed());

                }

                double speedAtEndOfSection = Math.sqrt(Math.pow(train.getActualSpeed(), 2) + 2 * train.getAcceleration() * (track.getLength() - train.getTailLength()));
                double constantTimeLow = calcTimeConstant(train.getTailLength(), train.getActualSpeed());
                double accelerationTime = calcTimeAcceleration(speedAtEndOfSection, train);

                train.setActualSpeed(speedAtEndOfSection);
                train.reduceTailLength(train.getTailLength());

                return constantTimeLow + accelerationTime;

            }
/*
            double speedAtEndOfSection = Math.sqrt(Math.pow(train.getActualSpeed(), 2) + 2 * train.getAcceleration() * track.getLength());
            double accelerationTime = calcTimeAcceleration(speedAtEndOfSection, train);

            train.setActualSpeed(speedAtEndOfSection);

            return accelerationTime;
*/
        }
    }

    public static double calcTimeBreaking(double targetSpeed, Train train) {

        return (-targetSpeed + train.getActualSpeed()) / train.getBreaks(); //Bremszeit ausrechnen und zurückgeben

    }

    public static double calcDistanceBreaking(double targetSpeed, Train train) {

        double breakTime = calcTimeBreaking(targetSpeed, train);

        return -0.5 * train.getBreaks() * Math.pow(breakTime, 2) + train.getActualSpeed() * breakTime;

    }

    public static double calcTimeBlockBreaking(double blockLength, double targetSpeed, Train train) {

        //Zuerst prüfen ob in dem Abschnitt auf die gewünschte Geschwindigkeit gebremst werden kann
        if (blockLength > calcDistanceBreaking(targetSpeed, train)) {
            //Wenn der Abschnitt lang genug ist, die Bremszeit und die langsamfahrzeit addieren und zurückgeben.

            double breakingTime = calcTimeBreaking(targetSpeed, train);
            double constantSpeedTimeLow = calcTimeConstant(blockLength - calcDistanceBreaking(targetSpeed, train), targetSpeed);

            train.setActualSpeed(targetSpeed);
            return breakingTime + constantSpeedTimeLow;
        }
        //Wenn der Abschnitt nicht lang genug ist die Geschwindigkeit am Abschnittsende bestimmen.
        //die Zeit berechnen, die dazu benötigt wird und dem Zug die Geschwindigkeit mitgeben

        double sectionEndSpeed = Math.sqrt(Math.pow(train.getActualSpeed(),2)-(2*train.getBreaks()*blockLength));
        double breakTime = calcTimeBreaking(sectionEndSpeed, train);

        train.setActualSpeed(sectionEndSpeed);

        return breakTime;
}

}