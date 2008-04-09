package org.lockss.test;

public interface TimingReporter {
  void startTimer();
  void stopTimer();
  void report();
}
