package hero.bane.config;

public final class SpeedAnchors {
    public enum Direction {
        FORWARD,
        BACKWARD;

        public String display() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    public enum Anchor {
        CROUCH(1.30),
        WALK(4.32),
        SPRINT(5.61),
        MAX(9.60);

        public final double defaultValue;

        Anchor(double defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String display() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    public static final Anchor[] ANCHORS = Anchor.values();

    private static final double EPSILON = 0.01;

    private final double[] forward = defaults();
    private final double[] backward = defaults();
    private Resolved cache;

    public double get(Direction direction, Anchor anchor) {
        return sign(direction) * row(direction)[anchor.ordinal()];
    }

    public static boolean accepts(Direction direction, double value) {
        return direction == Direction.FORWARD ? value > 0.0 : value < 0.0;
    }

    public void set(Direction direction, Anchor anchor, double value) {
        double[] row = row(direction);
        int index = anchor.ordinal();

        row[index] = Math.max(Math.abs(value), EPSILON * (index + 1));
        for (int j = index + 1; j < row.length; j++) row[j] = Math.max(row[j], row[j - 1] + EPSILON);
        for (int j = index - 1; j >= 0; j--) row[j] = Math.min(row[j], row[j + 1] - EPSILON);

        cache = null;
    }

    public void reset() {
        System.arraycopy(defaults(), 0, forward, 0, ANCHORS.length);
        System.arraycopy(defaults(), 0, backward, 0, ANCHORS.length);
        cache = null;
    }

    public Resolved resolved() {
        Resolved current = cache;
        if (current == null) {
            current = new Resolved(ladder(forward), ladder(backward));
            cache = current;
        }
        return current;
    }

    private double[] row(Direction direction) {
        return direction == Direction.FORWARD ? forward : backward;
    }

    private static double sign(Direction direction) {
        return direction == Direction.FORWARD ? 1.0 : -1.0;
    }

    private static double[] defaults() {
        double[] values = new double[ANCHORS.length];
        for (Anchor anchor : ANCHORS) values[anchor.ordinal()] = anchor.defaultValue;
        return values;
    }

    private static double[] ladder(double[] raw) {
        double[] out = new double[raw.length];
        double previous = 0.0;
        for (int i = 0; i < raw.length; i++) {
            out[i] = Math.max(Math.abs(raw[i]), previous + EPSILON);
            previous = out[i];
        }
        return out;
    }

    public static final class Resolved {
        private final double[] forward;
        private final double[] backward;

        private Resolved(double[] forward, double[] backward) {
            this.forward = forward;
            this.backward = backward;
        }

        public double forward(Anchor anchor) {
            return forward[anchor.ordinal()];
        }

        public double backward(Anchor anchor) {
            return backward[anchor.ordinal()];
        }
    }
}
