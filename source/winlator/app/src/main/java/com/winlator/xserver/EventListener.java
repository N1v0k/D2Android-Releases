package com.winlator.xserver;

import com.winlator.core.Bitmask;
import com.winlator.xserver.events.Event;

public class EventListener {
    public final XClient client;
    public final Bitmask eventMask;

    public EventListener(XClient client, Bitmask eventMask) {
        this.client = client;
        this.eventMask = eventMask;
    }

    public boolean isInterestedIn(int eventId) {
        return eventMask.isSet(eventId);
    }

    public boolean isInterestedIn(Bitmask mask) {
        return this.eventMask.intersects(mask);
    }

    public void sendEvent(Event event) {
        client.sendEvent(event); // D2Android: the same way, waiting at most a while for a client that reads nothing
    }
}
