/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2007-2010  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.pelcod;

import us.mn.state.dot.tms.server.comm.ControllerProperty;

/**
 * Pelco D Property
 *
 * @author Douglas Lau
 */
abstract public class PelcoDRequest implements ControllerProperty {

	/** Calculate the checksum of a request */
	protected byte calculateChecksum(byte[] message) {
		int i;
		byte checksum = 0;
		for(i = 1; i < 6; i++)
			checksum += message[i];
		return checksum;
	}

	/** Format a request for the specified receiver address */
	abstract public byte[] format(int drop);
}
