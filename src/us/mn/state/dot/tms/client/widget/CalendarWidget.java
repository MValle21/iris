/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.JPanel;
import javax.swing.JFrame;

/**
 * ZTable is a simple JTable extension which adds a setVisibleRowCount method
 *
 * @author Douglas Lau
 */
public class CalendarWidget extends JPanel {

	/** Calendar highlighter */
	static public interface Highlighter {
		boolean isHighlighted(Calendar cal);
	}

	/** Color to draw outlines of date boxes */
	static protected final Color OUTLINE = new Color(0, 0, 0, 32);

	/** Color to fill holiday boxes */
	static protected final Color COL_HOLIDAY = new Color(128, 208, 32, 128);

	/** Formatter for weekday labels */
	static protected final SimpleDateFormat WEEK_DAY =
		new SimpleDateFormat("EEE");

	/** Calendar for selected month */
	protected final Calendar month = Calendar.getInstance();

	/** Calendar highlighter */
	protected Highlighter highlighter = new Highlighter() {
		public boolean isHighlighted(Calendar cal) {
			return false;
		}
	};

	/** Set the calendar highlighter */
	public void setHighlighter(Highlighter h) {
		highlighter = h;
	}

	/** Create a new calendar widget */
	public CalendarWidget() {
		month.set(Calendar.DAY_OF_MONTH, 1);
		month.set(Calendar.HOUR_OF_DAY, 6);
	}

	/** Set the month to display on the calendar widget */
	public void setMonth(Calendar c) {
		month.setTimeInMillis(c.getTimeInMillis());
		month.set(Calendar.DAY_OF_MONTH, 1);
		month.set(Calendar.HOUR_OF_DAY, 6);
	}

	/** Get the preferred size of the calendar widget */
	public Dimension getPreferredSize() {
		return new Dimension(300, 200);
	}

	/** Paint the calendar widget */
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Dimension size = getSize();
		int hgap = size.width / 7;
		int vgap = size.height / 7;
		int points = Math.min(4 * vgap / 5, 2 * hgap / 5);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, points));
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(month.getTimeInMillis());
		g.setColor(COL_HOLIDAY);
		int wday = month.get(Calendar.DAY_OF_WEEK) -
			month.getFirstDayOfWeek();
		Calendar wcal = Calendar.getInstance();
		wcal.set(Calendar.DAY_OF_WEEK, wcal.getFirstDayOfWeek());
		for(int box = 0; box < 7; box++) {
			int v = box % 7;
			int h = box / 7;
			int x = v * hgap;
			int y = h * vgap;
			g.setColor(Color.GRAY);
			drawText(g2, WEEK_DAY.format(wcal.getTime()),
				x + hgap / 2, y + vgap / 2);
			wcal.add(Calendar.DATE, 1);
		}
		Calendar tcal = Calendar.getInstance();
		while(cal.get(Calendar.MONTH) == month.get(Calendar.MONTH)) {
			int day = cal.get(Calendar.DAY_OF_MONTH);
			int box = day + wday + 6;
			int v = box % 7;
			int h = box / 7;
			int x = v * hgap;
			int y = h * vgap;
			g.setColor(OUTLINE);
			g.drawRect(x + 1, y + 1, hgap - 2, vgap - 2);
			tcal.setTimeInMillis(cal.getTimeInMillis());
			int half = hgap / 2;
			if(highlighter.isHighlighted(tcal)) {
				g.setColor(COL_HOLIDAY);
				g.fillRect(x + 1, y + 1, half - 1, vgap - 2);
			}
			tcal.set(Calendar.AM_PM, Calendar.PM);
			if(highlighter.isHighlighted(tcal)) {
				g.setColor(COL_HOLIDAY);
				g.fillRect(x + half, y + 1, half - 1, vgap - 2);
			}
			g.setColor(Color.BLACK);
			drawText(g2, String.valueOf(day), x + hgap / 2,
				y + vgap / 2);
			cal.add(Calendar.DATE, 1);
		}
	}

	/** Draw text centered at a given point */
	protected void drawText(Graphics2D g2, String text, int x, int y) {
		Font font = g2.getFont();
		GlyphVector gv = font.createGlyphVector(
			g2.getFontRenderContext(), text);
		Rectangle2D rect = gv.getVisualBounds();
		int tx = (int)Math.round(rect.getWidth() / 2.0);
		int ty = (int)Math.round(rect.getHeight() / 2.0);
		g2.drawGlyphVector(gv, x - tx, y + ty);
	}
}
