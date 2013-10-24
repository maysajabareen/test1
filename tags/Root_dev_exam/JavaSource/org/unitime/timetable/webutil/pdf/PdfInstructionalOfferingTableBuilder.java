/*
 * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2007, UniTime.org, and individual contributors
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
package org.unitime.timetable.webutil.pdf;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.InstructionalOfferingListForm;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.BuildingPref;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.DatePattern;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DistributionPref;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.model.PreferenceGroup;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.RoomFeaturePref;
import org.unitime.timetable.model.RoomGroupPref;
import org.unitime.timetable.model.RoomPref;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.TimePattern;
import org.unitime.timetable.model.TimePref;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.comparators.InstrOfferingConfigComparator;
import org.unitime.timetable.model.comparators.InstructorComparator;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.InstructionalOfferingDAO;
import org.unitime.timetable.solver.CachedClassAssignmentProxy;
import org.unitime.timetable.solver.ClassAssignmentProxy;
import org.unitime.timetable.solver.ui.AssignmentPreferenceInfo;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.PdfEventHandler;
import org.unitime.timetable.webutil.RequiredTimeTable;
import org.unitime.timetable.webutil.WebInstructionalOfferingTableBuilder;

import net.sf.cpsolver.coursett.model.TimeLocation.IntEnumeration;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;


/**
 * @author Tomas Muller
 */
public class PdfInstructionalOfferingTableBuilder extends WebInstructionalOfferingTableBuilder {
	protected PdfWriter iWriter = null;
	protected Document iDocument = null;
	protected PdfPTable iPdfTable = null;
	
	protected static String indent = "    ";
	protected static String LABEL = " ";

    public PdfInstructionalOfferingTableBuilder() {
        super();
    }
    
    protected static Color sBorderColor = Color.BLACK;
    protected static Color sBgColorClass = Color.WHITE;
    protected static Color sBgColorSubpart = new Color(225,225,225);
    protected static Color sBgColorConfig = new Color(200,200,200);
    protected static Color sBgColorOffering = new Color(200,200,200);
    protected static Color sBgColorHeader = Color.WHITE;
    protected Color iBgColor = Color.WHITE;
    
    protected Color sEnableColor = Color.BLACK;
    protected Color sDisableColor = Color.GRAY;
    
	public PdfPCell createCell() {
		PdfPCell cell = new PdfPCell();
		cell.setBorderColor(sBorderColor);
		cell.setPadding(3);
		cell.setBorderWidth(0);
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setBackgroundColor(iBgColor);
		return cell;
	}
	
	public void addText(PdfPCell cell, String text) {
		addText(cell, text, false, false, Element.ALIGN_LEFT, sEnableColor, true);
	}
	
	public void addText(PdfPCell cell, String text, boolean bold, int orientation) {
		addText(cell, text, bold, false, orientation, sEnableColor, true);
	}

	public void addText(PdfPCell cell, String text, int orientation) {
		addText(cell, text, false, false, orientation, sEnableColor, true);
	}
	
	public void addText(PdfPCell cell, String text, boolean bold, boolean italic,  int orientation, Color color, boolean newLine) {
		if (text==null) return;
		if (cell.getPhrase()==null) {
			Chunk ch = new Chunk(text, FontFactory.getFont(FontFactory.HELVETICA, 12, (italic&&bold?Font.BOLDITALIC:italic?Font.ITALIC:bold?Font.BOLD:Font.NORMAL), color));
			cell.setPhrase(new Paragraph(ch));
			cell.setVerticalAlignment(Element.ALIGN_TOP);
			cell.setHorizontalAlignment(orientation);
		} else {
			cell.getPhrase().add(new Chunk((newLine?"\n":"")+text,FontFactory.getFont(FontFactory.HELVETICA, 12, (italic&&bold?Font.BOLDITALIC:italic?Font.ITALIC:bold?Font.BOLD:Font.NORMAL), color)));
		}
	}
	
	public int getNrColumns() {
		int ret = 0;
    	if (isShowLabel()) ret+=1;
    	if (isShowTitle()) ret+=1;
    	if (isShowDivSec()) ret+=1;
    	if (isShowDemand()) ret+=1;
    	if (isShowProjectedDemand()) ret+=1;
    	if (isShowConsent()) ret+=1;
    	if (isShowDesignatorRequired()) ret+=1;
    	if (isShowMinPerWk()) ret+=1;
    	if (isShowLimit()) ret+=1;
    	if (isShowRoomRatio()) ret+=1;
    	if (isShowManager()) ret+=1;
    	if (isShowDatePattern()) ret+=1;
    	if (isShowTimePattern()) ret+=1;
    	if (isShowPreferences()) ret+=PREFERENCE_COLUMN_ORDER.length+(getDisplayDistributionPrefs()?0:-1);
    	if (isShowInstructor()) ret+=1;
    	if (getDisplayTimetable() && isShowTimetable()) ret+=TIMETABLE_COLUMN_ORDER.length;
    	if (isShowCredit()) ret+=1;
    	if (isShowSchedulePrintNote()) ret+=1;
    	if (isShowNote()) ret+=1;
    	return ret;
	}
	
	public float[] getWidths() {
		float[] width = new float[getNrColumns()]; 
		int idx = 0;
    	if (isShowLabel()) width[idx++] = 175f;
    	if (isShowTitle()) width[idx++] = 200f;
    	if (isShowDivSec()) width[idx++] = 80f;
    	if (isShowDemand()) width[idx++] = 60f;
    	if (isShowProjectedDemand()) width[idx++] = 65f;
    	if (isShowConsent()) width[idx++] = 100f;
    	if (isShowDesignatorRequired()) width[idx++] = 75f;
    	if (isShowMinPerWk()) width[idx++] = 60f;
    	if (isShowLimit()) width[idx++] = 50f;
    	if (isShowRoomRatio()) width[idx++] = 50f;
    	if (isShowManager()) width[idx++] = 75f;
    	if (isShowDatePattern()) width[idx++] = 80f;
    	if (isShowTimePattern()) width[idx++] = 80f;
    	if (isShowPreferences()) {
    		for (int i=0;i<PREFERENCE_COLUMN_ORDER.length+(getDisplayDistributionPrefs()?0:-1);i++) {
    			if (i==0) {
    				if (getGridAsText())
    					width[idx++] = 200f;
    				else
    					width[idx++] = 100f;
    			} else if (i==1)
    				width[idx++] = 150f;
    			else
    				width[idx++] = 200f;
    		}
    	}
    	if (isShowInstructor()) width[idx++] = 200f;
    	if (getDisplayTimetable() && isShowTimetable()) {
    		for (int i=0;i<TIMETABLE_COLUMN_ORDER.length;i++) {
    			if (i==0)
    				width[idx++] = 130f;
    			else if (i==2)
    				width[idx++] = 70f;
    			else
    				width[idx++] = 100f;
    		}
    	}
    	if (isShowCredit()) width[idx++] = 100f;
    	if (isShowSchedulePrintNote()) width[idx++] = 150f;
    	if (isShowNote()) width[idx++] = 300f;
    	return width;
	}
	
    
    protected void pdfBuildTableHeader() {
    	iBgColor = sBgColorHeader;
    	//first line
    	if (isShowLabel()) {
    		PdfPCell c = createCell();
    		addText(c, LABEL, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowTitle()) {
    		PdfPCell c = createCell();
    		addText(c, TITLE, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowDivSec()){
    		PdfPCell c = createCell();
    		addText(c, DIV_SEC, true, Element.ALIGN_RIGHT);
    		iPdfTable.addCell(c);
    	}   	
    	if (isShowDemand()){
    		PdfPCell c = createCell();
    		addText(c, DEMAND, true, Element.ALIGN_RIGHT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowProjectedDemand()){
    		PdfPCell c = createCell();
    		addText(c, PROJECTED_DEMAND, true, Element.ALIGN_RIGHT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowConsent()) {
    		PdfPCell c = createCell();
    		addText(c, CONSENT, true, Element.ALIGN_CENTER);
    		iPdfTable.addCell(c);
    	}
    	if (isShowDesignatorRequired()) {
    		PdfPCell c = createCell();
    		addText(c, DESIGNATOR_REQ, true, Element.ALIGN_CENTER);
    		iPdfTable.addCell(c);
    	}
    	if (isShowMinPerWk()){
    		PdfPCell c = createCell();
    		addText(c, MIN_PER_WK, true, Element.ALIGN_RIGHT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowLimit()){
    		PdfPCell c = createCell();
    		addText(c, LIMIT, true, Element.ALIGN_RIGHT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowRoomRatio()){
    		PdfPCell c = createCell();
    		addText(c, ROOM_RATIO, true, Element.ALIGN_RIGHT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowManager()){
    		PdfPCell c = createCell();
    		addText(c, MANAGER, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowDatePattern()){
    		PdfPCell c = createCell();
    		addText(c, DATE_PATTERN, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowTimePattern()){
    		PdfPCell c = createCell();
    		addText(c, TIME_PATTERN, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowPreferences()){
    		PdfPCell c = createCell();
    		c.setColspan(PREFERENCE_COLUMN_ORDER.length + (getDisplayDistributionPrefs()?0:-1));
    		addText(c, "----" + PREFERENCES + "----", true, Element.ALIGN_CENTER);
    		iPdfTable.addCell(c);
    	}
    	if (isShowInstructor()){
    		PdfPCell c = createCell();
    		addText(c, INSTRUCTOR, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	if (getDisplayTimetable() && isShowTimetable()){
    		PdfPCell c = createCell();
    		c.setColspan(TIMETABLE_COLUMN_ORDER.length);
    		addText(c, "--------" + TIMETABLE + "--------", true, Element.ALIGN_CENTER);
    		iPdfTable.addCell(c);
    	}
    	if (isShowCredit()){
    		PdfPCell c = createCell();
    		addText(c, CREDIT, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowSchedulePrintNote()){
    		PdfPCell c = createCell();
    		addText(c, SCHEDULE_PRINT_NOTE, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	if (isShowNote()){
    		PdfPCell c = createCell();
    		addText(c, NOTE, true, Element.ALIGN_LEFT);
    		iPdfTable.addCell(c);
    	}
    	
    	//second line
    	if (isShowLabel()) {
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowTitle()) {
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowDivSec()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}   	
    	if (isShowDemand()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowProjectedDemand()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowConsent()) {
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowDesignatorRequired()) {
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowMinPerWk()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowLimit()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowRoomRatio()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowManager()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowDatePattern()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowTimePattern()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowPreferences()){
	    	for(int j = 0; j < PREFERENCE_COLUMN_ORDER.length+(getDisplayDistributionPrefs()?0:-1); j++){
	    		PdfPCell c = createCell();
	    		c.setBorderWidthBottom(1);
	    		addText(c, PREFERENCE_COLUMN_ORDER[j], true, Element.ALIGN_LEFT);
	    		iPdfTable.addCell(c);
	    	}
    	}
    	if (isShowInstructor()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (getDisplayTimetable() && isShowTimetable()){
	    	for(int j = 0; j < TIMETABLE_COLUMN_ORDER.length; j++){
	    		PdfPCell c = createCell();
	    		c.setBorderWidthBottom(1);
	    		addText(c, TIMETABLE_COLUMN_ORDER[j], true, Element.ALIGN_LEFT);
	    		iPdfTable.addCell(c);
	    	}
    	}
    	if (isShowCredit()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowSchedulePrintNote()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	if (isShowNote()){
    		PdfPCell c = createCell();
    		c.setBorderWidthBottom(1);
    		iPdfTable.addCell(c);
    	}
    	iPdfTable.setHeaderRows(2);
   }

    private PdfPCell pdfSubjectAndCourseInfo(InstructionalOffering io, CourseOffering co) {
    	PdfPCell cell = createCell();
        addText(cell, (co!=null? co.getSubjectAreaAbbv()+" "+co.getCourseNbr():""), false, false, Element.ALIGN_LEFT, (co.isIsControl().booleanValue()?sEnableColor:sDisableColor), true);
        for (Iterator it = io.courseOfferingsMinusSortCourseOfferingForSubjectArea(co.getSubjectArea().getUniqueId()).iterator(); it.hasNext(); ) {
        	CourseOffering tempCo = (org.unitime.timetable.model.CourseOffering) it.next();
            addText(cell,  tempCo.getSubjectAreaAbbv()+" "+tempCo.getCourseNbr(), false, false, Element.ALIGN_LEFT, sDisableColor, true);
        }
        return cell;
    }  
    
    protected PdfPCell pdfBuildPrefGroupLabel(PreferenceGroup prefGroup, String indentSpaces, boolean isEditable, String prevLabel){
    	Color color = (isEditable?sEnableColor:sDisableColor);
        String label = prefGroup.toString();
        if (prefGroup instanceof Class_) {
        	Class_ aClass = (Class_) prefGroup;
        	label = aClass.getClassLabel();
		}
        if (prevLabel != null && label.equals(prevLabel)){
        	label = "";
        }
        PdfPCell cell = createCell();
    	addText(cell, indentSpaces+label, false, false, Element.ALIGN_LEFT, color, true);
        return cell;
    }
    
    private PdfPCell pdfBuildDatePatternCell(PreferenceGroup prefGroup, boolean isEditable){
    	DatePattern dp = prefGroup.effectiveDatePattern();
    	PdfPCell cell = createCell();
    	if (dp!=null) {
    		Color color = (isEditable?sEnableColor:sDisableColor);
    		addText(cell, dp.getName(), false, false, Element.ALIGN_CENTER, color, true);
    	}
        return cell;
    }

    private PdfPCell pdfBuildTimePatternCell(PreferenceGroup prefGroup, boolean isEditable){
   		Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();
    	for (Iterator i=prefGroup.effectiveTimePatterns().iterator(); i.hasNext();) {
    		TimePattern tp = (TimePattern)i.next();
    		addText(cell, tp.getName(), false, false, Element.ALIGN_CENTER, color, true);  
    	}
        if (prefGroup instanceof Class_ && prefGroup.effectiveTimePatterns().isEmpty()) {
            if (((Class_)prefGroup).getSchedulingSubpart().getMinutesPerWk().intValue()<=0) {
                addText(cell, "Arr Hrs", false, false, Element.ALIGN_CENTER, color, true);
            } else {
                int nrHours = Math.round(((Class_)prefGroup).getSchedulingSubpart().getMinutesPerWk().intValue()/50.0f);
                addText(cell, "Arr "+nrHours+" Hrs", false, false, Element.ALIGN_CENTER, color, true);
            }
        }
        return cell;
    }
    
    private PdfPCell pdfBuildTimePrefCell(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
		Assignment a = null;
		if (getDisplayTimetable() && isShowTimetable() && classAssignment!=null && prefGroup instanceof Class_) {
			try {
				a = classAssignment.getAssignment((Class_)prefGroup);
			} catch (Exception e) {
				Debug.error(e);
			}
    	}
		
		PdfPCell cell = createCell();
		
		for (Iterator i=prefGroup.effectivePreferences(TimePref.class).iterator(); i.hasNext();) {
			TimePref tp = (TimePref)i.next();
			RequiredTimeTable rtt = tp.getRequiredTimeTable(a);
			if (getGridAsText()) {
				addText(cell, rtt.getModel().toString().replaceAll(", ", "\n"), false, false, Element.ALIGN_LEFT, color, true);
			} else {
				try {
					rtt.getModel().setDefaultSelection(getDefaultTimeGridSize());
					if (rtt.getModel().isExactTime()) {
						StringBuffer sb = new StringBuffer();
						addText(cell, rtt.exactTime(false), false, false, Element.ALIGN_LEFT, color, true);
					} else {
						java.awt.Image awtImage = rtt.createBufferedImage(getTimeVertival());
						Image img = Image.getInstance(awtImage, Color.WHITE);
						Chunk ck = new Chunk(img, 0, 0);
						if (cell.getPhrase()==null) {
							cell.setPhrase(new Paragraph(ck));
							cell.setVerticalAlignment(Element.ALIGN_TOP);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						} else {
							cell.getPhrase().add(ck);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
    	
    	return cell;
    	
    }
    private PdfPCell pdfBuildPreferenceCell(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, Class prefType, boolean isEditable){
    	if (!isEditable) return createCell();
   		Color color = (isEditable?sEnableColor:sDisableColor);

    	if (TimePref.class.equals(prefType)) {
    		return pdfBuildTimePrefCell(classAssignment, prefGroup, isEditable);
    	} else if (DistributionPref.class.equals(prefType)) {
        	PdfPCell cell = createCell();
        	for (Iterator i=prefGroup.effectivePreferences(prefType).iterator();i.hasNext();) {
        		DistributionPref pref = (DistributionPref)i.next();
        		addText(cell, PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText(true, true, " (", ", ",")").replaceAll("&lt;","<").replaceAll("&gt;",">"), false, false, Element.ALIGN_LEFT, color, true);
        	}
    		return cell ;
    	} else {
        	PdfPCell cell = createCell();
        	for (Iterator i=prefGroup.effectivePreferences(prefType).iterator();i.hasNext();) {
        		Preference pref = (Preference)i.next();
        		addText(cell, PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText(), false, false, Element.ALIGN_LEFT, color, true);
        	}
    		return cell ;
    	}
    	
    }
    
    private PdfPCell pdfBuildPreferenceCell(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, Class[] prefTypes, boolean isEditable){
    	if (!isEditable) return createCell();
    	Color color = (isEditable?sEnableColor:sDisableColor);

    	PdfPCell cell = createCell();
    	boolean noRoomPrefs = false;
    	if (prefGroup instanceof Class_ && ((Class_)prefGroup).getNbrRooms().intValue()==0) {
    		 noRoomPrefs = true;
    	}
        if (prefGroup instanceof SchedulingSubpart && ((SchedulingSubpart)prefGroup).getInstrOfferingConfig().isUnlimitedEnrollment().booleanValue())
            noRoomPrefs = true;
    	for (int i=0;i<prefTypes.length;i++) {
    		Class prefType = prefTypes[i];
    		if (noRoomPrefs) {
    			if (//prefType.equals(RoomPref.class) || 
    				prefType.equals(RoomGroupPref.class) || 
    				prefType.equals(RoomFeaturePref.class) || 
    				prefType.equals(BuildingPref.class))
    				continue;
    		}
    		for (Iterator j=prefGroup.effectivePreferences(prefType).iterator();j.hasNext();) {
    			Preference pref = (Preference)j.next();
    			addText(cell, PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText(), false, false, Element.ALIGN_LEFT, color, true);
    		}
    	}
    	if (noRoomPrefs && cell.getPhrase()==null)
    		addText(cell, "N/A", false, true, Element.ALIGN_LEFT, color, true);
		return cell;
    }

    private PdfPCell pdfBuildPrefGroupDemand(PreferenceGroup prefGroup, boolean isEditable){
    	return createCell();
    }
    
    private PdfPCell pdfBuildPrefGroupProjectedDemand(PreferenceGroup prefGroup, boolean isEditable){
    	return createCell();
    }
    
    private PdfPCell pdfBuildLimit(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);

    	PdfPCell cell = createCell();

    	if (prefGroup instanceof SchedulingSubpart){
	    	SchedulingSubpart ss = (SchedulingSubpart) prefGroup;
	    	boolean unlimited = ss.getInstrOfferingConfig().isUnlimitedEnrollment().booleanValue();
	    	if (!unlimited) {
		    	int limit = (ss.getLimit()==null?0:ss.getLimit().intValue());
		    	int maxExpCap = ss.getMaxExpectedCapacity(); 
		    	if (limit==maxExpCap)
		    		addText(cell, String.valueOf(limit), false, false, Element.ALIGN_RIGHT, color, true);
		    	else
		    		addText(cell, limit+"-"+maxExpCap, false, false, Element.ALIGN_RIGHT, color, true);
	    	} 
    	} else if (prefGroup instanceof Class_){
    		Class_ aClass = (Class_) prefGroup;
	    	boolean unlimited = aClass.getSchedulingSubpart().getInstrOfferingConfig().isUnlimitedEnrollment().booleanValue();
	    	if (!unlimited) {
	    		String limitString = null;
                Assignment a = null;
                try {
                    if (classAssignment!=null) a = classAssignment.getAssignment(aClass);
                } catch (Exception e) {}
                if (a==null) {
                    if (aClass.getExpectedCapacity() != null){
                        limitString = aClass.getExpectedCapacity().toString();
                        if (aClass.getMaxExpectedCapacity() != null && !aClass.getMaxExpectedCapacity().equals(aClass.getExpectedCapacity())){
                            limitString = limitString + "-" + aClass.getMaxExpectedCapacity().toString();
                        }
                    } else {
                        limitString = "0";
                        if (aClass.getMaxExpectedCapacity() != null && aClass.getMaxExpectedCapacity().intValue() != 0){
                            limitString = limitString + "-" + aClass.getMaxExpectedCapacity().toString();
                        }
                    }
                } else {
                    limitString = ""+aClass.getClassLimit(classAssignment);
                }
	    		addText(cell, limitString, false, false, Element.ALIGN_RIGHT, color, true);
	    	}
    	} 
    	
        return cell;
    }
    
    private PdfPCell pdfBuildDivisionSection(PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();
    	
    	if (prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
    		String divSec = aClass.getDivSecNumber();
    		if (divSec!=null)
    			addText(cell, divSec, false, false, Element.ALIGN_RIGHT, color, true);
    	}
    	
        return cell;
    }

    private PdfPCell pdfBuildInstructor(PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();
    	
    	if (prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
        	TreeSet sortedInstructors = new TreeSet(new InstructorComparator());
        	sortedInstructors.addAll(aClass.getClassInstructors());
    		for (Iterator i=sortedInstructors.iterator(); i.hasNext();) {
    			ClassInstructor ci = (ClassInstructor)i.next();
        		String label = ci.getInstructor().getName(getInstructorNameFormat());
        		boolean italic = !aClass.isDisplayInstructor().booleanValue();
        		boolean bold = ci.isLead().booleanValue();
        		addText(cell, label, bold, italic, Element.ALIGN_LEFT, color, true);
    		}
    	}
    	
        return cell;
    }

    private PdfPCell pdfBuildCredit(PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	if (prefGroup instanceof SchedulingSubpart) {
    		SchedulingSubpart ss = (SchedulingSubpart) prefGroup;
    		if (ss.getCredit() != null) {
    			addText(cell, ss.getCredit().creditAbbv(), false, false, Element.ALIGN_LEFT, color, true);
    		}   		
    	}
    	
        return cell;
    }

    private PdfPCell pdfBuildSchedulePrintNote(PreferenceGroup prefGroup, boolean isEditable) {
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	if (prefGroup instanceof Class_) {
    		Class_ c = (Class_) prefGroup;
    		if (c.getSchedulePrintNote()!=null) {
    			addText(cell, c.getSchedulePrintNote(), false, false, Element.ALIGN_LEFT, color, true);
    		}
    	}
    	
        return cell;
    }

    private PdfPCell pdfBuildSchedulePrintNote(InstructionalOffering io, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	StringBuffer note = new StringBuffer("");
		Set s = io.getCourseOfferings();
		for (Iterator i=s.iterator(); i.hasNext(); ) {
			CourseOffering coI = (CourseOffering) i.next();
			if (coI.getScheduleBookNote()!=null && coI.getScheduleBookNote().trim().length()>0) {
				if (note.length()>0)
					note.append("\n");
				note.append(coI.getScheduleBookNote());
			}
		}
		
		addText(cell, note.toString(), false, false, Element.ALIGN_LEFT, color, true);
        return(cell);
    }    
    
    private PdfPCell pdfBuildNote(PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	if (prefGroup instanceof Class_) {
    		Class_ c = (Class_) prefGroup;
    		if (c.getNotes()!=null) {
    			addText(cell, c.getNotes(), false, false, Element.ALIGN_LEFT, color, true);
    		}
    	}
    	
        return cell;
    }

    private PdfPCell pdfBuildManager(PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	Department managingDept = null;
    	if (prefGroup instanceof Class_) {
    		managingDept = ((Class_)prefGroup).getManagingDept();
    	} else if (prefGroup instanceof SchedulingSubpart) {
    		managingDept = ((SchedulingSubpart)prefGroup).getManagingDept();
    	}
    	if (managingDept!=null) {
    		addText(cell, managingDept.getShortLabel(), false, false, Element.ALIGN_LEFT, color, true);
    	}

        return cell;
    }

    private PdfPCell pdfBuildMinPerWeek(PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	if (prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
    		addText(cell, aClass.getSchedulingSubpart().getMinutesPerWk().toString(), false, false, Element.ALIGN_RIGHT, color, true);
    	} else if (prefGroup instanceof SchedulingSubpart) {
    		SchedulingSubpart aSchedulingSubpart = (SchedulingSubpart) prefGroup;
    		addText(cell, aSchedulingSubpart.getMinutesPerWk().toString(), false, false, Element.ALIGN_RIGHT, color, true);
    	} 

        return cell;
    }

    private PdfPCell pdfBuildRoomLimit(PreferenceGroup prefGroup, boolean isEditable, boolean classLimitDisplayed){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	if (prefGroup instanceof Class_){
    		Class_ aClass = (Class_) prefGroup;
    		if (aClass.getNbrRooms()!=null && aClass.getNbrRooms().intValue()!=1) {
    			if (aClass.getNbrRooms().intValue()==0)
    				addText(cell, "N/A", false, true, Element.ALIGN_RIGHT, color, true);
    			else {
    				String text = aClass.getNbrRooms().toString();
    				text += " at ";
    				if (aClass.getRoomRatio() != null)
    					text += sRoomRatioFormat.format(aClass.getRoomRatio().floatValue());
    				else
    					text += "0";
    				addText(cell, text, false, false, Element.ALIGN_RIGHT, color, true);
    			}
    		} else {
    			if (aClass.getRoomRatio() != null){
    				if (classLimitDisplayed && aClass.getRoomRatio().equals(new Float(1.0))){
    					addText(cell, "", false, false, Element.ALIGN_RIGHT, color, true);
    				} else {
    					addText(cell, sRoomRatioFormat.format(aClass.getRoomRatio().floatValue()), false, false, Element.ALIGN_RIGHT, color, true);
    				}
    			} else {
    				if (aClass.getExpectedCapacity() == null){
    					addText(cell, "", false, false, Element.ALIGN_RIGHT, color, true);
    				} else {
    					addText(cell, "0", false, false, Element.ALIGN_RIGHT, color, true);
    				}
    			}
    		}
    	}
    	
        return cell;
    }
    
    private PdfPCell pdfBuildAssignedTime(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable){
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	if (classAssignment!=null && prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
    		Assignment a = null;
    		AssignmentPreferenceInfo info = null;
    		try {
    			a = classAssignment.getAssignment(aClass);
    			info = classAssignment.getAssignmentInfo(aClass);
    		} catch (Exception e) {
    			Debug.error(e);
    		}
    		if (a!=null) {
    			StringBuffer sb = new StringBuffer();
   				IntEnumeration e = a.getTimeLocation().getDays();
   				while (e.hasMoreElements()){
   					sb.append(Constants.DAY_NAMES_SHORT[(int)e.nextInt()]);
   				}
   				sb.append(" ");
   				sb.append(a.getTimeLocation().getStartTimeHeader());
   				sb.append("-");
   				sb.append(a.getTimeLocation().getEndTimeHeader());
   				addText(cell, sb.toString(), false, false, Element.ALIGN_LEFT, color, true);
    		} 
    	}
    	
        return cell;
    }
   
    private PdfPCell pdfBuildAssignedRoom(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable) {
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	if (classAssignment!=null && prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
    		Assignment a = null;
    		AssignmentPreferenceInfo info = null;
    		try {
    			a= classAssignment.getAssignment(aClass);
    			info = classAssignment.getAssignmentInfo(aClass);
    		} catch (Exception e) {
    			Debug.error(e);
    		}
    		if (a!=null) {
    			StringBuffer sb = new StringBuffer();
	    		Iterator it2 = a.getRooms().iterator();
	    		while (it2.hasNext()){
	    			Location room = (Location)it2.next();
	    			sb.append(room.getLabel());
	    			if (it2.hasNext()){
	        			sb.append("\n");
	        		} 
	    		}
	    		addText(cell, sb.toString(), false, false, Element.ALIGN_LEFT, color, true);
    		}
    	}
    	
        return cell;
    }
    private PdfPCell pdfBuildAssignedRoomCapacity(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable) {
    	Color color = (isEditable?sEnableColor:sDisableColor);
    	PdfPCell cell = createCell();

    	if (classAssignment!=null && prefGroup instanceof Class_){
    		Class_ aClass = (Class_) prefGroup;
    		Assignment a = null;
   			try {
   				a = classAssignment.getAssignment(aClass);
   			} catch (Exception e) {
   				Debug.error(e);
   			}
	   		if (a!=null) {
	   			StringBuffer sb = new StringBuffer();
				Iterator it2 = a.getRooms().iterator();
				while (it2.hasNext()){
					sb.append(((Location) it2.next()).getCapacity());
					if (it2.hasNext()){
    					sb.append("\n");
    				} 
				}
				addText(cell, sb.toString(), false, false, Element.ALIGN_LEFT, color, true);
    		}
    	}

    	return cell;
    }
    
    //NOTE: if changing column order column order must be changed in
    //		buildTableHeader, addInstrOffrRowsToTable, buildClassOrSubpartRow, and buildConfigRow
    protected void pdfBuildClassOrSubpartRow(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, String indentSpaces, boolean isEditable, String prevLabel){
    	boolean classLimitDisplayed = false;
    	if (isShowLabel()){
	        iPdfTable.addCell(pdfBuildPrefGroupLabel(prefGroup, indentSpaces, isEditable, prevLabel));
    	} 
    	if (isShowTitle()){
	        iPdfTable.addCell(createCell());
    	} 
    	if (isShowDivSec()){
    		iPdfTable.addCell(pdfBuildDivisionSection(prefGroup, isEditable));
    	}
    	if (isShowDemand()){
    		iPdfTable.addCell(pdfBuildPrefGroupDemand(prefGroup, isEditable));
    	} 
    	if (isShowProjectedDemand()){
    		iPdfTable.addCell(pdfBuildPrefGroupProjectedDemand(prefGroup, isEditable));
    	} 
    	if (isShowConsent()){
	        iPdfTable.addCell(createCell());
    	} 
    	if (isShowDesignatorRequired()){
	        iPdfTable.addCell(createCell());
    	} 
    	if (isShowMinPerWk()){
    		iPdfTable.addCell(pdfBuildMinPerWeek(prefGroup, isEditable));
       	} 
    	if (isShowLimit()){
    		classLimitDisplayed = true;
    		iPdfTable.addCell(pdfBuildLimit(classAssignment, prefGroup, isEditable));
       	} 
    	if (isShowRoomRatio()){
    		iPdfTable.addCell(pdfBuildRoomLimit(prefGroup, isEditable, classLimitDisplayed));
       	} 
    	if (isShowManager()){
    		iPdfTable.addCell(pdfBuildManager(prefGroup, isEditable));
     	} 
    	if (isShowDatePattern()){
    		iPdfTable.addCell(pdfBuildDatePatternCell(prefGroup, isEditable));
     	} 
    	if (isShowTimePattern()){
    		iPdfTable.addCell(pdfBuildTimePatternCell(prefGroup, isEditable));
    	} 
    	if (isShowPreferences()){
	        for (int j = 0; j < PREFERENCE_COLUMN_ORDER.length; j++) {
	        	if (PREFERENCE_COLUMN_ORDER[j].equals(TIME)) {
	        		iPdfTable.addCell(pdfBuildPreferenceCell(classAssignment,prefGroup, TimePref.class, isEditable));
	        	} else if (sAggregateRoomPrefs && PREFERENCE_COLUMN_ORDER[j].equals(ALL_ROOM)) {
	        		iPdfTable.addCell(pdfBuildPreferenceCell(classAssignment,prefGroup, new Class[] {RoomPref.class, BuildingPref.class, RoomFeaturePref.class, RoomGroupPref.class} , isEditable));
	        	} else if (PREFERENCE_COLUMN_ORDER[j].equals(ROOM)) {
	        		iPdfTable.addCell(pdfBuildPreferenceCell(classAssignment,prefGroup, RoomPref.class, isEditable));
	        	} else if (PREFERENCE_COLUMN_ORDER[j].equals(BLDG)) {
	        		iPdfTable.addCell(pdfBuildPreferenceCell(classAssignment,prefGroup, BuildingPref.class, isEditable));
	        	} else if (PREFERENCE_COLUMN_ORDER[j].equals(FEATURES)) {
	        		iPdfTable.addCell(pdfBuildPreferenceCell(classAssignment,prefGroup, RoomFeaturePref.class, isEditable));
	        	} else if (getDisplayDistributionPrefs() && PREFERENCE_COLUMN_ORDER[j].equals(DISTRIBUTION)) {
	        		iPdfTable.addCell(pdfBuildPreferenceCell(classAssignment,prefGroup, DistributionPref.class, isEditable));
	        	} else if (PREFERENCE_COLUMN_ORDER[j].equals(ROOMGR)) {
	        		iPdfTable.addCell(pdfBuildPreferenceCell(classAssignment,prefGroup, RoomGroupPref.class, isEditable));
	        	}
	        }
    	} 
    	if (isShowInstructor()){
    		iPdfTable.addCell(pdfBuildInstructor(prefGroup, isEditable));
    	}
    	if (getDisplayTimetable() && isShowTimetable()){
	        for (int j = 0; j < TIMETABLE_COLUMN_ORDER.length; j++) {
	        	if (TIMETABLE_COLUMN_ORDER[j].equals(ASSIGNED_TIME)){
	        		iPdfTable.addCell(pdfBuildAssignedTime(classAssignment, prefGroup, isEditable));
	        	} else if (TIMETABLE_COLUMN_ORDER[j].equals(ASSIGNED_ROOM)){
	        		iPdfTable.addCell(pdfBuildAssignedRoom(classAssignment, prefGroup, isEditable));
	        	} else if (TIMETABLE_COLUMN_ORDER[j].equals(ASSIGNED_ROOM_CAPACITY)){
	        		iPdfTable.addCell(pdfBuildAssignedRoomCapacity(classAssignment, prefGroup, isEditable));
	        	}
	        }
    	} 
    	if (isShowCredit()){
    		iPdfTable.addCell(pdfBuildCredit(prefGroup, isEditable));     		
    	} 
    	if (isShowSchedulePrintNote()){
    		iPdfTable.addCell(pdfBuildSchedulePrintNote(prefGroup, isEditable));     		
    	} 
    	if (isShowNote()){
    		iPdfTable.addCell(pdfBuildNote(prefGroup, isEditable));     		
    	}
    }
    
    private void pdfBuildSchedulingSubpartRow(ClassAssignmentProxy classAssignment, SchedulingSubpart ss, String indentSpaces, User user){
        boolean isEditable = ss.isViewableBy(user);
        iBgColor = sBgColorSubpart;
        pdfBuildClassOrSubpartRow(classAssignment, ss, indentSpaces, isEditable, null);
    }
    
    private void pdfBuildSchedulingSubpartRows(Vector subpartIds, ClassAssignmentProxy classAssignment, SchedulingSubpart ss, String indentSpaces, User user){
    	if (subpartIds!=null) subpartIds.add(ss.getUniqueId());
        pdfBuildSchedulingSubpartRow(classAssignment, ss, indentSpaces, user);
        Set childSubparts = ss.getChildSubparts();
        
		if (childSubparts != null && !childSubparts.isEmpty()){
		    
		    ArrayList childSubpartList = new ArrayList(childSubparts);
		    Collections.sort(childSubpartList, new SchedulingSubpartComparator());
            Iterator it = childSubpartList.iterator();
            SchedulingSubpart child = null;
            
            while (it.hasNext()){              
                child = (SchedulingSubpart) it.next();
                pdfBuildSchedulingSubpartRows(subpartIds, classAssignment, child, indentSpaces + indent, user);
            }
        }
    }
 
    protected void pdfBuildClassRow(ClassAssignmentProxy classAssignment, int ct, Class_ aClass, String indentSpaces, User user, String prevLabel){
        boolean isEditable = aClass.isViewableBy(user);
        iBgColor = sBgColorClass;
        pdfBuildClassOrSubpartRow(classAssignment, aClass, indentSpaces, isEditable, prevLabel);
    }
    
    private void pdfBuildClassRows(ClassAssignmentProxy classAssignment, int ct, Class_ aClass, String indentSpaces, User user, String prevLabel){

        pdfBuildClassRow(classAssignment, ct, aClass, indentSpaces, user, prevLabel);
    	Set childClasses = aClass.getChildClasses();

    	if (childClasses != null && !childClasses.isEmpty()){
        
    	    ArrayList childClassesList = new ArrayList(childClasses);
            Collections.sort(childClassesList, getClassComparator());
            
            Iterator it = childClassesList.iterator();
            Class_ child = null;
            String previousLabel = aClass.htmlLabel();
            while (it.hasNext()){              
                child = (Class_) it.next();
                pdfBuildClassRows(classAssignment, ct, child, indentSpaces + indent, user, previousLabel);
            }
        }
    }


	protected void pdfBuildConfigRow(Vector subpartIds, ClassAssignmentProxy classAssignment, InstrOfferingConfig ioc, User user, boolean printConfigLine) {
		iBgColor = sBgColorConfig;
	    boolean isEditable = ioc.isViewableBy(user);
	    Color color = (isEditable?sEnableColor:sDisableColor);
	    String configName = ioc.getName();
	    boolean unlimited = ioc.isUnlimitedEnrollment().booleanValue();
	    boolean hasConfig = false;
		if (printConfigLine) {
        	if (isShowLabel()) {
        	    if (configName==null || configName.trim().length()==0)
        	        configName = ioc.getUniqueId().toString();
        	    PdfPCell cell = createCell();
        	    addText(cell, indent + "Configuration " + configName, false, false, Element.ALIGN_LEFT, color, true);
        	    iPdfTable.addCell(cell);
        	}
        	if (isShowTitle()){
    	        iPdfTable.addCell(createCell());
        	} 
        	if (isShowDivSec()){
        	    iPdfTable.addCell(createCell());
    		}
        	if (isShowDemand()){
        	    iPdfTable.addCell(createCell());
    		}
        	if (isShowProjectedDemand()){
        	    iPdfTable.addCell(createCell());
    		} 
        	if (isShowConsent()){
    	        iPdfTable.addCell(createCell());
        	} 
        	if (isShowDesignatorRequired()){
    	        iPdfTable.addCell(createCell());
        	} 
        	if (isShowMinPerWk()){
        	    iPdfTable.addCell(createCell());
        	} 
        	if (isShowLimit()){
        	    PdfPCell cell = createCell();
        	    addText(cell, (unlimited?"inf":ioc.getLimit().toString()), false, false, Element.ALIGN_RIGHT, color, true);
        	    iPdfTable.addCell(cell);
        	} 
        	if (isShowRoomRatio()){
        	    iPdfTable.addCell(createCell());
        	} 
        	if (isShowManager()){
        	    iPdfTable.addCell(createCell());
        	} 
        	if (isShowDatePattern()){
        	    iPdfTable.addCell(createCell());
	       	} 
        	if (isShowTimePattern()){
        	    iPdfTable.addCell(createCell());
        	} 
        	if (isShowPreferences()){
		        for (int j = 0; j < PREFERENCE_COLUMN_ORDER.length + (getDisplayDistributionPrefs()?0:-1); j++) {
	        	    iPdfTable.addCell(createCell());
		        }
        	} 
        	if (isShowInstructor()){
        	    iPdfTable.addCell(createCell());
        	} 
        	if (getDisplayTimetable() && isShowTimetable()){
        		for (int j = 0; j < TIMETABLE_COLUMN_ORDER.length; j++){
            	    iPdfTable.addCell(createCell());
        		}
        	} 
        	if (isShowCredit()){
        	    iPdfTable.addCell(createCell());
        	} 
        	if (isShowSchedulePrintNote()){
        	    iPdfTable.addCell(createCell());
        	} 
        	if (isShowNote()){
        	    iPdfTable.addCell(createCell());
        	}
	        
	        hasConfig = true;
		}
		
        ArrayList subpartList = new ArrayList(ioc.getSchedulingSubparts());
        Collections.sort(subpartList, new SchedulingSubpartComparator());
        Iterator it = subpartList.iterator();
        SchedulingSubpart ss = null;
        while(it.hasNext()){
            ss = (SchedulingSubpart) it.next();
            if (ss.getParentSubpart() == null){
                pdfBuildSchedulingSubpartRows(subpartIds, classAssignment, ss, (hasConfig?indent+indent:indent) , user);
            }
        }
        it = subpartList.iterator();
        int ct = 0;
        String prevLabel = null;
        while (it.hasNext()) {   	
			ss = (SchedulingSubpart) it.next();
			if (ss.getParentSubpart() == null) {
				if (ss.getClasses() != null) {
					Vector classes = new Vector(ss.getClasses());
					Collections.sort(classes,getClassComparator());
					Iterator cIt = classes.iterator();					
					Class_ c = null;
					while (cIt.hasNext()) {
						c = (Class_) cIt.next();
						pdfBuildClassRows(classAssignment, ++ct, c, indent, user, prevLabel);
						prevLabel = c.htmlLabel();
					}
				}
			}
		}
        
        //TODO: print config reservations
        /*
		if (!printConfigLine) {
	        ReservationsTableBuilder r = new ReservationsTableBuilder();
	        String resvTable = r.htmlTableForReservations(ioc.effectiveReservations(), true, ioc.isViewableBy(user));
	        if (resvTable!=null && resvTable.trim().length()>0) {
			    TableRow row = this.initRow(false);
		        TableCell cell = initColSpanCell(r.createTable(resvTable, "margin:0;", null, null), false, 12);
		        row.addContent(cell);
		        table.addContent(row);
	        }
		}
		*/
   }

    private void pdfBuildConfigRows(ClassAssignmentProxy classAssignment, Set instrOfferingConfigs, User user, boolean printConfigLine) {
        Iterator it = instrOfferingConfigs.iterator();
        InstrOfferingConfig ioc = null;
        while (it.hasNext()){
            ioc = (InstrOfferingConfig) it.next();
            pdfBuildConfigRow(null, classAssignment,  ioc, user, printConfigLine && instrOfferingConfigs.size()>1);
        }
    }

    private void pdfAddInstrOffrRowsToTable(ClassAssignmentProxy classAssignment, InstructionalOffering io, Long subjectAreaId, User user){
    	iBgColor = sBgColorOffering;
        CourseOffering co = io.findSortCourseOfferingForSubjectArea(subjectAreaId);
        boolean isEditable = io.isViewableBy(user);
        if (!isEditable){
        	if (io.getInstrOfferingConfigs() != null && io.getInstrOfferingConfigs().size() > 0){
        		boolean canEdit = true;
        		Iterator it = io.getInstrOfferingConfigs().iterator();
        		InstrOfferingConfig ioc = null;
        		while(canEdit && it.hasNext()){
        			ioc = (InstrOfferingConfig) it.next();
        			if(!ioc.isViewableBy(user)){
        				canEdit = false;
        			}
        		}
        		isEditable = canEdit;
        	}
        }
        boolean isManagedAs = !co.isIsControl().booleanValue();
        Color color = (isEditable?sEnableColor:sDisableColor);
        
    	if (isShowLabel()){
    		iPdfTable.addCell(pdfSubjectAndCourseInfo(io, co));
    	}
    	if (isShowTitle()){
    	    PdfPCell cell = createCell();
    	    addText(cell, (io.getControllingCourseOffering().getTitle()!=null ? io.getControllingCourseOffering().getTitle() : ""), false, false, Element.ALIGN_LEFT, color, true);
    	    iPdfTable.addCell(cell);
    	} 
    	if (isShowDivSec()){
    		iPdfTable.addCell(createCell());
		}
    	if (isShowDemand()){
    	    PdfPCell cell = createCell();
    	    addText(cell, (io.getDemand() != null?io.getDemand().toString(): "0"), false, false, Element.ALIGN_RIGHT, color, true);
    	    iPdfTable.addCell(cell);
		}
    	if (isShowProjectedDemand()){
    	    PdfPCell cell = createCell();
    	    addText(cell, (io.getProjectedDemand() != null?io.getProjectedDemand().toString(): "0"), false, false, Element.ALIGN_RIGHT, color, true);
    	    iPdfTable.addCell(cell);
		} 
    	if (isShowConsent()){
    	    PdfPCell cell = createCell();
    	    addText(cell, (io.getConsentType()!=null ? io.getConsentType().getAbbv() : ""), false, false, Element.ALIGN_CENTER, color, true);
    	    iPdfTable.addCell(cell);
    	} 
    	if (isShowDesignatorRequired()){
    	    PdfPCell cell = createCell();
    	    addText(cell, (io.isDesignatorRequired()!=null && io.isDesignatorRequired().booleanValue()?"Yes" : "No"), false, false, Element.ALIGN_CENTER, color, true);
    	    iPdfTable.addCell(cell);
    	} 
    	if (isShowMinPerWk()){
    		iPdfTable.addCell(createCell());
    	} 
    	if (isShowLimit()){
			boolean unlimited = false;
			for (Iterator x=io.getInstrOfferingConfigs().iterator();!unlimited && x.hasNext();)
				if ((((InstrOfferingConfig)x.next())).isUnlimitedEnrollment().booleanValue())
					unlimited = true;
    	    PdfPCell cell = createCell();
    	    addText(cell, (unlimited?"inf":io.getLimit()==null?"0":io.getLimit().toString()), false, false, Element.ALIGN_RIGHT, color, true);
    	    iPdfTable.addCell(cell);
    	} 
    	if (isShowRoomRatio()){
    		iPdfTable.addCell(createCell());
    	} 
    	if (isShowManager()){
    		iPdfTable.addCell(createCell());
    	}
    	if (isShowDatePattern()){
    		iPdfTable.addCell(createCell());
       	}
    	PdfPCell managedCell = null;
    	if (isShowTimePattern()){
            if (isManagedAs) {
            	managedCell = createCell();
        	    addText(managedCell, "Managed As " + io.getControllingCourseOffering().getCourseName(), false, false, Element.ALIGN_LEFT, color, true);
        	    managedCell.setColspan(1);
            } else {
            	iPdfTable.addCell(createCell());
            }
    	}
    	if (isShowPreferences()){
	        for (int j = 0; j < PREFERENCE_COLUMN_ORDER.length + (getDisplayDistributionPrefs()?0:-1); j++) {
    			if (managedCell!=null)
    				managedCell.setColspan(1+managedCell.getColspan());
    			else
    				iPdfTable.addCell(createCell());
	        }
    	}
    	if (isShowInstructor()){
			if (managedCell!=null)
				managedCell.setColspan(1+managedCell.getColspan());
			else
				iPdfTable.addCell(createCell());
    	}
    	if (getDisplayTimetable() && isShowTimetable()){
    		for (int j = 0; j < TIMETABLE_COLUMN_ORDER.length; j++){
    			if (managedCell!=null)
    				managedCell.setColspan(1+managedCell.getColspan());
    			else
    				iPdfTable.addCell(createCell());
    		}
    	} 
    	if (managedCell!=null)
    		iPdfTable.addCell(managedCell);
    	if (isShowCredit()){
    		if (io.getCredit()!=null) {
        	    PdfPCell cell = createCell();
        	    addText(cell, io.getCredit().creditAbbv(), false, false, Element.ALIGN_LEFT, color, true);
    	    	iPdfTable.addCell(cell);
    		} else
    			iPdfTable.addCell(createCell());     		
    	}
    	if (isShowSchedulePrintNote()){
    		iPdfTable.addCell(pdfBuildSchedulePrintNote(io, isEditable));     		
    	}
    	if (isShowNote()){
    		iPdfTable.addCell(createCell());     		
    	}

        if (io.getInstrOfferingConfigs() != null & !io.getInstrOfferingConfigs().isEmpty()){
        	TreeSet configs = new TreeSet(new InstrOfferingConfigComparator(io.getControllingCourseOffering().getSubjectArea().getUniqueId()));
        	configs.addAll(io.getInstrOfferingConfigs());
            pdfBuildConfigRows(classAssignment, configs, user, true);
        }
    }

    public File pdfTableForInstructionalOffering(
    		ClassAssignmentProxy classAssignment, 
            Long instructionalOfferingId, 
            User user,
            Comparator classComparator){
    	
    	if (instructionalOfferingId != null && user != null){
	        InstructionalOfferingDAO idao = new InstructionalOfferingDAO();
	        InstructionalOffering io = idao.get(instructionalOfferingId);
	        Long subjectAreaId = io.getControllingCourseOffering().getSubjectArea().getUniqueId();
	        
	        // Get Configuration
	        TreeSet ts = new TreeSet();
	        ts.add(io);
	        WebInstructionalOfferingTableBuilder iotbl = new WebInstructionalOfferingTableBuilder();
	        iotbl.setDisplayDistributionPrefs(false);
	        setVisibleColumns(COLUMNS);
		    return pdfTableForInstructionalOfferings(
				        classAssignment,
				        ts, subjectAreaId, user, false, false, classComparator);
    	}
    	return null;
    }
    
    public File pdfTableForInstructionalOfferings(
            ClassAssignmentProxy classAssignment, 
            InstructionalOfferingListForm form, 
            Long subjectAreaId, 
            User user,
            boolean displayHeader,
            boolean allCoursesAreGiven){
    	
    	setVisibleColumns(form);
    	return pdfTableForInstructionalOfferings(classAssignment, 
    			(TreeSet) form.getInstructionalOfferings(), 
     			subjectAreaId,
    			user,
    			displayHeader, allCoursesAreGiven,
    			new ClassComparator(form.getSortBy(), classAssignment, false));
   	
    }
    
    protected File pdfTableForInstructionalOfferings(
            ClassAssignmentProxy classAssignment, 
            TreeSet insructionalOfferings, 
            Long subjectAreaId, 
            User user,
            boolean displayHeader, boolean allCoursesAreGiven,
            Comparator classComparator) {
    	
    	if (classComparator!=null)
    		setClassComparator(classComparator);
    	
    	FileOutputStream out = null;
    	try {
    	
    	if (isShowTimetable()) {
    			boolean hasTimetable = false;
    			try {
    				TimetableManager manager = TimetableManager.getManager(user);
    				if (manager!=null && manager.canSeeTimetable(Session.getCurrentAcadSession(user), user) && classAssignment!=null) {
                    	if (classAssignment instanceof CachedClassAssignmentProxy) {
                    		Vector allClasses = new Vector();
            				for (Iterator i=insructionalOfferings.iterator();!hasTimetable && i.hasNext();) {
            					InstructionalOffering io = (InstructionalOffering)i.next();
            					for (Iterator j=io.getInstrOfferingConfigs().iterator();!hasTimetable && j.hasNext();) {
            						InstrOfferingConfig ioc = (InstrOfferingConfig)j.next();
            						for (Iterator k=ioc.getSchedulingSubparts().iterator();!hasTimetable && k.hasNext();) {
            							SchedulingSubpart ss = (SchedulingSubpart)k.next();
            							for (Iterator l=ss.getClasses().iterator();l.hasNext();) {
            								Class_ clazz = (Class_)l.next();
            								allClasses.add(clazz);
            							}
            						}
            					}
            				}
                    		((CachedClassAssignmentProxy)classAssignment).setCache(allClasses);
                    		hasTimetable = !classAssignment.getAssignmentTable(allClasses).isEmpty();
                    	} else {
            				for (Iterator i=insructionalOfferings.iterator();!hasTimetable && i.hasNext();) {
            					InstructionalOffering io = (InstructionalOffering)i.next();
            					for (Iterator j=io.getInstrOfferingConfigs().iterator();!hasTimetable && j.hasNext();) {
            						InstrOfferingConfig ioc = (InstrOfferingConfig)j.next();
            						for (Iterator k=ioc.getSchedulingSubparts().iterator();!hasTimetable && k.hasNext();) {
            							SchedulingSubpart ss = (SchedulingSubpart)k.next();
            							for (Iterator l=ss.getClasses().iterator();l.hasNext();) {
            								Class_ clazz = (Class_)l.next();
            								if (classAssignment.getAssignment(clazz)!=null) {
            									hasTimetable = true; break;
            								}
            							}
                    				}
                    			}
                    		}
                    	}
    				}
    			} catch (Exception e) {}
    			setDisplayTimetable(hasTimetable);
    	}

        ArrayList notOfferedOfferings = new ArrayList();
        ArrayList offeredOfferings = new ArrayList();
        ArrayList offeringIds = new ArrayList();
        
        Iterator it = insructionalOfferings.iterator();
        InstructionalOffering io = null;
        boolean hasOfferedCourses = false;
        boolean hasNotOfferedCourses = false;
		setUserSettings(user);
        
         while (it.hasNext()){
            io = (InstructionalOffering) it.next();
            if (io.isNotOffered() == null || io.isNotOffered().booleanValue()){
            	hasNotOfferedCourses = true;
            	notOfferedOfferings.add(io);
            } else {
            	hasOfferedCourses = true;
            	offeredOfferings.add(io);
            }
        }
         
        File file = ApplicationProperties.getTempFile("insofferings", "pdf");
    	
    	float[] widths = getWidths();
    	float totalWidth = 0;
    	for (int i=0;i<widths.length;i++)
    		totalWidth += widths[i];
    	
    	
    	iDocument = new Document(new Rectangle(60f+totalWidth,60f+0.77f*totalWidth), 30f, 30f, 30f, 30f); 

    	out = new FileOutputStream(file);
		iWriter = PdfEventHandler.initFooter(iDocument, out);
		iDocument.open();
         
         
        if (hasOfferedCourses || allCoursesAreGiven) {
        	iPdfTable = new PdfPTable(getWidths());
        	iPdfTable.setWidthPercentage(100);
        	iPdfTable.getDefaultCell().setPadding(3);
        	iPdfTable.getDefaultCell().setBorderWidth(0);
        	iPdfTable.setSplitRows(false);

        	if(displayHeader) {
   		    	iDocument.add(new Paragraph("Offered Courses", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
    		}
    		pdfBuildTableHeader();
                  
            if (hasOfferedCourses){
                it = offeredOfferings.iterator();
                while (it.hasNext()){
                    io = (InstructionalOffering) it.next();
                    offeringIds.add(io.getUniqueId());
                    	pdfAddInstrOffrRowsToTable(classAssignment, io, subjectAreaId, user);            	
                }
            } else {
                if(displayHeader)
                	iDocument.add(new Paragraph("There are no courses currently offered for this subject.", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, Color.RED)));
            }
            
            iDocument.add(iPdfTable);
        }
        
        if (hasNotOfferedCourses || allCoursesAreGiven) {
        	iPdfTable = new PdfPTable(getWidths());
        	iPdfTable.setWidthPercentage(100);
        	iPdfTable.getDefaultCell().setPadding(3);
        	iPdfTable.getDefaultCell().setBorderWidth(0);
        	iPdfTable.setSplitRows(false);

        	if(displayHeader) {
   	        	iDocument.newPage();
   				iDocument.add(new Paragraph("Not Offered Courses", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
            }
            pdfBuildTableHeader();
            
            if (hasNotOfferedCourses){
                it = notOfferedOfferings.iterator();
                while (it.hasNext()){
                    io = (InstructionalOffering) it.next();
                    offeringIds.add(io.getUniqueId());
                    pdfAddInstrOffrRowsToTable(classAssignment, io, subjectAreaId, user);            	
                }
            } else {
                if(displayHeader)
                	iDocument.add(new Paragraph("All courses are currently being offered for this subject.", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, Color.BLACK)));
            }
            
    		iDocument.add(iPdfTable);
        }
        
		
		iDocument.close();

		return file;
    	} catch (Exception e) {
    		Debug.error(e);
    	} finally {
        	try {
        		if (out!=null) out.close();
        	} catch (IOException e) {}
    	}
    	return null;
    }

}