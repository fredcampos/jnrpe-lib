/*
 * Copyright (c) 2008 Massimiliano Ziccardi Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package it.jnrpe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class implements a Thread that kills all the JNRPEServerThread threads
 * that runs for more than a specified number of seconds.
 *
 * @author Massimiliano Ziccardi
 */
class ThreadTimeoutWatcher extends Thread
{
    /**
     * The default thread timeout (seconds).
     */
    private static final int DEFAULT_THREAD_TIMEOUT = 10;

    /**
     * The value of one second.
     */
    private static final int SECOND = 1000;

    /**
     * The list of the currently executing thread.
     */
    private List<ThreadData> m_ThreadList = Collections
            .synchronizedList(new ArrayList<ThreadData>());

    /**
     * Checks whether a thread needs to be killed every m_iPollingTime seconds.
     * (defaults to 2).
     */
    private static int m_iPollingTime = 2;

    /**
     * This variable is used to stop the thread.
     */
    private static boolean m_bRun = true;

    /**
     * The maximum number of seconds a thread is allowed to run (default to.
     * {@link #DEFAULT_THREAD_TIMEOUT} )
     */
    private static int m_iThreadTimeout = DEFAULT_THREAD_TIMEOUT;

    /**
     * Utility class used to store thread informations.
     *
     * @author Massimiliano Ziccardi
     */
    private static class ThreadData
    {
        /**
         * The thread being monitored.
         */
        private Thread m_thread;

        /**
         * The time when the thread started.
         */
        private long m_lStartTime;

        /**
         * Builds and initialized the object.
         *
         * @param t
         *            The thread to be monitored
         */
        ThreadData(final Thread t)
        {
            m_thread = t;
            m_lStartTime = System.currentTimeMillis();
        }

        /**
         * Returns the time when the thread started.
         *
         * @return The time when the thread started
         */
        public long getStartTime()
        {
            return m_lStartTime;
        }

        /**
         * Returns the thread being monitored.
         *
         * @return The thread being monitored
         */
        public Thread getThread()
        {
            return m_thread;
        }
    }

    /**
     * Adds the specified thread to the list of threads to be watched.
     *
     * @param t The thread to be watched
     */
    public void watch(final Thread t)
    {
        m_ThreadList.add(new ThreadData(t));
    }

    /**
     * Stop the thread execution.
     */
    public void stopWatching()
    {
        m_bRun = false;
    }

    /**
     * Kills the thread identified by the specified thread data.
     *
     * @param td
     *            The thread data
     */
    private void killThread(final ThreadData td)
    {
        Thread t = td.getThread();
        if (t.isAlive())
        {
            ((JNRPEServerThread) t).stopNow();
        }
    }

    /**
     * Gets the oldest thread from the list and, if needed, kills it. Threads no
     * longer running gets removed from the list.
     *
     * @return <code>true</code> if at leas one thread has been removed from the
     *         list.
     */
    private boolean killOldestThread()
    {
        // The thread in the first position is always the oldest one
        ThreadData td = (ThreadData) m_ThreadList.get(0);
        Thread t = td.getThread();

        // If the thread is not alive, or if the thread is older than
        // THREAD_TIMEOUT, it must be killed
        // and removed from the list
        if (!t.isAlive()
                || (System.currentTimeMillis() - td.getStartTime()
                        >= (m_iThreadTimeout * SECOND)))
        {
            killThread(td);
            m_ThreadList.remove(0);
            return true;
        }

        return false;
    }

    /**
     * Sets how long to wait before killing a stalled thread.
     *
     * @param iTimeout
     *            The timeout (in seconds)
     */
    public void setThreadTimeout(final int iTimeout)
    {
        m_iThreadTimeout = iTimeout;
    }

    /**
     * The run method.
     */
    public void run()
    {
        while (m_bRun)
        {
            try
            {
                Thread.sleep(m_iPollingTime * SECOND);
            }
            catch (InterruptedException e)
            {
            }

            try
            {
                // Keep on killing threads as long as there are threads to kill
                while (!m_ThreadList.isEmpty() && killOldestThread())
                {
                    // empty block
                };
            }
            catch (Throwable thr)
            {
                // This thread must never die
            }
        }

        // Stops all the threads currently running
        for (ThreadData td : m_ThreadList)
        {
            killThread(td);
        }
    }
}
