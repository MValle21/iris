/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2005-2010  Minnesota Department of Transportation
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
package us.mn.state.dot.tms;

import us.mn.state.dot.sonar.SonarObject;

/**
 * GeoLoc contains attributes necessary to describe a map location.
 *
 * @author Douglas Lau
 */
public interface GeoLoc extends SonarObject {

	/** SONAR type name */
	String SONAR_TYPE = "geo_loc";

	/** Set the roadway name */
	void setRoadway(Road r);

	/** Get the roadway name */
	Road getRoadway();

	/** Set the roadway direction */
	void setRoadDir(short d);

	/** Get the roadway direction */
	short getRoadDir();

	/** Set the cross-street name */
	void setCrossStreet(Road x);

	/** Get the cross-street name */
	Road getCrossStreet();

	/** Set the cross street direction */
	void setCrossDir(short d);

	/** Get the cross street direction */
	short getCrossDir();

	/** Set the cross street modifier */
	void setCrossMod(short m);

	/** Get the cross street modifier */
	short getCrossMod();

	/** Set the UTM Easting */
	void setEasting(Integer x);

	/** Get the UTM Easting */
	Integer getEasting();

	/** Set the UTM Northing */
	void setNorthing(Integer y);

	/** Get the UTM Northing */
	Integer getNorthing();
}
