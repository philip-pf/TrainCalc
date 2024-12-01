import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RunTime {

    public static List<List<Track>> makeBlocks(List<Track> tracks, Train train) {


        List<Track> trackList = new ArrayList<>(tracks); //Liste kopieren, da Teile der Liste gelöscht werden, um die Originalliste zu schützen

        boolean platform = false;
        for (Track track : trackList) {
            if (track.getElement().equals("platform")) {
                platform = true;
                break;
            }
        }

        if (train.isStop() && platform) {

            double breakLength = Calculations.calcDistanceBreaking(0.1, train);
            int indexPlatform = 0;
            int indexBreaking = 0;
/*
        Die Liste so bearbeiten, dass der Bremseinsatzpunkt für den Haltepunkt eingefügt wird
        dazu den Abschnitt in dem angefangen werden muss zu bremsen in zwei Abschnitte zerteilen
        in einen Abschnitt, der die Restlänge hat und einen folgenden in dem schon gebremst wird

 */
            //System.out.println("trackList\t" + trackList.size() + " " + trackList);
            for (Track t : trackList) {
                if (t.getElement().equals("platform")) {
                    indexPlatform = trackList.indexOf(t);//index auf den Block setzten in dem der Haltepunkt ist
                }
            }

            for (int i = indexPlatform; i >= 0; i--) {
                if (breakLength - trackList.get(i).getLength() > 0) {
                    breakLength -= trackList.get(i).getLength();
                } else {
                    indexBreaking = i;//index auf den Abschnitt setzten in dem angefangen werden muss zu bremsen
                    break;
                }
            }

            trackList.add(indexBreaking, new Track((trackList.get(indexBreaking).getLength() - breakLength), (trackList.get(indexBreaking).getSpeed() * 3.6), trackList.get(indexBreaking).getElement(), trackList.get(indexBreaking).getBuildTime(), trackList.get(indexBreaking).getResetTime()));
            trackList.remove(indexBreaking + 1);
            trackList.add(indexBreaking + 1, new Track(breakLength, 0.1 /3.6, "null"));

            indexPlatform += 1;
            indexBreaking += 1;

            //alle Abschnitte bis zum Bahnsteigende die Geschwindigkeit 0.1 geben, außer dem in dem gebremst werden soll und den neuen
            for (int i = indexBreaking; i <= indexPlatform; i++) {
                trackList.add(i, new Track(trackList.get(i).getLength(), 0.1 / 3.6, trackList.get(i).getElement(), trackList.get(i).getBuildTime(), trackList.get(i).getResetTime()));
                trackList.remove(i + 1);
            }

        }

        //Anzahl der Blöcke zählen
        int blockAmount = 1;

        for (
                Track t : trackList) {
            if (Objects.equals(t.getElement(), "mainSignal")) {
                blockAmount++;
            }
        }

        List<List<Track>> blocks = new ArrayList<>();

        for (int i = 0; i < blockAmount; i++) {

            List<Track> block = new ArrayList<>();

            block.add(trackList.get(0));
            trackList.remove(trackList.get(0));

            for (Track track : trackList) {

                if (Objects.equals(track.getElement(), "mainSignal")) {
                    break;
                } else {
                    block.add(track);
                }

            }

            trackList.removeAll(block);
            blocks.add(block);

        }

        return blocks;

    }

    public static List<List<Double>> calcRunTime(List<Track> tracks, Train train) {

        List<List<Track>> trackList = makeBlocks(tracks, train);
        List<List<Double>> runTimeComplete = new ArrayList<>(); //Enthält die Fahrzeiten aller Abschnitte
        List<Track> trackListComplete = new ArrayList<>();
        int index = 0;

        //alle Abschnitte hintereinander in trackList speichern als Ersatz für tracks
        for (List<Track> section : trackList) {
            trackListComplete.addAll(section);
        }

        for (List<Track> section : trackList) {
            List<Double> blockTime = new ArrayList<>();

            for (Track track : section) {

                //System.out.println("--");
                //System.out.println("Aktuelle Geschwindigkeit: " + train.getActualSpeed() * 3.6 + " km/h");
                //System.out.println("Abschnitt Nr.: " + index + " - Länge: " + track.getLength() + "m");

                if (Math.min(track.getSpeed(), train.getMaxV()) == train.getActualSpeed()) {

                    //System.out.println("constant");
                    blockTime.add(Calculations.calcTimeConstant (track.getLength(), train.getActualSpeed()));

                } else if (Math.min(track.getSpeed(), train.getMaxV()) > train.getActualSpeed()) {

                    //System.out.println("accelerating");
                    blockTime.add(Calculations.calcTimeBlockAccelerating (track, train, trackListComplete.get(Math.max(index - 1, 0))));

                } else if (Math.min(track.getSpeed(), train.getMaxV()) < train.getActualSpeed()) {

                    //System.out.println("breaking");
                    blockTime.add(Calculations.calcTimeBlockBreaking (track.getLength(), track.getSpeed(), train));

                } else {
                    System.out.println("Fehler Geschwindigkeit nicht vergleichbar");
                    System.exit(102);
                }

                //Wenn in diesem Durchlauf der Haltepunkt bearbeitet wurde, den Eintrag um die Haltezeit verlängern
                if (track.getElement().equals("platform") && train.isStop()) {
                    blockTime.set(blockTime.size() - 1, blockTime.get(blockTime.size() - 1) + train.getStopTime());
                    System.out.println(track.getElement());
                }
                index++; //Index hochzählen bevor der nächste Abschnitt bearbeitet wird

            }

            //System.out.println("blockTime - " + blockTime);
            runTimeComplete.add(blockTime);

        }

        return runTimeComplete;

    }

    public static List<Double> compactRunTime(List<List<Double>> runTimeList) {

        double blockRunTime = 0;
        List<Double> runTime = new ArrayList<>();

        for (List<Double> timeList : runTimeList) {

            for (double time : timeList) {

                blockRunTime += time;

            }

            runTime.add(blockRunTime);
            blockRunTime = 0;

        }

        return runTime;
    }

    public static List<Double> calcApproachTime(List<Track> tracks, Train train) {

        List<List<Object>> approachSectionIndex = new ArrayList<>();
        List<Track> trackList = new ArrayList<>();

        //alle Abschnitte in trackList hintereinander speichern als Ersatz für tracks
        for (List<Track> section : makeBlocks(tracks, train)) {
            trackList.addAll(section);
        }

        for (Track t : trackList) {

            if (Objects.equals(t.getElement(), "distantSignal")) {

                ArrayList<Object> approachIndex = new ArrayList<>();

                for (int i = trackList.indexOf(t); i < trackList.size(); i++) {

                    if (Objects.equals(trackList.get(i).getElement(), "mainSignal")) break;

                    approachIndex.add(trackList.indexOf(trackList.get(i)));

                }

                approachSectionIndex.add(approachIndex);

            }

        }

        //Alle Zeiten hintereinander in einer Liste speichern, damit man mit dem Index die Zeiten für die Annäherungsfahrzeit bekommen kann
        List<Double> times = new ArrayList<>();

        for (List<Double> time : calcRunTime(tracks, train)) {
            times.addAll(time);
        }

        List<List<Double>> approachTime = new ArrayList<>();

        for (List<Object> sectionIndex : approachSectionIndex) {

            List<Double> approachTimeBlock = new ArrayList<>();

            for (Object t : sectionIndex) {

                approachTimeBlock.add(times.get((Integer) t));

            }
            approachTime.add(approachTimeBlock);

        }

        //Endgültige Annäherungsfahrzeits liste
        List<Double> approachTimes = new ArrayList<>();

        //Die einzelnen Abschnittszeiten zusammenrechnen
        double approachTimeSection = 0;
        for (List<Double> timeBlock : approachTime) {
            for (Double time : timeBlock) {
                approachTimeSection += time;
            }

            approachTimes.add(approachTimeSection);

            approachTimeSection = 0;
        }

        return approachTimes;

    }

    public static List<Double> calcClearingTime(List<Track> tracks, Train train) {

        List<Track> trackList = new ArrayList<>();
        List<Integer> indexOfSignal = new ArrayList<>();
        List<Double> times = new ArrayList<>();
        List<Double> clearingTime = new ArrayList<>();

        //Alle Abschnitte in trackList speichern als ersatz für tracks
        for (List<Track> section : makeBlocks(tracks, train))
            trackList.addAll(section);

        for (Track t : trackList) {

            if (Objects.equals(t.getElement(), "mainSignal")) {

                indexOfSignal.add(trackList.indexOf(t));

            }
        }

        for (List<Double> time : calcRunTime(trackList, train)) {
            times.addAll(time);
        }

        for (int t : indexOfSignal)
            clearingTime.add(times.get(t));

        return clearingTime;
    }

    public static List<List<Double>> makeRunList(List<Track> tracks, Train train) {

        //dem Zug die Geschwindigkeit des ersten Abschnittes geben
        train.setActualSpeed(Math.min(tracks.get(0).getSpeed(), train.getMaxV()));

        //System.out.println("--Fahrzeit--");
        List<Double> runTime = compactRunTime(calcRunTime(tracks, train));
        train.setActualSpeed(Math.min(tracks.get(0).getSpeed(), train.getMaxV()));

        //System.out.println("--Anrückzeit--");
        List<Double> approachTime = calcApproachTime(tracks, train);
        train.setActualSpeed(Math.min(tracks.get(0).getSpeed(), train.getMaxV()));

        //System.out.println("--Räumzeit--");
        List<Double> clearingTime = calcClearingTime(tracks, train);
        train.setActualSpeed(Math.min(tracks.get(0).getSpeed(), train.getMaxV()));

        //Die Listen mit den Zeiten so anpassen, dass die Zeiten die nicht benötigt werden entfernt werden
        runTime.remove(0); //Fahrzeit des Annäherungsabschnitts entfernen
        runTime.remove(runTime.size() - 1); //Fahrzeit des Räumungsabschnitts hinter dem Einfahrsignal entfernen
        approachTime.remove(approachTime.size() - 1); //Annäherungsfahrzeit an das Einfahrsignal des nächsten Bahnhofs entfernen
        clearingTime.remove(0); //Räumfahrzeit des Ausfahrsignals entfernen

        List<Double> sectionList = new ArrayList<>();
        List<List<Double>> runList = new ArrayList<>();


        List<Track> mainSignal = new ArrayList<>();
        for (Track track : tracks) {
            if (track.getElement().equals("mainSignal")) {
                mainSignal.add(track);
            }
        }

        /*
        Für das von Pachl beschriebene Verfahren zur Ermittlung der Sperrzeiten eine Liste mit dreimal 0.0
        anlegen die dann von der for-Schleife für jeden Abschnitt mit den in den Listen oben bestimmten Werten gefüllt wird.
         */

        for (int i = 0; i < 3; i++) {
            sectionList.add(0.0);
        }

        //Schleife über alle Abschnitte
        for (int i = 0; i < runTime.size(); i++) {

            //Zunächst den zweiten Eintrag für die Fahrzeit bestimmen
            if (i == 0) sectionList.set(1, 0.0);
            else sectionList.set(1, sectionList.get(1) + runTime.get(i - 1));

            //Wenn die Fahrzeit bestimmt ist, die Anrückfahrzeit abziehen
            sectionList.set(0, sectionList.get(1) - 12 - approachTime.get(0) - mainSignal.get(i).getBuildTime());

            //zuletzt die Räumfahrzeit auf die Fahrzeit addieren
            sectionList.set(2, sectionList.get(1) + runTime.get(i) + clearingTime.get(i) + mainSignal.get(i).getResetTime());


            //Kopie der sectionList in jedem Durchlauf neu anlegen damit richig in die runList eingefügt wird
            List<Double> sectionListCopy = new ArrayList<>(sectionList);
            runList.add(sectionListCopy);

        }

        return runList;


    }

}