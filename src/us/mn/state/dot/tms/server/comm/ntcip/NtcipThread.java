/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2000-2016  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ntcip;

import java.io.IOException;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.CommThread;
import us.mn.state.dot.tms.server.comm.Messenger;
import us.mn.state.dot.tms.server.comm.OpController;
import us.mn.state.dot.tms.server.comm.OpQueue;
import us.mn.state.dot.tms.server.comm.snmp.SNMP;

/**
 * NTCIP thread
 *
 * @author Douglas Lau
 */
public class NtcipThread extends CommThread {

	/** SNMP message protocol */
	private final SNMP snmp = new SNMP();

	/** Create a new Ntcip thread */
	@SuppressWarnings("unchecked")
	public NtcipThread(NtcipPoller p, OpQueue q, Messenger m) {
		super(p, q, m);
	}

	/** Create a new message for the specified operation */
	@Override
	protected CommMessage createCommMessage(OpController o)
		throws IOException
	{
		ControllerImpl c = o.getController();
		return snmp.new Message(messenger.getOutputStream(c),
			messenger.getInputStream("", c), c.getPassword());
	}
}