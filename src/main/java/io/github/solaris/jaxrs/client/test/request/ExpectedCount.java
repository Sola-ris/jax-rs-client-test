package io.github.solaris.jaxrs.client.test.request;

public final class ExpectedCount {
    private final int min;
    private final int max;

    private ExpectedCount(int min, int max) {
        if (min < 0) {
            throw new IllegalArgumentException("'min' must be >= 0");
        }
        if (max < min) {
            throw new IllegalArgumentException("'min' must be > 'max'");
        }
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public static ExpectedCount never() {
        return new ExpectedCount(0, 0);
    }

    public static ExpectedCount once() {
        return new ExpectedCount(1, 1);
    }

    public static ExpectedCount times(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("'count' must be >= 1");
        }
        return new ExpectedCount(count, count);
    }

    public static ExpectedCount min(int min) {
        if (min < 1) {
            throw new IllegalArgumentException("'min' must be >= 1");
        }
        return new ExpectedCount(min, Integer.MAX_VALUE);
    }

    public static ExpectedCount max(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("'max' must be >= 1");
        }
        return new ExpectedCount(1, max);
    }

    public static ExpectedCount between(int min, int max) {
        return new ExpectedCount(min, max);
    }
}
