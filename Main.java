import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {

        //Datum und Uhrzeit für die benennung der logdatei
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String date = myFormatObj.format(time);

        //logdatei erzeugen
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("log/" + date + ".txt", StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("logdatei konnte nicht erstellt werden");
            System.out.println(e.getMessage());
            System.exit(1);
        }

        double lineSpeed = readLineSpeed();
        double timeSpan = readTime();
        timeSpan *= 60;

        List<Train> trains = new ArrayList<>(readTrains());

        List<Track> tracks = new ArrayList<>(readTracks());

        //alle Blöcke die keine Geschwindigkeit haben bekommen Streckengeschwindigkeit
        for (Track track : tracks)
            if (track.getSpeed() == 0) track.setSpeed(lineSpeed / 3.6);

        //Bremsweg prüfen
        for (Train train : trains)
            checkBreaks(train);

        //Anzahl der Blöcke feststellen
        int blockAmount = -1;
        for (Track track : tracks)
            if (track.getElement().equals("mainSignal")) blockAmount++;

        writer.println("Sperrzeitenanteile");
        //Fahrzeiten ausgeben
        writer.print("Zug                 ");
        for (int j = 0; j < blockAmount; j++)
            writer.print((j + 1) + ". Abschnitt    ");
        writer.println();

        for (Train value : trains) {
            writer.print(value.getName());
            for (int k = 0; k < 20 - value.getName().length(); k++)
                writer.print(" ");

            for (int j = 0; j < blockAmount; j++) {
                writer.print(Math.ceil(RunTime.makeRunList(tracks, value).get(j).get(0)));
                for (int k = 0; k < 16 - String.valueOf(Math.ceil(RunTime.makeRunList(tracks, value).get(j).get(0))).length(); k++) {
                    writer.print(" ");
                }
            }
            writer.println();

            writer.print("                    ");
            for (int j = 0; j < blockAmount; j++) {
                writer.print(Math.ceil(RunTime.makeRunList(tracks, value).get(j).get(1)));
                for (int k = 0; k < 16 - String.valueOf(Math.ceil(RunTime.makeRunList(tracks, value).get(j).get(1))).length(); k++)
                    writer.print(" ");
            }
            writer.println();
            writer.print("                    ");
            for (int j = 0; j < blockAmount; j++) {
                writer.print(Math.ceil(RunTime.makeRunList(tracks, value).get(j).get(2)));
                for (int k = 0; k < 16 - String.valueOf(Math.ceil(RunTime.makeRunList(tracks, value).get(j).get(2))).length(); k++)
                    writer.print(" ");
            }
            writer.println();
        }
        writer.println();

        //Mindeszugfolgematrix
        double[][] minimumHeadwayTable = new double[trains.size()][trains.size()];


        writer.println("Mindestzugfolgezeiten:");
        for (int i = 0; i < minimumHeadwayTable.length; i++) {
            for (int j = 0; j < minimumHeadwayTable.length; j++) {
                minimumHeadwayTable[i][j] = findMinimumHeadway(tracks, trains.get(i), trains.get(j));
                writer.print(Math.ceil(minimumHeadwayTable[i][j]) + "\t");
            }
            writer.println();
        }
        writer.println();

        //Gesamtzahl der Züge
        double numberOfTrains = 0;
        for (Train train : trains)
            numberOfTrains += train.getNumberOf();

        //mittlere Mindestzugfolgezeit, dazu die Mindestzugfolgezeitmatrix mit den Zugfahrten multiplizieren
        double[][] averageMinimumHeadwayTable = new double[trains.size()][trains.size()];

        writer.println("Mindestzugfolgezeitmatrix * Zugzahlen");
        for (int i = 0; i < averageMinimumHeadwayTable.length; i++) {
            for (int j = 0; j < averageMinimumHeadwayTable.length; j++) {
                averageMinimumHeadwayTable[i][j] = minimumHeadwayTable[i][j] * (trains.get(i).getNumberOf() * trains.get(j).getNumberOf());
                writer.print(Math.ceil(averageMinimumHeadwayTable[i][j]) + "\t\t");
            }
            writer.println();
        }
        writer.println();

        //Matrix des Gleichrangs, bei Gleichrang ist der Eintrag 1 bei Ungleichrang 0
        double[][] sameRangTable = new double[trains.size()][trains.size()];

        writer.println("Gleichrangmatrix");
        for (int i = 0; i < sameRangTable.length; i++) {
            for (int j = 0; j < sameRangTable.length; j++) {
                if (trains.get(i).getRang() == trains.get(j).getRang()) sameRangTable[i][j] = 1;
                else sameRangTable[i][j] = 0;
                writer.print(sameRangTable[i][j] + "\t");
            }
            writer.println();
        }
        writer.println();

        //Gesamtzahl der Zugfolgefälle
        double totalTrains = 0;

        for (int i = 0; i < trains.size(); i++) {
            for (Train train : trains) {
                totalTrains += trains.get(i).getNumberOf() * train.getNumberOf();
            }
        }

        //Gesamtzahl der gleichen Zugfolgefälle
        double totalTrainsSame = 0;

        for (int i = 0; i < trains.size(); i++) {
            for (Train train : trains) {
                if (trains.get(i).getRang() == train.getRang())
                    totalTrainsSame += trains.get(i).getNumberOf() * train.getNumberOf();
            }
        }

        //Summe von averageMinimumHeadwaytable
        double sumAverageHeadway = 0;

        for (int i = 0; i < trains.size(); i++) {
            for (int j = 0; j < trains.size(); j++) {
                sumAverageHeadway += averageMinimumHeadwayTable[i][j];
            }
        }

        //Summe von averageMinimumHeadwaytable mit der Gleichrangmatrix
        double sumAverageHeadwaySame = 0;

        for (int i = 0; i < trains.size(); i++) {
            for (int j = 0; j < trains.size(); j++) {
                sumAverageHeadwaySame += (averageMinimumHeadwayTable[i][j] * sameRangTable[i][j]);
            }
        }

        //Die Gleichrangmatrix in die Ungleichrangmatrix umbauen
        for (int i = 0; i < sameRangTable.length; i++) {
            for (int j = 0; j < sameRangTable.length; j++) {
                if (sameRangTable[i][j] == 0) sameRangTable[i][j] = 1;
                else sameRangTable[i][j] = 0;
            }
        }

        //Summe von averageMinimumHeadwaytable mit der Ungleichrangmatrix
        double sumAverageHeadwayDiffer = 0;

        for (int i = 0; i < trains.size(); i++) {
            for (int j = 0; j < trains.size(); j++) {
                sumAverageHeadwayDiffer += (averageMinimumHeadwayTable[i][j] * sameRangTable[i][j]);
            }
        }

        //Durchschnittliche Mindestzugfolgezeit
        double averageHeadway = sumAverageHeadway / totalTrains;
        writer.println("mittlere Zugfolgezeit: " + averageHeadway + "s");

        //Durchschnittliche Mindestzugfolgezeit Gleichrang
        double averageHeadwaySame = sumAverageHeadwaySame / totalTrainsSame;
        //averageHeadwaySame=3.6;
        writer.println("mittlere Zugfolgezeit Gleichrang: " + averageHeadwaySame + "s");

        //Durchschnittliche Mindestzugfolgezeit Ungleichrang
        double averageHeadwayDiffer = sumAverageHeadwayDiffer / (totalTrains - totalTrainsSame);
        //averageHeadwayDiffer=3.69;
        writer.println("mittlere Zugfolgezeit Ungleichrang: " + averageHeadwayDiffer + "s");
        writer.println();

        //Belegungszeit
        double blockedTime = averageHeadway * numberOfTrains;
        writer.println("Belegungszeit: " + blockedTime + "s");

        //Verketteter Belegungsgrad
        double blockedRate = blockedTime / timeSpan;
        writer.println("Verketteter Belegungsgrad: " + blockedRate);

        //mittlere vorhandene Pufferzeit
        double averageRealBuffer = (timeSpan - blockedTime) / numberOfTrains;
        writer.println("Mittlere vorhandene Pufferzeit: " + averageRealBuffer + "s");
        writer.println();

        //Wahrscheinlichkeit des Gleichrangs
        double probabilitySameRang = totalTrainsSame / totalTrains;
        writer.println("Wahrscheinlichkeit der gleichrangigen Zugfolge: " + probabilitySameRang);

        //Wahrscheinlichkeit für das Auftreten einer Einbruchsverspätung
        double numberOfDelyaedTrains = 0;
        for (Train train : trains)
            numberOfDelyaedTrains += train.getNumberOf() * train.getProbabilityDelay();
        double probabilityOfDelay = numberOfDelyaedTrains / numberOfTrains;
        writer.println("Wahrscheinlichkeit einer Einbruchsverspätung: " + probabilityOfDelay);

        //durchschnittliche Einbruchsverspätung ^-1
        double delay = 0;
        for (Train train : trains)
            delay += train.getAverageDelay() * train.getNumberOf() * train.getProbabilityDelay();
        double averageDelay = numberOfDelyaedTrains / delay;
        writer.println("Kehrwert der Durchschnittlichen Einbruchsverspätungen: " + averageDelay);

        //maximale Folgeverspätungssumme
        writer.println();
        double passengerProbotion = 0;
        for (Train train : trains)
            if (train.isPassenger()) passengerProbotion += train.getNumberOf();
        passengerProbotion = passengerProbotion / numberOfTrains;
        double qualityNieau = 1 * 0.26 * Math.exp(-1.3 * passengerProbotion) * timeSpan;
        writer.println("Zulässiger Wert der Folgeverspätungen optimal: " + qualityNieau + "s");
        double qualityNieauHigh = 0.5 * 0.26 * Math.exp(-1.3 * passengerProbotion) * timeSpan;
        writer.println("Zulässiger Wert der Folgeverspätungen Premiumqualität: " + qualityNieauHigh + "s");
        double qualityNieauLow = 1.2 * 0.26 * Math.exp(-1.3 * passengerProbotion) * timeSpan;
        writer.println("Zulässiger Wert der Folgeverspätungen Riskobehaftet: " + qualityNieauLow + "s");
        writer.println();

        //Pufferzeitquotient

        /*
        Iteration
        Es wird von einem Startwert zunächst 0,1 aufaddiert bis der zulässige Wert für die Folgeverspätungssumme überschritten wird.
        Ist der zulässige Wert überschritten, wird ein Schritt zurück durchgeführt und der Wert für die Erhöhung des Wertes des
        Pufferzeitquotienten um den Faktor 10 verringert und wieder addiert bis der Zielwert überschritten wird.
        Hat der Multiplikationsfaktor eine gewisse Länge erreicht, wird die Iteration abgebrochen, um Rechenzeit zu sparen.
         */
        double bufferQuotient = 0.1;
        double subsequentDelay = calcSubsequentDelay(timeSpan, probabilityOfDelay, probabilitySameRang, averageDelay, averageHeadway, averageHeadwaySame, averageHeadwayDiffer, bufferQuotient);

        double multiplier = 0.1;
        while (subsequentDelay != qualityNieau) {
            if (subsequentDelay > qualityNieau) bufferQuotient += multiplier;
            else {
                bufferQuotient -= multiplier;
                multiplier /= 10;
                if (multiplier < 0.000000001) break;
            }
            subsequentDelay = calcSubsequentDelay(timeSpan, probabilityOfDelay, probabilitySameRang, averageDelay, averageHeadway, averageHeadwaySame, averageHeadwayDiffer, bufferQuotient);
        }
        writer.println("Pufferzeitquotient optimal: " + bufferQuotient);

        multiplier = 0.1;
        double bufferQuotientHigh = 0.1;
        subsequentDelay = calcSubsequentDelay(timeSpan, probabilityOfDelay, probabilitySameRang, averageDelay, averageHeadway, averageHeadwaySame, averageHeadwayDiffer, bufferQuotientHigh);

        while (subsequentDelay != qualityNieauHigh) {
            if (subsequentDelay > qualityNieauHigh) bufferQuotientHigh += multiplier;

            else {
                bufferQuotientHigh -= multiplier;
                multiplier /= 10;
                if (multiplier < 0.000000001) break;
            }
            subsequentDelay = calcSubsequentDelay(timeSpan, probabilityOfDelay, probabilitySameRang, averageDelay, averageHeadway, averageHeadwaySame, averageHeadwayDiffer, bufferQuotientHigh);
        }
        writer.println("Pufferzeitquotient Premiumqualität: " + bufferQuotientHigh);

        multiplier = 0.1;
        double bufferQuotientLow = 0.1;
        subsequentDelay = calcSubsequentDelay(timeSpan, probabilityOfDelay, probabilitySameRang, averageDelay, averageHeadway, averageHeadwaySame, averageHeadwayDiffer, bufferQuotientLow);

        while (subsequentDelay != qualityNieauLow) {
            if (subsequentDelay > qualityNieauLow) bufferQuotientLow += multiplier;
            else {
                bufferQuotientLow -= multiplier;
                multiplier /= 10;
                if (multiplier < 0.000000001) break;
            }
            subsequentDelay = calcSubsequentDelay(timeSpan, probabilityOfDelay, probabilitySameRang, averageDelay, averageHeadway, averageHeadwaySame, averageHeadwayDiffer, bufferQuotientLow);
        }
        writer.println("Pufferzeitquotient Risikobehaftet: " + bufferQuotientLow);

        //mittlere erforderliche Pufferzeit
        writer.println();
        double neededBuffer = averageHeadway * bufferQuotient;
        writer.println("mittlere erforderliche Pufferzeit Optimale Qualität: " + neededBuffer + "s");
        double neededBufferHigh = averageHeadway * bufferQuotientHigh;
        writer.println("mittlere erforderliche Pufferzeit Premiumqualität: " + neededBufferHigh + "s");
        double neededBufferLow = averageHeadway * bufferQuotientLow;
        writer.println("mittlere erforderliche Pufferzeit Risikobehaftete Qualität: " + neededBufferLow + "s");

        //Leistungsfähigkeit
        writer.println();
        double possibleTrains = timeSpan / (averageHeadway + neededBuffer);
        writer.println("Leistungsfähigkeit optimal: " + possibleTrains + " Züge");
        double possibleTrainsHigh = timeSpan / (averageHeadway + neededBufferHigh);
        writer.println("Leistungsfähigkeit Premiumqualität: " + possibleTrainsHigh + " Züge");
        double possibleTrainsLow = timeSpan / (averageHeadway + neededBufferLow);
        writer.println("Leistungsfähigkeit Risikobehaftet: " + possibleTrainsLow + " Züge");

        //Bestimmung, ob das Betriebsprogramm fahrbar ist
        writer.println();
        if (possibleTrains >= numberOfTrains && averageRealBuffer >= neededBuffer)
            writer.println("Das Betriebsprogramm ist fahrbar");
        else if (possibleTrains >= numberOfTrains && averageRealBuffer < neededBuffer)
            writer.println("Das Betriebsprogramm ist nicht fahrbar - Pufferzeit nicht ausreichend");
        else if (possibleTrains < numberOfTrains && averageRealBuffer > neededBuffer)
            writer.println("Das Betriebsprogramm ist nicht fahrbar - Leistungsfähigkeit nicht ausreichend");
        else
            writer.println("Das Betriebsprogramm ist nicht fahrbar - Leistungsfähigkeit und Pufferzeit nicht ausreichend");


        writer.close();

    }

    private static double findMinimumHeadway(List<Track> tracks, Train train0, Train train1) {

        List<Double> headwayOverlap = new ArrayList<>();

        for (int i = 0; i < RunTime.makeRunList(tracks, train0).size(); i++)
            headwayOverlap.add(RunTime.makeRunList(tracks, train0).get(i).get(2) - RunTime.makeRunList(tracks, train1).get(i).get(0));

        double maximum = 0.0;

        for (Double overlap : headwayOverlap)
            if (overlap > maximum) maximum = overlap;

        return maximum;
    }

    private static double calcSubsequentDelay(double timeSpan, double probabilityOfDelay, double probabilitySameRang, double averageDelay, double averageHeadway, double averageHeadwaySame, double averageHeadwayDiffer, double bufferQuotient) {

        return timeSpan * (probabilityOfDelay - (Math.pow(probabilityOfDelay, 2) / 2)) * (
                ((probabilitySameRang * Math.pow(1 - Math.exp(-averageDelay * averageHeadwaySame), 2)) + ((1 - probabilitySameRang) * averageDelay * averageHeadwayDiffer * (1 - Math.exp(-2 * averageDelay * averageHeadwayDiffer))) + (1 / bufferQuotient * Math.pow(1 - Math.exp(-averageDelay * averageHeadway), 2))) /
                        (Math.pow(averageDelay * averageHeadway, 2) * (1 + bufferQuotient) * (bufferQuotient + (1 - Math.exp(-averageDelay * averageHeadway)) / (averageDelay * averageHeadway))));
    }

    private static void checkBreaks(Train train) {

        train.setActualSpeed(train.getMaxV());
        if (Calculations.calcDistanceBreaking(0, train) > 1000){
            double newSpeed = 2000 / Calculations.calcTimeBreaking(0, train);
            System.out.println("Bei Zug " + train.getName() + " Bremsweg zu Lang neue Höchstgeschwindigkeit " + newSpeed * 3.6);
            train.setMaxV(newSpeed);
        }
    }

    private static List<Train> readTrains() throws FileNotFoundException {
        List<Train> trains = new ArrayList<>();

        String name = "";
        double vMax = 0;
        double acc = 0;
        double breaks = 0;
        double length = 0;
        double numberOf = 0;
        double rang = 0;
        boolean passenger = false;
        double probabilityDelay = 0;
        double averageDelay = 0;
        double stopTime = 0;

        File object = new File("inputData/train.txt");

        if (!object.exists()) {
            System.out.println("Zugdatendatei nicht gefunden");
            System.exit(103);
        }

        List<String> names = findName(object);
        List<Double> values = findNumbers(object);
        List<Boolean> bools = findBoolean(object);

        //Prüfen ob alle drei Listen sich durch die richtige länge teilen lassen
        if ((names.size() + values.size() + bools.size()) % 11 != 0) {
            System.out.println("Input Datei Züge fehlerhaft");
            System.exit(103);
        }

        //Anzahl der Züge in der Input-Datei finden
        int trainNumber = (names.size() + values.size() + bools.size()) / 11;

        for (int i = 0; i < trainNumber; i++) {
            name = names.get(i);
            vMax = values.get(9 * i);
            acc = values.get(1 + 9 * i);
            breaks = values.get(2 + 9 * i);
            length = values.get(3 + 9 * i);
            numberOf = values.get(4 + 9 * i);
            rang = values.get(5 + 9 * i);
            passenger = bools.get(i);
            probabilityDelay = values.get(6 + 9 * i);
            averageDelay = values.get(7 + 9 * i);
            stopTime = values.get(8 + 9 * i);

            trains.add(new Train(vMax, acc, breaks, length, numberOf, rang, passenger, probabilityDelay, averageDelay, stopTime, name));

        }

        return trains;
    }

    private static List<Track> readTracks() throws FileNotFoundException {
        List<Track> tracks = new ArrayList<>();

        File object = new File("inputData/track.txt");

        if (!object.exists()) {
            System.out.println("Streckendatendatei nicht gefunden");
            System.exit(103);
        }

        Scanner reader = new Scanner(object);
        reader.useLocale(Locale.ENGLISH);

        //neun Zeilen skippen für den Einleitungstext
        for (int i = 0; i < 9; i++) {
            reader.nextLine();
        }

        double length = 0;
        double speed = 0;
        String element = "";
        double buildTime = 0;
        double resetTime = 0;

        while (reader.hasNextLine()) {

            if (reader.hasNextDouble()) length = reader.nextDouble();

            if (reader.hasNextDouble()) speed = reader.nextDouble();

            if (!reader.hasNextDouble()) element = reader.next();

            if (element.equals("mainSignal")) buildTime = reader.nextDouble();

            if (element.equals("mainSignal")) resetTime = reader.nextDouble();

            tracks.add(new Track(length, speed, element, buildTime, resetTime));

            //Alle Werte wieder zurücksetzen damit die Entscheidungsfindung wieder funktioniert
            length = 0;
            speed = 0;
            element = "";
            buildTime = 0;
            resetTime = 0;

        }

        return tracks;
    }

    private static double readLineSpeed() throws FileNotFoundException {

        File object = new File("inputData/track.txt");

        if (!object.exists()) {
            System.out.println("Streckendatendatei nicht gefunden");
            System.exit(103);
        }

        Scanner reader = new Scanner(object);
        reader.useLocale(Locale.ENGLISH);

        //vier Zeilen skippen für den Einleitungstext
        for (int i = 0; i < 4; i++) {
            reader.nextLine();
        }

        double lineSpeed = 0;
        Scanner data = new Scanner(reader.nextLine());
        data.next();
        if (data.hasNextDouble()) lineSpeed = data.nextDouble();
        else System.out.println("Fehler");

        return lineSpeed;
    }

    private static double readTime() throws FileNotFoundException {

        File object = new File("inputData/track.txt");

        if (!object.exists()) {
            System.out.println("Streckendatendatei nicht gefunden");
            System.exit(103);
        }

        Scanner reader = new Scanner(object);
        reader.useLocale(Locale.ENGLISH);

        //fünf Zeilen skippen für den Einleitungstext
        for (int i = 0; i < 5; i++) {
            reader.nextLine();
        }

        double timeSpan = 0;
        Scanner data = new Scanner(reader.nextLine());
        data.next();
        if (data.hasNextDouble()) timeSpan = data.nextDouble();
        else System.out.println("Fehler");

        return timeSpan;
    }

    private static List<Double> findNumbers(File file) throws FileNotFoundException {
        List<Double> numbers = new ArrayList<>();

        Scanner reader = new Scanner(file);
        reader.useLocale(Locale.ENGLISH);

        while (reader.hasNext()) {
            if (reader.hasNextDouble()) numbers.add(Double.valueOf(reader.next()));
            else reader.next();
        }
        return numbers;
    }

    private static List<String> findName(File file) throws FileNotFoundException {
        List<String> names = new ArrayList<>();

        Scanner reader = new Scanner(file);
        reader.useLocale(Locale.ENGLISH);

        //Den ersten Eintrag zur Liste hinzufügen
        names.add(reader.next());

        reader.nextLine(); //Workaround damit nicht die Geschwindigkeit in die Liste hinzugefügt wird

        //Jeden Eintrag hinzufügen der auf eine Leerzeile folgt
        while (reader.hasNextLine()) {
            if (reader.hasNextLine() && reader.nextLine().isEmpty()) names.add(reader.nextLine());

        }

        return names;
    }

    private static List<Boolean> findBoolean(File file) throws FileNotFoundException {
        List<Boolean> bool = new ArrayList<>();

        Scanner reader = new Scanner(file);
        reader.useLocale(Locale.ENGLISH);

        while (reader.hasNext()) {
            if (reader.hasNextBoolean()) bool.add(reader.nextBoolean());
            else reader.next();
        }
        return bool;
    }

} //end of soap