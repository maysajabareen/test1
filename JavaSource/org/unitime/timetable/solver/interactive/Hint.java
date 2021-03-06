/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.solver.interactive;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.solver.Solver;
import org.dom4j.Element;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.ui.AssignmentPreferenceInfo;


/**
 * @author Tomas Muller
 */
public class Hint implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long iClassId;
	private int iDays;
	private int iStartSlot;
	private List<Long> iRoomIds;
	private Long iPatternId;
	private Long iDatePatternId;
	private AssignmentPreferenceInfo iInfo = null;
	private ClassAssignmentDetails iDetails = null;
	public Hint(Long classId, int days, int startSlot, List<Long> roomIds, Long patternId, Long datePatternId) {
		iClassId = classId; iDays = days; iStartSlot = startSlot; iRoomIds = roomIds; iPatternId = patternId; iDatePatternId = datePatternId;
	}
	public Hint(Solver solver, Placement placement) {
		this(solver, placement, false);
	}
	public Hint(Solver solver, Placement placement, boolean populateInfo) {
		iClassId = ((Lecture)placement.variable()).getClassId();
		iDays = placement.getTimeLocation().getDayCode();
		iStartSlot = placement.getTimeLocation().getStartSlot();
		iRoomIds = placement.getRoomIds();
		iPatternId = placement.getTimeLocation().getTimePatternId();
		iDatePatternId = placement.getTimeLocation().getDatePatternId();
		if (populateInfo && solver!=null)
			iInfo = new AssignmentPreferenceInfo(solver, placement, false);
		if (placement.variable().isCommitted() && solver!=null)
			iDetails = new ClassAssignmentDetails(solver, placement.variable(),placement,false);
	}
	public Placement getPlacement(TimetableModel model) {
		return getPlacement(model, true);
	}
	public Placement getPlacement(TimetableModel model, boolean checkValidity) {
		for (Lecture lecture: model.variables()) {
			if (!lecture.getClassId().equals(iClassId)) continue;
    		TimeLocation timeLocation = null;
        	for (TimeLocation t: lecture.timeLocations()) {
        		if (t.getDayCode()!=iDays) continue;
        		if (t.getStartSlot()!=iStartSlot) continue;
        		if (!t.getTimePatternId().equals(iPatternId)) continue;
        		if (!t.getDatePatternId().equals(iDatePatternId)) continue;
        		timeLocation = t; break;
        	}
        	Vector roomLocations = new Vector();
        	if (lecture.getNrRooms()>0) {
            	for (Long roomId: iRoomIds) {
                	for (RoomLocation r: lecture.roomLocations()) {
                		if (r.getId().equals(roomId))
                			roomLocations.add(r);
                	}
        		}
        	}
    		if (timeLocation!=null && roomLocations.size()==lecture.getNrRooms()) {
    			Placement placement = new Placement(lecture,timeLocation,roomLocations);
    			if (checkValidity && !placement.isValid()) return null;
    			return placement;
    		}
    		/*
			Vector values = lecture.values();
	    	if (!lecture.allowBreakHard()) values = lecture.computeValues(true);
			for (Enumeration e2=values.elements();e2.hasMoreElements();) {
				Placement placement = (Placement)e2.nextElement();
				if (placement.getTimeLocation().getDayCode()!=iDays) continue;
				if (placement.getTimeLocation().getStartSlot()!=iStartSlot) continue;
				boolean sameRooms = true;
				for (Enumeration e3=iRoomIds.elements();sameRooms && e3.hasMoreElements();) {
					Long roomId = (Integer)e3.nextElement();
					if (!placement.hasRoomLocation(roomId)) sameRooms = false;
				}
				if (!sameRooms) continue;
				return placement;
			}
			*/
		}
		return null;
	}
	public AssignmentPreferenceInfo getInfo(Solver solver) {
		Placement p = getPlacement((TimetableModel)solver.currentSolution().getModel());
		if (p==null) return null;
		return new AssignmentPreferenceInfo(solver, p, false);
	}
	
	public String getNotValidReason(Solver solver) {
		Placement p = getPlacement((TimetableModel)solver.currentSolution().getModel(), false);
		if (p==null) return "Selected placement is not valid (room or instructor not avaiable).";
		if (p.isValid()) return "Selected placement is valid.";
		String reason = p.getNotValidReason(solver.currentSolution().getAssignment(), solver.getProperties().getPropertyBoolean("General.UseAmPm", true));
		return (reason==null?"Selected placement is not valid (room or instructor not avaiable).":"Selected placement is not valid ("+reason+")");
	}
	public Long getClassId() { return iClassId; }
	public int getDays() { return iDays; }
	public int getStartSlot() { return iStartSlot; }
	public List<Long> getRoomIds() { return iRoomIds; }
	public Long getPatternId() { return iPatternId; }
	public Long getDatePatternId() { return iDatePatternId; }
	
	public boolean equals(Object o) {
		if (o==null || !(o instanceof Hint)) return false;
		return iClassId.equals(((Hint)o).getClassId());
	}
	public int hashCode() { return iClassId.hashCode(); }
	
	public ClassAssignmentDetails getDetails(SessionContext context, SolverProxy solver, boolean includeConstraints) {
		if (iDetails!=null) return iDetails;
		if (iInfo==null && solver != null) iInfo = solver.getInfo(this);
		iDetails = ClassAssignmentDetails.createClassAssignmentDetails(context, solver, iClassId, includeConstraints);
		if (iDetails!=null) iDetails.setAssigned(iInfo, iRoomIds,iDays,iStartSlot,iPatternId,iDatePatternId);
		return iDetails;
	}
	public ClassAssignmentDetails getDetailsUnassign(SessionContext context, SolverProxy solver, boolean includeConstraints) {
		if (iDetails!=null) return iDetails;
		iDetails = ClassAssignmentDetails.createClassAssignmentDetails(context, solver, iClassId, includeConstraints);
		return iDetails;
	}
	public void setDetails(ClassAssignmentDetails details) {
		iDetails = details;
	}

    public static class HintComparator implements Comparator {
    	private Vector iOrder; 
        public HintComparator(Vector order) { iOrder = order; }
        public int compare(Object o1, Object o2) {
            Hint p1 = (Hint)o1;
            Hint p2 = (Hint)o2;
            int i1 = iOrder.indexOf(p1.getClassId());
            int i2 = iOrder.indexOf(p2.getClassId());
            return (Integer.valueOf(i1)).compareTo(Integer.valueOf(i2));
        }
    }
    
    public void toXml(Element element) {
    	element.addAttribute("id", String.valueOf(iClassId));
    	element.addAttribute("days", String.valueOf(iDays));
    	element.addAttribute("start", String.valueOf(iStartSlot));
    	if (iRoomIds!=null) {
    		for (Long roomId: iRoomIds) {
    			if (roomId!=null)
    				element.addElement("room").addAttribute("id", roomId.toString());
    		}
    	}
    	if (iPatternId!=null)
    		element.addAttribute("pattern", iPatternId.toString());
    	if (iDatePatternId!=null)
    		element.addAttribute("dates", iDatePatternId.toString());
    }
    
    public static Hint fromXml(Element element) {
    	Vector roomIds = new Vector();
    	for (Iterator i=element.elementIterator("room");i.hasNext();)
    		roomIds.addElement(Long.valueOf(((Element)i.next()).attributeValue("id")));
    	return new Hint(
    			Long.valueOf(element.attributeValue("id")),
    			Integer.parseInt(element.attributeValue("days")),
    			Integer.parseInt(element.attributeValue("start")),
    			roomIds,
    			(element.attributeValue("pattern")==null?null:Long.valueOf(element.attributeValue("pattern"))),
    			(element.attributeValue("dates")==null?null:Long.valueOf(element.attributeValue("dates")))
    			);
    }
    
    public String toString() {
        return "Hint{classId = "+iClassId+", days = "+iDays+", startSlot = "+iStartSlot+", roomIds = "+iRoomIds+"}";
    }
}
