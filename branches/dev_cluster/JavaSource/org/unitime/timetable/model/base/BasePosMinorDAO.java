/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC, and individual contributors
 * as indicated by the @authors tag.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
*/
package org.unitime.timetable.model.base;

import java.util.List;

import org.unitime.timetable.model.PosMinor;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.model.dao.PosMinorDAO;

public abstract class BasePosMinorDAO extends _RootDAO<PosMinor,Long> {

	private static PosMinorDAO sInstance;

	public static PosMinorDAO getInstance() {
		if (sInstance == null) sInstance = new PosMinorDAO();
		return sInstance;
	}

	public Class<PosMinor> getReferenceClass() {
		return PosMinor.class;
	}

	@SuppressWarnings("unchecked")
	public List<PosMinor> findBySession(org.hibernate.Session hibSession, Long sessionId) {
		return hibSession.createQuery("from PosMinor x where x.session.uniqueId = :sessionId").setLong("sessionId", sessionId).list();
	}
}
