package com.winlator.d2.test;









public interface Zwischenziel {

    int ERREICHT = 1, AUFGEGEBEN = 2, WEG = 3;


    Ziel waehlen(Zustand z, Zustand.Ziele ziele, long now);


    Ziel nachfuehren(Ziel ziel, Zustand z, long now);





    default boolean handeln(Ziel ziel, Zustand z, Gehirn.Absicht a, long now) {
        return false;
    }





    void ende(Ziel ziel, int wie, long now);


    final class Ziel {

        public final String name;

        public final long id;
        public final double x, y;

        public final double radius;

        public final long fristMs;

        public Ziel(String name, long id, double x, double y, double radius, long fristMs) {
            this.name = name;
            this.id = id;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.fristMs = fristMs;
        }


        public Ziel bei(double nx, double ny) {
            return new Ziel(name, id, nx, ny, radius, fristMs);
        }
    }
}
