/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017-2018  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.monstream;

import java.io.IOException;
import java.nio.ByteBuffer;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.CameraHelper;
import us.mn.state.dot.tms.ControllerIO;
import us.mn.state.dot.tms.PlayList;
import us.mn.state.dot.tms.PlayListHelper;
import us.mn.state.dot.tms.VideoMonitor;
import us.mn.state.dot.tms.VideoMonitorHelper;
import us.mn.state.dot.tms.server.CameraImpl;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.VideoMonitorImpl;
import us.mn.state.dot.tms.server.comm.Operation;
import us.mn.state.dot.tms.server.comm.OpStep;
import us.mn.state.dot.tms.server.comm.ParsingException;

/**
 * MonStream operation to receive monitor status.
 *
 * @author Douglas Lau
 */
public class OpStatus extends OpStep {

	/** ASCII record separator */
	static private final String RECORD_SEP =
		String.valueOf(MonProp.RECORD_SEP);

	/** ASCII unit separator */
	static private final String UNIT_SEP =
		String.valueOf(MonProp.UNIT_SEP);

	/** Buffer to parse received data */
	private final byte[] buf = new byte[2048];

	/** Display property */
	private DisplayProp display;

	/** Create a new status op */
	public OpStatus() {
		setPolling(false);
	}

	/** Set polling */
	@Override
	public void setPolling(boolean p) {
		// No polling -- just wait for status messages
		super.setPolling(false);
	}

	/** Poll the controller */
	@Override
	public void poll(Operation op, ByteBuffer tx_buf) throws IOException {
		DisplayProp dp = display;
		if (dp != null) {
			dp.encodeStore(op, tx_buf);
			display = null;
		}
	}

	/** Parse data received from controller */
	@Override
	public void recv(Operation op, ByteBuffer rx_buf) throws IOException {
		doRecv(op, rx_buf);
	}

	/** Parse received data */
	private void doRecv(Operation op, ByteBuffer rx_buf) throws IOException{
		ControllerImpl ctrl = op.getController();
		int len = Math.min(rx_buf.remaining(), buf.length);
		rx_buf.get(buf, 0, len);
		doRecv(ctrl, new String(buf, 0, len, "UTF8"));
	}

	/** Parse received messages */
	private void doRecv(ControllerImpl ctrl, String msgs)throws IOException{
		for (String msg : msgs.split(RECORD_SEP))
			parseMsg(ctrl, msg);
	}

	/** Parse one received message */
	private void parseMsg(ControllerImpl ctrl, String msg)
		throws IOException
	{
		String[] par = msg.split(UNIT_SEP);
		String cod = (par.length > 0) ? par[0] : "";
		if (cod.length() > 0) {
			switch (cod) {
			case "status":
				parseStatus(ctrl, par);
				break;
			case "query":
				parseQuery(par);
				break;
			case "switch":
				parseSwitch(ctrl, par);
				break;
			case "next":
				parseNext(ctrl, par);
				break;
			case "previous":
				parsePrevious(ctrl, par);
				break;
			case "sequence":
				parseSequence(ctrl, par);
				break;
			default:
				throw new ParsingException("INVALID MSG");
			}
		}
	}

	/** Parse status message */
	private void parseStatus(ControllerImpl ctrl, String[] par)
		throws IOException
	{
		String mon = (par.length > 1) ? par[1] : "";
		String cam = (par.length > 2) ? par[2] : "";
		String stat = (par.length > 3) ? par[3] : "";
		parseStatus(ctrl, mon, cam, stat);
	}

	/** Parse status message */
	private void parseStatus(ControllerImpl ctrl, String mon, String cam,
		String stat) throws IOException
	{
		try {
			int pin = Integer.parseInt(mon) + 1;
			ControllerIO cio = ctrl.getIO(pin);
			if (cio instanceof VideoMonitorImpl)
				parseStatus((VideoMonitorImpl) cio, cam, stat);
			else
				throw new ParsingException("INVALID PIN: "+pin);
		}
		catch (NumberFormatException e) {
			throw new ParsingException("INVALID MON NUM: " + mon);
		}
	}

	/** Parse video monitor status */
	private void parseStatus(VideoMonitorImpl vm, String cam, String stat)
		throws IOException
	{
		Camera c = CameraHelper.find(cam);
		if (c instanceof CameraImpl)
			parseStatus(vm, (CameraImpl) c, stat);
		else
			throw new ParsingException("INVALID CAM: " + cam);
	}

	/** Parse video monitor status */
	private void parseStatus(VideoMonitorImpl vm, CameraImpl c, String stat)
		throws IOException
	{
		vm.setCameraNotify(c, "MONSTREAM", false);
		c.setVideoLossNotify(stat.length() > 0);
	}

	/** Parse query message */
	private void parseQuery(String[] par) throws IOException {
		String mon = (par.length > 1) ? par[1] : "";
		parseQuery(mon);
	}

	/** Parse query message */
	private void parseQuery(String mon) throws IOException {
		VideoMonitor vm = VideoMonitorHelper.findUID(mon);
		if (vm instanceof VideoMonitorImpl)
			parseQuery((VideoMonitorImpl) vm);
		else
			throw new ParsingException("INVALID MON: " + mon);
	}

	/** Parse query message */
	private void parseQuery(VideoMonitorImpl vm) throws IOException {
		display = new DisplayProp(vm);
		super.setPolling(true);
	}

	/** Parse switch message */
	private void parseSwitch(ControllerImpl ctrl, String[] par)
		throws IOException
	{
		String mon = (par.length > 1) ? par[1] : "";
		String cam = (par.length > 2) ? par[2] : "";
		parseSwitch(ctrl, mon, cam);
	}

	/** Parse switch message */
	private void parseSwitch(ControllerImpl ctrl, String mon, String cam)
		throws IOException
	{
		VideoMonitor vm = VideoMonitorHelper.findUID(mon);
		if (vm instanceof VideoMonitorImpl)
			parseSwitch(ctrl, (VideoMonitorImpl) vm, cam);
		else
			throw new ParsingException("INVALID MON: " + mon);
	}

	/** Parse switch message */
	private void parseSwitch(ControllerImpl ctrl, VideoMonitorImpl vm,
		String cam) throws IOException
	{
		Camera c = CameraHelper.find(cam);
		if (c instanceof CameraImpl)
			selectCamera(ctrl, vm, (CameraImpl) c);
		else
			throw new ParsingException("INVALID CAM: " + cam);
	}

	/** Select a camera on the selected video monitor */
	private void selectCamera(ControllerImpl ctrl, VideoMonitorImpl vm,
		CameraImpl c)
	{
		int mn = vm.getMonNum();
		// FIXME: only needed if we're controlling camera
		c.sendPTZ(0, 0, 0);
		VideoMonitorImpl.setCameraNotify(mn, c, "SEL " + ctrl);
	}

	/** Parse next message */
	private void parseNext(ControllerImpl ctrl, String[] par)
		throws IOException
	{
		String mon = (par.length > 1) ? par[1] : "";
		parseNext(ctrl, mon);
	}

	/** Parse next message */
	private void parseNext(ControllerImpl ctrl, String mon)
		throws IOException
	{
		VideoMonitor vm = VideoMonitorHelper.findUID(mon);
		if (vm instanceof VideoMonitorImpl)
			selectNext(ctrl, (VideoMonitorImpl) vm);
		else
			throw new ParsingException("INVALID MON: " + mon);
	}

	/** Select next camera on the selected video monitor */
	private void selectNext(ControllerImpl ctrl, VideoMonitorImpl vm) {
		Camera c = vm.getCamera();
		if (c != null) {
			Integer cn = c.getCamNum();
			if (cn != null)
				selectNext(ctrl, vm, cn);
		}
	}

	/** Select next camera on the selected video monitor */
	private void selectNext(ControllerImpl ctrl, VideoMonitorImpl vm,
		int cn)
	{
		int mn = vm.getMonNum();
		Camera c = CameraHelper.findNextOrFirst(cn);
		if (c instanceof CameraImpl) {
			VideoMonitorImpl.setCameraNotify(mn, (CameraImpl) c,
				"NEXT " + ctrl);
		}
	}

	/** Parse previous message */
	private void parsePrevious(ControllerImpl ctrl, String[] par)
		throws IOException
	{
		String mon = (par.length > 1) ? par[1] : "";
		parsePrevious(ctrl, mon);
	}

	/** Parse previous message */
	private void parsePrevious(ControllerImpl ctrl, String mon)
		throws IOException
	{
		VideoMonitor vm = VideoMonitorHelper.findUID(mon);
		if (vm instanceof VideoMonitorImpl)
			selectPrevious(ctrl, (VideoMonitorImpl) vm);
		else
			throw new ParsingException("INVALID MON: " + mon);
	}

	/** Select previous camera on the selected video monitor */
	private void selectPrevious(ControllerImpl ctrl, VideoMonitorImpl vm) {
		Camera c = vm.getCamera();
		if (c != null) {
			Integer cn = c.getCamNum();
			if (cn != null)
				selectPrevious(ctrl, vm, cn);
		}
	}

	/** Select previous camera on the selected video monitor */
	private void selectPrevious(ControllerImpl ctrl, VideoMonitorImpl vm,
		int cn)
	{
		int mn = vm.getMonNum();
		Camera c = CameraHelper.findPrevOrLast(cn);
		if (c instanceof CameraImpl) {
			VideoMonitorImpl.setCameraNotify(mn, (CameraImpl) c,
				"PREV " + ctrl);
		}
	}

	/** Parse sequence message */
	private void parseSequence(ControllerImpl ctrl, String[] par)
		throws IOException
	{
		String mon = (par.length > 1) ? par[1] : "";
		String seq = (par.length > 2) ? par[2] : "";
		parseSequence(ctrl, mon, seq);
	}

	/** Parse sequence message */
	private void parseSequence(ControllerImpl ctrl, String mon, String seq)
		throws IOException
	{
		VideoMonitor vm = VideoMonitorHelper.findUID(mon);
		if (vm instanceof VideoMonitorImpl)
			parseSequence(ctrl, (VideoMonitorImpl) vm, seq);
		else
			throw new ParsingException("INVALID MON: " + mon);
	}

	/** Parse sequence message */
	private void parseSequence(ControllerImpl ctrl, VideoMonitorImpl vm,
		String seq) throws IOException
	{
		PlayList pl = PlayListHelper.findNum(seq);
		if (pl != null)
			vm.setPlayList(pl);
		else
			throw new ParsingException("INVALID SEQ: " + seq);
	}

	/** Get the next step */
	@Override
	public OpStep next() {
		return this;
	}
}
