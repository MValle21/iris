/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014  AHMCT, University of California
 * Copyright (C) 2016-2017  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.cohuptz;

import us.mn.state.dot.tms.DeviceRequest;
import us.mn.state.dot.tms.server.CameraImpl;
import us.mn.state.dot.tms.server.comm.BasePoller;
import us.mn.state.dot.tms.server.comm.CameraPoller;
import us.mn.state.dot.tms.server.comm.Operation;
import us.mn.state.dot.tms.server.comm.OpStep;
import static us.mn.state.dot.tms.server.comm.PriorityLevel.COMMAND;
import static us.mn.state.dot.tms.utils.URIUtil.TCP;

/**
 * Poller for the Cohu PTZ protocol.
 *
 * @author Travis Swanston
 * @author Douglas Lau
 */
public class CohuPTZPoller extends BasePoller implements CameraPoller {

	/** Create a new Cohu PTZ poller */
	public CohuPTZPoller(String n) {
		super(n, TCP);
	}

	/** Create an operation */
	private void createOp(String n, CameraImpl c, OpStep s) {
		Operation op = new Operation(n, c, s);
		op.setPriority(COMMAND);
		addOp(op);
	}

	/** Send a "PTZ camera move" command */
	@Override
	public void sendPTZ(CameraImpl c, float p, float t, float z) {
		createOp("camera.op.send.ptz", c, new OpPTZCamera(p, t, z));
	}

	/** Send a "store camera preset" command */
	@Override
	public void sendStorePreset(CameraImpl c, int preset) {
		createOp("camera.op.store.preset", c,
			new OpStorePreset(preset));
	}

	/** Send a "recall camera preset" command */
	@Override
	public void sendRecallPreset(CameraImpl c, int preset) {
		createOp("camera.op.recall.preset", c,
			new OpRecallPreset(preset));
	}

	/** Send a device request.
	 * @param c The CameraImpl object.
	 * @param dr Device request to send. */
	@Override
	public void sendRequest(CameraImpl c, DeviceRequest dr) {
		switch (dr) {
		case RESET_DEVICE:
		case CAMERA_FOCUS_STOP:
		case CAMERA_FOCUS_NEAR:
		case CAMERA_FOCUS_FAR:
		case CAMERA_FOCUS_MANUAL:
		case CAMERA_FOCUS_AUTO:
		case CAMERA_IRIS_STOP:
		case CAMERA_IRIS_CLOSE:
		case CAMERA_IRIS_OPEN:
		case CAMERA_IRIS_MANUAL:
		case CAMERA_IRIS_AUTO:
			createOp("device.op.request", c, new OpDeviceReq(dr));
			break;
		case CAMERA_WIPER_ONESHOT:
			// FIXME: not yet implemented
			break;
		default:
			break;
		}
	}
}
