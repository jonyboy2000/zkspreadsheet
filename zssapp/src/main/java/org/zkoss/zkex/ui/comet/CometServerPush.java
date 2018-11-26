/* CometServerPush.java

	Purpose:

	Description:

	History:
		Tue May  6 10:20:05     2008, Created by tomyeh

Copyright (C) 2008 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zkex.ui.comet;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.lang.Library;
import org.zkoss.zk.au.out.AuScript;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.DesktopUnavailableException;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.DesktopCtrl;
import org.zkoss.zk.ui.sys.Scheduler;
import org.zkoss.zk.ui.sys.ServerPush;
import org.zkoss.zk.ui.sys.WebAppCtrl;
import org.zkoss.zk.ui.util.Clients;

/**
 * A server-push implementation that is based on comet.
 * This implementation is compatible with any version of Servlet. It also means
 * it doesn't utilize Servlet 3 asynchronous processing support.
 * If you are using Servlet 3 servers, it is recommended to use
 * {@link org.zkoss.zkmax.ui.comet.CometServerPush} instead.
 *
 * <p>Available in ZK PE.
 * @author tomyeh
 * @since 6.0.0
 */
public class CometServerPush implements ServerPush {
    private static final Logger log = LoggerFactory.getLogger(CometServerPush.class);

    /** Denote a server-push thread gives up the activation (timeout). */
    private static final int GIVEUP = -99;

    private Desktop _desktop;
    /** List of ThreadInfo that called activate but not granted. */
    private final List<ThreadInfo> _pending = new LinkedList<ThreadInfo>();
    /** The active thread. */
    private ThreadInfo _active;
    /** A mutex that is used by this object to wait for the server-push thread
     * to complete.
     */
    private final Object _mutex = new Object();
    /** The asynchronous information. */
    private AsyncInfo _ai;
    /** Not null if process() is called before activate(). */
    private ThreadInfo _ready;
    /** Used to denote deactivate() was called. */
    private boolean _busy;

    /** Sends an AU response to the client to start the server push.
     * It is called by {@link #start}.
     * <p>The derived class usually overrides this method to support
     * different devices.
     * <p>The default implementation is to send an {@link AuScript} response to contain
     * the script specified in a preference called <code>CometServerPush.start</code>,
     * or the script returned by {@link #getStartScript}, if the preference is not found.
     * Devices that don't support scripts could override this method
     * to send a custom AU response ({@link org.zkoss.zk.au.AuResponse}).
     */
    protected void startClientPush() {
        String script = _desktop.getWebApp().getConfiguration().getPreference("CometServerPush.start", null);
        if (script == null)
            script = getStartScript();
        // B65-ZK-1913: Make the key different then stopClientPush
        Clients.response("zk.cometpush.start", new AuScript(null, script));
    }

    /** Sends an AU response the client to stop the server push.
     * <p>The derived class usually overrides this method to support
     * different devices, such as ZK Mobile.
     * <p>The default implementation is to send an {@link AuScript} instance to contain
     * the script specified in a preference called <code>CometServerPush.stop</code>,'
     * or the script returned by {@link #getStopScript}, if the preference is not found.
     * Devices that don't support scripts could override this method
     * to send a custom AU response ({@link org.zkoss.zk.au.AuResponse}).
     */
    protected void stopClientPush() {
        String script = _desktop.getWebApp().getConfiguration().getPreference("CometServerPush.stop", null);
        if (script == null)
            script = getStopScript();
        // B65-ZK-1913: Make the key different then startClientPush
        Clients.response("zk.cometpush.stop", new AuScript(null, script));
    }

    /** Returns the JavaScript codes to enable (a.k.a., start) the server push.
     * It is called by {@link #startClientPush} to prepare the script
     * of {@link AuScript} that will be sent to the client.
     */
    protected String getStartScript() {
        return getStartScript("zkex");
    }

    protected String getStartScript(String zkediton) {
        // ZK-2169: the default value changes from false to true
        boolean disabled = Boolean
                .valueOf(Library.getProperty("org.zkoss.zkex.ui.comet.smartconnection.disabled", "true"));
        //ZK-3188: read from preference if any
        final int retryDelay = getIntPref("CometServerPush.retry.delay"),
                ajaxTimeout = getIntPref("CometServerPush.ajax.timeout");
        String retryCountStr = "-1";
        String s = _desktop.getWebApp().getConfiguration().getPreference("CometServerPush.retry.count", null);
        if (s != null) {
            try {
                int retryCount = Integer.parseInt(s);
                if (retryCount == -1)
                    retryCountStr = "Infinity";
                else
                    retryCountStr = String.valueOf(retryCount);
            } catch (NumberFormatException ex) {
                log.warn("Not a number specified at CometServerPush.retry.count");
            }
        }
        // B65-ZK-1913: Make sure server push started after zkex.cmsp loaded
        return "zk.load('zkex.cmsp');zk.afterLoad('zkex.cmsp', function(){" + zkediton + ".cmsp.start('" + _desktop.getId() + "',"
                + disabled + ", " + retryCountStr + ", " + retryDelay + ", " + ajaxTimeout + ");});";
    }

    /**
     * Retrieve the integer preference by the given key.
     * Visibility set to protected so that zkmax can have access to this method.
     */
    protected int getIntPref(String key) {
        final String s = _desktop.getWebApp().getConfiguration().getPreference(key, null);
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                log.warn("Not a number specified at " + key);
            }
        }
        return -1;
    }

    /** Returns the JavaScript codes to disable (a.k.a., stop) the server push.
     * It is called by {@link #stopClientPush} to prepare the script
     * of {@link AuScript} that will be sent to the client.
     */
    protected String getStopScript() {
        // B65-ZK-1913: stop after zkex.cmsp loaded
        return "zk.afterLoad('zkex.cmsp', function(){zkex.cmsp.stop('" + _desktop.getId() + "');});";
    }

    /** Returns the desktop that this server push belongs to (never null).
     */
    public Desktop getDesktop() {
        return _desktop;
    }

    //ServerPush//
    public boolean isActive() {
        return _active != null && _active.nActive > 0;
    }

    /** Starts the server push.
     * <p>The derived class rarely need to override this method.
     * Rather, override {@link #startClientPush}.
     */
    public void start(Desktop desktop) {
        if (_desktop != null) {
            log.warn("Ignored: Sever-push already started");
            return;
        }

        _desktop = desktop;
        onStart();
        startClientPush();
    }

    /**
     * ZK-1777 resume serverpush after DesktopRecycling
     */
    public void resume() {
        if (_desktop == null) {
            throw new IllegalStateException(
                    "ServerPush cannot be resumed without desktop, or has been stopped!call #start(desktop)} instead");
        }
        startClientPush();
    }

    /** Stops the server push.
     * <p>The derived class rarely need to override this method.
     * Rather, override {@link #stopClientPush}.
     */
    public void stop() {
        if (_desktop == null) {
            log.warn("Ignored: Sever-push not started");
            return;
        }

        final Execution exec = Executions.getCurrent();
        final boolean inexec = exec != null && exec.getDesktop() == _desktop;
        //it might be caused by DesktopCache expunge (when creating another desktop)
        try {
            if (inexec && _desktop.isAlive()) //Bug 1815480: don't send if timeout
                stopClientPush();
        } finally {
            _desktop = null; //to cause DesktopUnavailableException being thrown
            wakePending();

            //if inexec, either in working thread, or other event listener
            //if in working thread, we cannot notify here (too early to wake).
            //if other listener, no need notify (since onPiggyback not running)
            if (!inexec) {
                synchronized (_mutex) {
                    _mutex.notify();
                }
            }
        }
        onStop();
    }

    /** Called when {@link #start} is called.
     * This method is called before {@link #startClientPush}, and used to initialize the desktop,
     * if necessary.
     * <p>The default implementation initializes an extension to process the comet request.
     */
    protected void onStart() {
        try {
            CometExtension.init(_desktop.getWebApp());
        } catch (javax.servlet.ServletException ex) {
            throw UiException.Aide.wrap(ex);
        }
    }

    /** Called when {@link #stop} is called.
     * This method is called after {@link #stopClientPush} is called and this server push
     * has been cleaned up.
     * <p>The default implementation does nothing.
     */
    protected void onStop() {
    }

    private void wakePending() {
        synchronized (_pending) {
            for (ThreadInfo info : _pending) {
                synchronized (info) {
                    info.notify();
                }
            }
            _pending.clear();
            _pending.notify(); //wake process()
            cleanAsyncInfo();
        }
    }

    public void onPiggyback() {
    }

    public <T extends Event> void schedule(EventListener<T> listener, T event, Scheduler<T> scheduler) {
        scheduler.schedule(listener, event); //delegate back
        synchronized (_pending) {
            _pending.notify(); //wake up comet servlet
            cleanAsyncInfo();
        }
    }

    /** Sets busy and return if it is busy for processing other request.
     * If it is busy, true is returned.
     * If it is not busy, false is returned but it is marked as busy.
     *
     * <p>It is possible since the client might abort the previous one
     * and issue a new one but their server didn't know.
     */
    protected synchronized boolean setBusy() {
        final boolean old = _busy;

        // toggle the status for the next request to check, we won't keep the true for ever
        _busy = !old;

        // Bug ZK-2269, we should wake up all the pending processors, if they may be dead.
        if (old) {
            synchronized (_pending) {
                for (ThreadInfo info : _pending) {
                    synchronized (info) {
                        info.notify();
                    }
                }
                _pending.clear();

                // Bug ZK-2269 wake all process(), if they may be dead
                _pending.notifyAll();
                cleanAsyncInfo();
            }
        }
        return old;
    }

    // Bug ZK-1979
    /**package*/
    synchronized void hibernate() {
        Desktop desktop = _desktop;
        try {

            final Execution exec = Executions.getCurrent();
            final boolean inexec = exec != null && exec.getDesktop() == _desktop;
            //it might be caused by DesktopCache expunge (when creating another desktop)
            if (!inexec) {
                if (log.isDebugEnabled()) {
                    log.debug("hibernate the server push");
                }
                stop();
            }
        } finally {
            _desktop = desktop;
        }
    }

    /** Called when receiving the comet request from the client.
     */
    protected void processRequest(Execution exec, AsyncInfo ai) throws java.io.IOException {
        //Activation is done by the working thread, so we process one
        //pending at a time. Otherwise, AU request might be processed
        //between two working threads and then it might fail to update
        //at the client (client might process AU response first and then
        //the result of two working threads and it is wrong).

        org.zkoss.zkex.rt.Runtime.init(exec);
        cleanAsyncInfo(); //just in case

        for (ThreadInfo info = null;;) {
            try {
                boolean readyUsed = false;
                synchronized (_pending) {
                    while (!readyUsed && _pending.isEmpty()) { //no one activate
                        if (!_busy || // for Bug ZK-2269 if not in a busy state, we need to release the current thread, the because upcoming thread will be in the waiting queue.
                                (_desktop == null //aborted
                                        || ((DesktopCtrl) _desktop).scheduledServerPush())) { //AU's job to run scheduled tasks

                            // Bug ZK-2747
                            if (ai != null)
                                ai.complete();

                            if (log.isDebugEnabled()) {
                                log.debug("Returning the processRequest(), _busy [{}], _desktop [{}]", _busy, _desktop);
                            }
                            return;
                        }

                        _ready = new ThreadInfo(null);
                        _ready.setActive(exec);
                        if (ai != null) { //asynchronous
                            _ai = ai; //queue
                            return; //done
                        }
                        try {
                            _pending.wait(10 * 60 * 1000); //wait activate()
                            readyUsed = _ready == null; //activate() called
                        } finally {
                            _ready = null;
                        }
                    }
                    if (!readyUsed) {
                        if (_pending.isEmpty()) {
                            if (ai != null)
                                ai.complete();

                            if (log.isDebugEnabled()) {
                                log.debug("Caused by schedule(), so done (to get back scheduled task)");
                            }
                            return; //caused by schedule(), so done (to get back scheduled task)
                        }
                        info = _pending.remove(0);
                    }
                }

                if (!readyUsed) {
                    synchronized (info) {
                        if (info.nActive == GIVEUP)
                            continue; //give up and try next
                        info.setActive(exec);
                        info.notify();
                    }
                }

                if (ai != null) { //asynchronous
                    _ai = ai; //queue

                    if (log.isDebugEnabled()) {
                        log.debug("Asynchronous done");
                    }
                    return; //done
                }

                synchronized (_mutex) {
                    if (_busy && _desktop != null) //not abort
                        _mutex.wait(); //wait until working thread complete
                }
                if (log.isDebugEnabled()) {
                    log.debug("Done (handle one pending at a time)");
                }
                return; //done (handle one pending at a time)
            } catch (InterruptedException ex) {
                throw UiException.Aide.wrap(ex);
            } finally {
                if (ai == null && info != null) {
                    info.exec = null;
                }
                _busy = false; //just in case
            }
        }
    }

    private void cleanAsyncInfo() {
        //a patch for ZSS-1365 model dirty listener throws Null Pointer Exception when deactivate a desktop
        if (_ai != null && !isActive()) {
            synchronized (_pending) {
                if (_ai != null) {
                    _ready = null; //Bug 1620: must clear ready too
                    _ai.complete();
                    _ai = null;
                }
            }
        }
    }

    public boolean activate(long timeout) throws InterruptedException, DesktopUnavailableException {
        final Thread curr = Thread.currentThread();
        if (_active != null && _active.thread.equals(curr)) { //re-activate
            ++_active.nActive;

            if (log.isDebugEnabled()) {
                log.debug("Re-activate");
            }
            return true;
        }

        ThreadInfo info = new ThreadInfo(curr);
        synchronized (_pending) {
            if (_desktop != null) {
                if (_ready != null) { //comet servlet ready
                    (info = _ready).thread = curr;
                    _ready = null; //means _ready is used
                    _pending.notify(); //wait up comet servlet, i.e., process()
                } else {
                    _pending.add(info);
                }
            }
        }

        boolean loop;
        do {
            loop = false;
            boolean bTimeout = false;
            boolean bDead = false;
            try {
                synchronized (info) {
                    if (_desktop != null) {
                        if (info.nActive == 0) //not granted yet
                            info.wait(timeout <= 0 ? 10 * 60 * 1000 : timeout);

                        if (info.nActive <= 0) { //not granted
                            bTimeout = timeout > 0;
                            bDead = _desktop == null || !_desktop.isAlive();
                            if (bTimeout || bDead) { //not timeout
                                info.nActive = GIVEUP; //denote timeout (and give up)

                                if (bDead)
                                    throw new DesktopUnavailableException("Stopped");
                                return false; //timeout
                            }

                            if (log.isDebugEnabled())
                                log.debug("Executions.activate() took more than 10 minutes");
                            loop = true; //try again
                        }
                    }
                }
            } finally {
                if (bTimeout || bDead) {
                    synchronized (_pending) { //undo pending
                        _pending.remove(info);
                    }
                }
            }
        } while (loop);

        if (_desktop == null)
            throw new DesktopUnavailableException("Stopped");
        org.zkoss.zkex.rt.Runtime.init(_desktop);

        _active = info;
        ((WebAppCtrl) _desktop.getWebApp()).getUiEngine().beginUpdate(_active.exec);

        if (log.isDebugEnabled()) {
            log.debug("Activated");
        }
        return true;
    }

    public boolean deactivate(boolean stop) {
        boolean stopped = false;
        if (_active != null && Thread.currentThread().equals(_active.thread)) {
            if (--_active.nActive <= 0) {
                final Execution exec = _active.exec;
                _active.exec = null; //just in case
                _active.nActive = 0; //just in case
                _active = null;
                if (exec != null)
                    try {
                        if (stop)
                            stopClientPush();
                        ((WebAppCtrl) _desktop.getWebApp()).getUiEngine().endUpdate(exec);
                    } catch (Throwable ex) {
                        log.warn("Ignored error", ex);
                    }

                if (stop) {
                    _desktop = null; //to cause DesktopUnavailableException being thrown
                    wakePending();
                    stopped = true;
                }

                //wake up process()
                synchronized (_mutex) {
                    _busy = false;
                    _mutex.notify();
                }

                cleanAsyncInfo();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Deactivate, and should be stopped [{}] ", stopped);
        }
        return stopped;
    }

    /** The info of a server-push thread.
     * It is also a mutex used to start a pending server-push thread.
     */
    private static class ThreadInfo {
        private Thread thread;
        /** # of activate() was called. */
        private int nActive;
        private Execution exec;

        private ThreadInfo(Thread thread) {
            this.thread = thread;
        }

        private void setActive(Execution exec) {
            this.nActive = 1;
            this.exec = exec;
        }

        public String toString() {
            return "[" + thread + ',' + nActive + ']';
        }
    }

    /** The interface for implementing asynchronous processing.
     * It is overridden and implemented in zkmax.
     */
    public static interface AsyncInfo {
        /** Called to complete the request.
         */
        public void complete();
    }
}
