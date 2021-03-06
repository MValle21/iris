/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group
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
package us.mn.state.dot.tms.client.notification;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import us.mn.state.dot.tms.Notification;
import us.mn.state.dot.tms.NotificationHelper;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyColumn;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyTableModel;

/**
 * Table model for push notifications indicating something requiring user
 * interaction. Note that none of these fields are editable.
 *
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class NotificationModel extends ProxyTableModel<Notification> {

	/** Create a proxy descriptor */
	static public ProxyDescriptor<Notification> descriptor(Session s) {
		return new ProxyDescriptor<Notification>(
				s.getSonarState().getNotificationCache(),
				false, false, false);
	}

	/** Create the columns in the model */
	@Override
	protected ArrayList<ProxyColumn<Notification>> createColumns() {
		ArrayList<ProxyColumn<Notification>> cols =
				new ArrayList<ProxyColumn<Notification>>();
		cols.add(new ProxyColumn<Notification>("notification.title", 150) {
			@Override
			public Object getValueAt(Notification pn) {
				return pn.getTitle();
			}
			@Override
			protected TableCellRenderer createCellRenderer() {
				return new ValueCellRenderer();
			}
		});
		cols.add(new ProxyColumn<Notification>(
				"notification.description", 400) {
			@Override
			public Object getValueAt(Notification pn) {
				return pn.getDescription();
			}
			@Override
			protected TableCellRenderer createCellRenderer() {
				return new ValueCellRenderer();
			}
		});
		cols.add(new ProxyColumn<Notification>("notification.sent", 150) {
			@Override
			public Object getValueAt(Notification pn) {
				// show the time since the notification was sent
				Date sentTime = pn.getSentTime();
				if (sentTime != null)
					return NotificationHelper.getDurationString(sentTime);
				return "";
			}
		});
		cols.add(new ProxyColumn<Notification>(
				"notification.addressed_time", 150) {
			@Override
			public Object getValueAt(Notification pn) {
				// show the time since the notification was sent
				Date addrTime = pn.getAddressedTime();
				if (addrTime != null)
					return NotificationHelper.getDurationString(addrTime);
				return "";
			}
		});
		cols.add(new ProxyColumn<Notification>(
				"notification.addressed_by", 100) {
			@Override
			public Object getValueAt(Notification pn) {
				return pn.getAddressedBy();
			}
		});
		// NOTE we don't show the object type/name (assume it is indicated
		// in the title/description if needed)
		return cols;
	}

	protected class ValueCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus,
			int row, int column)
		{
			JLabel label = (JLabel)
				super.getTableCellRendererComponent(table,
				value, isSelected, hasFocus, row, column);
			String txt = label.getText();
			label.setText("<html>" + txt + "</html>");
			label.setVerticalAlignment(JLabel.TOP);
			return label;
		}
	}

	/** Check if this notification should be included in the list.
	 *  Notifications are only included if the user can see them (based on
	 *  whether they can read or write the object type referenced in the
	 *  notification, depending on the state of needs_write) and if they
	 *  either haven't been addressed yet or were addressed recently
	 *  (determined by the NOTIFICATION_TIMEOUT_SECS system attribute).
	 */
	@Override
	protected boolean check(Notification pn) {
		return NotificationHelper.check(session, pn, true);
	}

	public NotificationModel(Session s) {
		super(s, descriptor(s), 12, 36);
	}
}
