package org.lockss.test;

public abstract class TimingReporterImpl implements TimingReporter {
  protected long m_timeStart = -1;
  protected long m_count = 0;
  protected long m_sumTime = 0;
  protected long m_sumTimeSquared = 0;
  
  public void startTimer() {
    m_timeStart = System.currentTimeMillis();
  }

  public void stopTimer() {
    long timeEnd = System.currentTimeMillis();
    long delta = timeEnd - m_timeStart;
    
    // Increment all counters.
    m_count++;
    m_sumTime += delta;
    m_sumTimeSquared += delta * delta;
  }

  // The average for the time recorded.
  protected double averageTime() {
    return (double) m_sumTime / (double) m_count;
  }
  
  // The standard deviation for the time recorded.
  protected double stddevTime() {
    return Math.sqrt((m_sumTimeSquared - (m_sumTime * m_sumTime / m_count)) / (m_count - 1));
  }
}
