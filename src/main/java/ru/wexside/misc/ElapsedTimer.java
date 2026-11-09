package ru.wexside.misc;

public final class ElapsedTimer {
    private long longType = System.currentTimeMillis();

    public void update() {
        this.longType = System.currentTimeMillis();
    }

    public boolean hasElapsed(long l) {
        return this.getElapsedMillis() >= l;
    }

    public boolean hasElapsedAndReset(long l) {
        if (this.hasElapsed(l)) {
            this.update();
            return true;
        }
        return false;
    }

    public long getElapsedMillis() {
        return System.currentTimeMillis() - this.longType;
    }
}

