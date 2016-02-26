/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2016  Minnesota Department of Transportation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.server.comm.incfeed;

import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.comm.MessagePoller;
import us.mn.state.dot.tms.server.comm.Messenger;

/**
 * Incident feed poller, which periodically retrieves incidents
 * generated by an external system via the specified URL.
 *
 * @author Douglas Lau
 */
public class IncFeedPoller extends MessagePoller<IncFeedProperty> {

	/** Incident feed debug log */
	static private final DebugLog INC_LOG = new DebugLog("inc_feed");

	/** Log a message to the debug log */
	static public void log(String msg) {
		INC_LOG.log(msg);
	}

	/** Feed ID */
	private final String feed_id;

	/** Create a new poller */
	public IncFeedPoller(String n, Messenger m) {
		super(n, m);
		feed_id = n;
	}

	/** Check if a drop address is valid */
	@Override
	public boolean isAddressValid(int drop) {
		return true;
	}

	/** Query incident feed */
	public void queryIncidents(ControllerImpl c) {
		addOperation(new OpReadIncFeed(c, feed_id));
	}

	/** Get the protocol debug log */
	@Override
	protected DebugLog protocolLog() {
		return INC_LOG;
	}
}