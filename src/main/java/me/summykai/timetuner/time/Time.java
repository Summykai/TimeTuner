package me.summykai.timetuner.time;

public class Time {
    public static final long DAY_LENGTH = 24000L;
    public static final long DAY_START = 0;
    public static final long NIGHT_START = 12000L;
    
    private double totalTime;

    public Time(double totalTime) {
        this.totalTime = totalTime;
    }

    public long getTicks() {
        return (long) Math.floor(totalTime);
    }

    public double getTotalTime() {
        return totalTime;
    }

    public Time add(Time other) {
        return new Time(this.totalTime + other.totalTime);
    }

    public double subtract(Time other) {
        return this.totalTime - other.totalTime;
    }

    public boolean between(Time start, Time end) {
        double timeOfDay = totalTime % DAY_LENGTH;
        double startTime = start.totalTime % DAY_LENGTH;
        double endTime = end.totalTime % DAY_LENGTH;

        if (startTime < endTime) {
            return timeOfDay >= startTime && timeOfDay < endTime;
        } else {
            return timeOfDay >= startTime || timeOfDay < endTime;
        }
    }

    public static Time fromWorldTime(long worldTime) {
        return new Time(worldTime);
    }
}