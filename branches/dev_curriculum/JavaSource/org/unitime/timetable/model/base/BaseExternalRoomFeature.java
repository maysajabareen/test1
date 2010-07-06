/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC, and individual contributors
 * as indicated by the @authors tag.
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
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package org.unitime.timetable.model.base;

import java.io.Serializable;

import org.unitime.timetable.model.ExternalRoom;
import org.unitime.timetable.model.ExternalRoomFeature;

public abstract class BaseExternalRoomFeature implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;
	private String iName;
	private String iValue;

	private ExternalRoom iRoom;

	public static String PROP_UNIQUEID = "uniqueId";
	public static String PROP_NAME = "name";
	public static String PROP_VALUE = "value";

	public BaseExternalRoomFeature() {
		initialize();
	}

	public BaseExternalRoomFeature(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public String getName() { return iName; }
	public void setName(String name) { iName = name; }

	public String getValue() { return iValue; }
	public void setValue(String value) { iValue = value; }

	public ExternalRoom getRoom() { return iRoom; }
	public void setRoom(ExternalRoom room) { iRoom = room; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof ExternalRoomFeature)) return false;
		if (getUniqueId() == null || ((ExternalRoomFeature)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((ExternalRoomFeature)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "ExternalRoomFeature["+getUniqueId()+" "+getName()+"]";
	}

	public String toDebugString() {
		return "ExternalRoomFeature[" +
			"\n	Name: " + getName() +
			"\n	Room: " + getRoom() +
			"\n	UniqueId: " + getUniqueId() +
			"\n	Value: " + getValue() +
			"]";
	}
}
