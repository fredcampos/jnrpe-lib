/*
 * Copyright (c) 2012 Massimiliano Ziccardi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.jnrpe;

import java.util.List;

import it.jnrpe.events.IJNRPEEventListener;

/**
 * A simple utility class that helps in sending events.
 *
 * @author Massimiliano Ziccardi
 */
final class EventsUtil
{
    /**
     * Default private constructor to avoid instantiations.
     */
    private EventsUtil()
    {
    }

    /**
     * Sends the given event to the given list of listeners.
     *
     * @param vListeners
     *            The list of listeners
     * @param sender
     *            The sender
     * @param sEventName
     *            The name of the event
     * @param vParams
     *            The event parameters
     */
    public static void sendEvent(final List<IJNRPEEventListener> vListeners,
            final Object sender, final String sEventName,
            final Object[] vParams)
    {
        if (vListeners == null || vListeners.isEmpty()) 
        {
            return;
        }

        SimpleEvent se = new SimpleEvent(sEventName, vParams);

        for (IJNRPEEventListener listener : vListeners)
        {
            listener.receive(sender, se);
        }
    }
}
