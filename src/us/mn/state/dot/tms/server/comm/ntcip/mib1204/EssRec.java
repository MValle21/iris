/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017  Iteris Inc.
 * Copyright (C) 2019  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import java.text.SimpleDateFormat;
import java.util.Date;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.server.WeatherSensorImpl;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Integer;
import us.mn.state.dot.tms.units.Distance;
import static us.mn.state.dot.tms.units.Distance.Units.DECIMETERS;
import static us.mn.state.dot.tms.units.Distance.Units.METERS;
import us.mn.state.dot.tms.units.Temperature;
import static us.mn.state.dot.tms.units.Temperature.Units.CELSIUS;

/**
 * A collection of weather condition values with functionality
 * to convert from NTCIP 1204 units to IRIS units.
 *
 * @author Michael Darter
 * @author Douglas Lau
 */
public class EssRec {

	/** Precipitation of 65535 indicates error or missing value */
	static private final int PRECIP_INVALID_MISSING = 65535;

	/** Convert precipitation rate to mm/hr.
	 * @param pr precipitation rate in 1/10s of gram per square meter per
	 *           second.
	 * @return Precipiration rate in mm/hr or null if missing */
	static private Integer convertPrecipRate(ASN1Integer pr) {
		if (pr != null) {
			// 1mm of water over 1 sqm is 1L which is 1Kg
			int tg = pr.getInteger();
			if (tg != PRECIP_INVALID_MISSING) {
				int mmhr = (int) Math.round((double) tg * 0.36);
				return new Integer(mmhr);
			}
		}
		return null;
	}

	/** Convert one hour precipitation amount.
	 * @param pr One hour precipitation in tenths of a mm.
	 * @return One hour precipitation in mm or null */
	static private Integer convertPrecip(ASN1Integer pr) {
		if (pr != null) {
			int pri = pr.getInteger();
			if (pri != PRECIP_INVALID_MISSING) {
				int cp = (int) Math.round((double) pri * 0.1);
				return new Integer(cp);
			}
		}
		return null;
	}

	/** Convert humidity to an integer.
	 * @param rhu Relative humidity in percent. A value of 101 indicates an
	 *            error or missing value.
	 * @return Humidity as a percent or null if missing. */
	static private Integer convertHumidity(ASN1Integer rhu) {
		if (rhu != null) {
			int irhu = rhu.getInteger();
			if (irhu >= 0 && irhu <= 100)
				return new Integer(irhu);
		}
		return null;
	}

	/** Convert atmospheric pressure to pascals.
	 * @param apr Atmospheric pressure in 1/10ths of millibars, with
	 *            65535 indicating an error or missing value.
	 * @return Pressure in pascals */
	static private Integer convertAtmosphericPressure(ASN1Integer apr) {
		if (apr != null) {
			int tmb = apr.getInteger();
			if (tmb != 65535) {
				double mb = (double) tmb * 0.1;
				double pa = mb * 100;
				return new Integer((int) Math.round(pa));
			}
		}
		return null;
	}

	/** Visibility of 1000001 indicates error or missing value */
	static private final int VISIBILITY_ERROR_MISSING = 1000001;

	/** Convert visibility to Distance.
	 * @param vis Visibility in decimeters with 1000001 indicating an error
	 *            or missing value.
	 * @return Visibility distance or null for missing */
	static private Distance convertVisibility(ASN1Integer vis) {
		if (vis != null) {
			int iv = vis.getInteger();
			if (iv != VISIBILITY_ERROR_MISSING)
				return new Distance(iv, DECIMETERS);
		}
		return null;
	}

	/** Creation time */
	private final long create_time;

	/** Weather sensor */
	private final WeatherSensorImpl w_sensor;

	/** Wind sensor values */
	public final WindSensorValues wind_values = new WindSensorValues();

	/** Table of temperature sensor data read from the controller */
	public final TemperatureSensorsTable ts_table =
		new TemperatureSensorsTable();

	/** Relative humidity (%)  */
	private Integer rel_humidity = null;

	/** Precipitation rate in mm/hr */
	private Integer precip_rate = null;

	/** Precipitation situation */
	private EssPrecipSituation precip_situation = null;

	/** Precipitation 1h */
	private Integer precip_one_hour = null;

	/** Air pressure in Pascals  */
	private Integer air_pressure = null;

	/** Visibility */
	private Distance visibility = null;

	/** Pavement surface temperature */
	private Temperature pvmt_surf_temp = null;

	/** Surface temperature */
	private Temperature surf_temp = null;

	/** Pavement surface status */
	private EssSurfaceStatus pvmt_surf_status = null;

	/** Pavement surface temperature */
	private Temperature surf_freeze_temp = null;

	/** Subsurface temperature */
	private Temperature subsurf_temp = null;

	/** Create a new ESS record */
	public EssRec(WeatherSensorImpl ws) {
		w_sensor = ws;
		create_time = TimeSteward.currentTimeMillis();
	}

	/** Get the dew point temp */
	private Temperature getDewPointTemp() {
		return ts_table.dew_point_temp.getTemperature();
	}

	/** Get the max temp */
	private Temperature getMaxTemp() {
		return ts_table.max_air_temp.getTemperature();
	}

	/** Get the min temp */
	private Temperature getMinTemp() {
		return ts_table.min_air_temp.getTemperature();
	}

	/** Store the wind sensor samples */
	private void storeWinds() {
		w_sensor.setWindDirNotify(wind_values.getAvgWindDir());
		w_sensor.setWindSpeedNotify(wind_values.getAvgWindSpeedKPH());
		w_sensor.setSpotWindDirNotify(wind_values.getSpotWindDir());
		w_sensor.setSpotWindSpeedNotify(wind_values
			.getSpotWindSpeedKPH());
		w_sensor.setMaxWindGustDirNotify(wind_values.getGustWindDir());
		w_sensor.setMaxWindGustSpeedNotify(wind_values
			.getGustWindSpeedKPH());
	}

	/** Store the temperatures */
	private void storeTemps() {
		Temperature dpt = getDewPointTemp();
		w_sensor.setDewPointTempNotify((dpt != null) ?
			dpt.round(CELSIUS) : null);
		Temperature mxt = getMaxTemp();
		w_sensor.setMaxTempNotify((mxt != null) ?
			mxt.round(CELSIUS) : null);
		Temperature mnt = getMinTemp();
		w_sensor.setMinTempNotify((mnt != null) ?
			mnt.round(CELSIUS) : null);
		// Air temperature is assumed to be the first sensor
		// in the table.  Additional sensors are ignored.
		Temperature t = ts_table.getAirTemp(1);
		w_sensor.setAirTempNotify((t != null) ?
			t.round(CELSIUS) : null);
	}

	/** Store humidity */
	public void storeHumidity(ASN1Integer rhu) {
		rel_humidity = convertHumidity(rhu);
		w_sensor.setHumidityNotify(rel_humidity);
	}

	/** Store the precipitation rate */
	public void storePrecipRate(ASN1Integer prr) {
		precip_rate = convertPrecipRate(prr);
		w_sensor.setPrecipRateNotify(precip_rate);
	}

	/** Store the precipitation stuation */
	public void storePrecipSituation(EssPrecipSituation ps) {
		precip_situation = ps;
		w_sensor.setPrecipSituationNotify((ps != null)
			? ps.ordinal()
		        : null);
	}

	/** Store the precipitation 1h */
	public void storePrecipOneHour(ASN1Integer pr) {
		precip_one_hour = convertPrecip(pr);
		w_sensor.setPrecipOneHourNotify(precip_one_hour);
	}

	/** Store the atmospheric pressure */
	public void storeAtmosphericPressure(ASN1Integer apr) {
		air_pressure = convertAtmosphericPressure(apr);
		w_sensor.setPressureNotify(air_pressure);
	}

	/** Store visibility */
	public void storeVisibility(ASN1Integer vis) {
		visibility = convertVisibility(vis);
		w_sensor.setVisibilityNotify(visibility.round(METERS));
	}

	/** Store all sample values */
	public void store() {
		storeWinds();
		storeTemps();
		long storage_time = TimeSteward.currentTimeMillis();
		w_sensor.setStampNotify(storage_time);
	}

	/** Store pavement sensor related values.
	 * @param pst Pavement sensor table, which might contain observations
	 *            from multiple sensors.  Only the first sensor is used. */
	public void store(PavementSensorsTable pst) {
		// Even if no table rows present, set values
		// Ignore rows > 1
		final int row = 1;
		pvmt_surf_temp = pst.getSurfTemp(row);
		w_sensor.setPvmtSurfTempNotify((pvmt_surf_temp != null) ?
			pvmt_surf_temp.round(CELSIUS) : null);

		surf_temp = pst.getSurfTemp(row);
		w_sensor.setSurfTempNotify((surf_temp != null) ?
			surf_temp.round(CELSIUS) : null);

		pvmt_surf_status = pst.getPvmtSurfStatus(row);
		w_sensor.setPvmtSurfStatusNotify((pvmt_surf_status != null)
			? pvmt_surf_status.ordinal()
			: EssSurfaceStatus.undefined.ordinal());

		surf_freeze_temp = pst.getSurfFreezeTemp(row);
		w_sensor.setSurfFreezeTempNotify((surf_freeze_temp != null) ?
			surf_freeze_temp.round(CELSIUS) : null);
	}

	/** Store subsurface sensor related values.
	 * @param sst Subsurface sensor table, which might contain observations
	 *            from multiple sensors. Only the first sensor is used. */
	public void store(SubSurfaceSensorsTable sst) {
		// Even if no table rows present, set values
		// Ignore rows > 1
		final int row = 1;
		subsurf_temp = sst.getTemp(row);
		w_sensor.setSubSurfTempNotify((subsurf_temp != null) ?
			subsurf_temp.round(CELSIUS) : null);
	}

	/** To string */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(EssRec:");
		sb.append(" w_sensor_name=").append(w_sensor.getName());
		sb.append(" create_time=").append(new Date(create_time));
		sb.append(" air_temp_c=").append(ts_table.getAirTemp(1));
		sb.append(" dew_point_temp_c=").append(getDewPointTemp());
		sb.append(" max_temp_c=").append(getMaxTemp());
		sb.append(" min_temp_c=").append(getMinTemp());
		sb.append(" rel_humidity_perc=").append(rel_humidity);
		sb.append(" wind_speed_avg_mph=").append(wind_values
			.getAvgWindSpeedMPH());
		sb.append(" wind_dir_avg_degs=").append(
			wind_values.getAvgWindDir());
		sb.append(" max_wind_gust_speed_mph=").append(wind_values
			.getGustWindSpeedMPH());
		sb.append(" max_wind_gust_dir_degs=").append(wind_values
			.getGustWindDir());
		sb.append(" spot_wind_speed_mph=").append(wind_values
			.getSpotWindSpeedMPH());
		sb.append(" spot_wind_dir_degs=").append(wind_values
			.getSpotWindDir());
		sb.append(" air_pressure_pa=").append(air_pressure);
		sb.append(" precip_rate_mmhr=").append(precip_rate);
		sb.append(" precip_situation=").append(precip_situation);
		sb.append(" precip_1h=").append(precip_one_hour);
		sb.append(" visibility_m=").append(visibility);
		sb.append(" pvmt_surf_temp_c=").append(pvmt_surf_temp);
		sb.append(" surf_temp_c=").append(surf_temp);
		sb.append(" pvmt_surf_status=").append(pvmt_surf_status);
		sb.append(" pvmt_surf_freeze_temp=").append(surf_freeze_temp);
		sb.append(" subsurf_temp=").append(subsurf_temp);
		sb.append(")");
		return sb.toString();
	}
}
