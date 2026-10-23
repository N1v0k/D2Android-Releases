package com.winlator.d2;






final class D2InstallSteps {

    enum Step {
        FETCH(1), DOWNLOAD_LOD(12), DOWNLOAD_CLASSIC(34), KEYS(1), INSTALL_CLASSIC(24), INSTALL_LOD(14), PATCH(7), CHECK(7);


        final int weight;

        Step(int weight) {
            this.weight = weight;
        }
    }


    static final int DONE = 0, NOW = 1, LATER = 2;

    private Step current = Step.FETCH;

    private int percent = -1;


    void at(Step step) {
        current = step;
        percent = -1;
    }


    void percent(int value) {
        percent = value < 0 ? -1 : Math.min(100, value);
    }

    Step current() {
        return current;
    }

    int percent() {
        return percent;
    }

    int state(Step step) {
        int c = step.compareTo(current);
        return c < 0 ? DONE : c == 0 ? NOW : LATER;
    }



    int overall() {
        int all = 0, done = 0;
        for (Step s : Step.values()) {
            all += s.weight;
            if (s.compareTo(current) < 0) done += s.weight;
        }
        double now = percent > 0 ? current.weight * percent / 100.0 : 0;
        return (int) Math.min(100, Math.floor((done + now) * 100.0 / all));
    }


    static final long STALL_MS = 5 * 60_000L;


    static boolean stalled(long lastChange, long now) {
        return now - lastChange >= STALL_MS;
    }


    static Step download(D2InstallPlan.Product product) {
        return product == D2InstallPlan.Product.CLASSIC ? Step.DOWNLOAD_CLASSIC : Step.DOWNLOAD_LOD;
    }


    static Step install(D2InstallPlan.Product product) {
        return product == D2InstallPlan.Product.CLASSIC ? Step.INSTALL_CLASSIC : Step.INSTALL_LOD;
    }
}
