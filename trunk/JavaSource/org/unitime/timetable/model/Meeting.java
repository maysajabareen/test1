/* 
 * UniTime 3.1 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2008, UniTime.org
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
 
package org.unitime.timetable.model;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.unitime.timetable.model.base.BaseMeeting;
import org.unitime.timetable.model.dao.MeetingDAO;
import org.unitime.timetable.model.dao.RoomDAO;



public class Meeting extends BaseMeeting implements Comparable<Meeting> {
	private static final long serialVersionUID = 1L;
	private Location location = null;
	
/*[CONSTRUCTOR MARKER BEGIN]*/
	public Meeting () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Meeting (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public Meeting (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.Event event,
		org.unitime.timetable.model.EventType eventType,
		java.util.Date meetingDate,
		java.lang.Integer startPeriod,
		java.lang.Integer stopPeriod,
		java.lang.Boolean classCanOverride) {

		super (
			uniqueId,
			event,
			eventType,
			meetingDate,
			startPeriod,
			stopPeriod,
			classCanOverride);
	}

/*[CONSTRUCTOR MARKER END]*/

	@Override
	public Object clone()  {
		Meeting newMeeting = new Meeting();
		newMeeting.setClassCanOverride(isClassCanOverride());
		newMeeting.setEventType(getEventType());
		newMeeting.setLocationPermanentId(getLocationPermanentId());
		newMeeting.setMeetingDate(getMeetingDate());
		newMeeting.setStartOffset(getStartOffset());
		newMeeting.setStartPeriod(getStartPeriod());
		newMeeting.setStopOffset(getStopOffset());
		newMeeting.setStopPeriod(getStopPeriod());
		
		return(newMeeting);
	}

	public int compareTo(Meeting other) {
		int cmp = getMeetingDate().compareTo(other.getMeetingDate());
		if (cmp!=0) return cmp;
		cmp = getStartPeriod().compareTo(other.getStartPeriod());
		if (cmp!=0) return cmp;
		return getUniqueId().compareTo(other.getUniqueId());
	}

	public Location getLocation(){
		if (location != null){
			return(location);
		}
		if(getLocationPermanentId() == null){
			return(null);
		}
		if (getMeetingDate() == null){
			return(null);
		}
		Calendar mtgDt = Calendar.getInstance();
		mtgDt.setTime(getMeetingDate());
		List<?> locations = (RoomDAO.getInstance()).getSession().createQuery("from Room as r where r.permanentId = :permId").setLong("permId", getLocationPermanentId().longValue()).list();
		if (locations != null && !locations.isEmpty()){
			for(Iterator<?> locIt = locations.iterator(); locIt.hasNext(); ){
				Room r = (Room) locIt.next();
				Calendar sessStart = Calendar.getInstance();
				sessStart.setTime(r.getSession().getSessionBeginDateTime());
				Calendar sessStop = Calendar.getInstance();
				sessStop.setTime(r.getSession().getSessionEndDateTime());
				if (mtgDt.compareTo(sessStart) >= 0 && mtgDt.compareTo(sessStop) <= 0){
					location = r;
				}
			}
			for(Iterator<?> locIt = locations.iterator(); locIt.hasNext(); ){
				Room r = (Room) locIt.next();
				Calendar sessStart = Calendar.getInstance();
				sessStart.setTime(r.getSession().getSessionBeginDateTime());
				sessStart.add(Calendar.DAY_OF_MONTH, -30);
				Calendar sessStop = Calendar.getInstance();
				sessStop.setTime(r.getSession().getSessionEndDateTime());
				if (mtgDt.compareTo(sessStart) >= 0 && mtgDt.compareTo(sessStop) <= 0){
					location = r;
				}
			}
			
		} else {
			locations = (RoomDAO.getInstance()).getSession().createQuery("from NonUniversityLocation as nul where nul.permanentId = :permId").setLong("permId", getLocationPermanentId().longValue()).list();
			for(Iterator<?> locIt = locations.iterator(); locIt.hasNext(); ){
				NonUniversityLocation nul = (NonUniversityLocation) locIt.next();
				Calendar sessStart = Calendar.getInstance();
				sessStart.setTime(nul.getSession().getSessionBeginDateTime());
				Calendar sessStop = Calendar.getInstance();
				sessStop.setTime(nul.getSession().getSessionEndDateTime());
				if (mtgDt.compareTo(sessStart) >= 0 && mtgDt.compareTo(sessStop) <= 0){
					location = nul;
				}
			}
			for(Iterator<?> locIt = locations.iterator(); locIt.hasNext(); ){
				NonUniversityLocation nul = (NonUniversityLocation) locIt.next();
				Calendar sessStart = Calendar.getInstance();
				sessStart.setTime(nul.getSession().getSessionBeginDateTime());
				sessStart.add(Calendar.DAY_OF_MONTH, -30);
				Calendar sessStop = Calendar.getInstance();
				sessStop.setTime(nul.getSession().getSessionEndDateTime());
				if (mtgDt.compareTo(sessStart) >= 0 && mtgDt.compareTo(sessStop) <= 0){
					location = nul;
				}
			}
		}
		return(location);
	}
	
	public List<?> getTimeRoomOverlaps(){
		return (MeetingDAO.getInstance()).getSession().createQuery("from Meeting m where m.meetingDate=:meetingDate and m.startPeriod <= :stopPeriod and m.stopPeriod >= :startPeriod and m.locationPermanentId = :locPermId and m.uniqueId != :uniqueId")
		.setDate("meetingDate", getMeetingDate())
		.setInteger("stopPeriod", getStopPeriod())
		.setInteger("startPeriod", getStartPeriod())
		.setLong("locPermId", getLocationPermanentId())
		.setLong("uniqueId", this.getUniqueId())
		.list();
	}

	public boolean hasTimeRoomOverlaps(){
		Long count = (Long)MeetingDAO.getInstance().getSession().createQuery("select count(m) from Meeting m where m.meetingDate=:meetingDate and m.startPeriod <= :stopPeriod and m.stopPeriod >= :startPeriod and m.locationPermanentId = :locPermId and m.uniqueId != :uniqueId")
		.setDate("meetingDate", getMeetingDate())
		.setInteger("stopPeriod", getStopPeriod())
		.setInteger("startPeriod", getStartPeriod())
		.setLong("locPermId", getLocationPermanentId())
		.setLong("uniqueId", this.getUniqueId())
		.uniqueResult();
		return(count > 0);
	}

}