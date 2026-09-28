import com.evacsim.engine.SimulationEngine;

public class Smoke {
    public static void main(String[] args) {
        run("mixed", 70, 0.55);
        run("shy-heavy", 70, 0.85);
        run("angry-heavy", 70, 0.15);
    }

    static void run(String name, int n, double shy) {
        SimulationEngine e = new SimulationEngine();
        e.reset(n, shy);
        for (int i = 0; i < 800 && !e.isFinished(); i++) {
            e.step();
        }
        var s = e.getStats();
        System.out.println(name
                + " gens=" + s.getGeneration()
                + " remaining=" + s.getRemaining()
                + " evacuated=" + s.getEvacuated()
                + " collisions=" + s.getCollisions()
                + " finished=" + e.isFinished()
                + " time=" + s.getEvacuationTime());
    }
}
