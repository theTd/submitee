package org.starrel.submitee;

import java.util.Iterator;
import java.util.LinkedList;

public class TimeThrottleList {
    LinkedList<Long> timeList = new LinkedList<>();
    private final long period;
    private final int threshold;

    public TimeThrottleList(long period, int threshold) {
        this.period = period;
        this.threshold = threshold;
    }

    public synchronized long getNewest() {
        return timeList.getLast();
    }

    /**
     *
     * @return false if violate
     */
    public synchronized boolean checkViolation() {
        long now = System.currentTimeMillis();
        long edge = now - period;

        Iterator<Long> iterator = timeList.descendingIterator();
        int hit = 0;
        while (iterator.hasNext()) {
            if (iterator.next() > edge) {
                if (++hit >= threshold) {
                    return false;
                }
            } else {
                break;
            }
        }
        timeList.add(now);
        while (timeList.size() > threshold) {
            timeList.removeFirst();
        }
        return true;
    }
}
