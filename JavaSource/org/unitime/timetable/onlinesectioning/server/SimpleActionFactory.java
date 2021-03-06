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
package org.unitime.timetable.onlinesectioning.server;

import java.lang.reflect.InvocationTargetException;

import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.onlinesectioning.OnlineSectioningAction;
import org.unitime.timetable.onlinesectioning.OnlineSectioningActionFactory;

/**
 * @author Tomas Muller
 */
public class SimpleActionFactory implements OnlineSectioningActionFactory {

	@Override
	public <X extends OnlineSectioningAction> X createAction(Class<X> clazz) throws SectioningException {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (InstantiationException e) {
			throw new SectioningException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new SectioningException(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			throw new SectioningException(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new SectioningException(e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			throw new SectioningException(e.getMessage(), e);
		} catch (SecurityException e) {
			throw new SectioningException(e.getMessage(), e);
		}
	}

}
