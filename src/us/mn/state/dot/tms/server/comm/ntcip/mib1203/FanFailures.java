/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2000-2009  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1203;

import us.mn.state.dot.tms.server.comm.ntcip.ASN1OctetStr;

/**
 * Ntcip FanFailures object.  This object has been deprecated by
 * NTCIP 1203 v2.
 *
 * @author Douglas Lau
 */
public class FanFailures extends ASN1OctetStr {

	/** Get the object identifier */
	public int[] getOID() {
		return MIBNode.statError.createOID(new int[] {8, 0});
	}

	/** Set the octet string value */
	public void setOctetString(byte[] v) {
		// Note: Skyline signs return 16-bit, network byte order
		if(v.length == 2 && v[0] == 0) {
			value = new byte[1];
			value[0] = v[1];
		} else
			value = v;
	}

	/** Get the object value */
	public String getValue() {
		StringBuffer buf = new StringBuffer();
		int f = 1;
		for(int i = 0; i < value.length; i++) {
			for(int b = 0; b < 8; b++, f++) {
				if((value[i] & 1 << b) != 0) {
					if(buf.length() > 0)
						buf.append(", ");
					buf.append("#");
					buf.append(f);
					buf.append(" FAILED");
				}
			}
		}
		if(buf.length() == 0)
			return "OK";
		else
			return buf.toString();
	}
}
