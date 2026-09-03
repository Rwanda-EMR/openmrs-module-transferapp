/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.transferapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PatientTransferIdentifierDetectorTest {

	@Test
	public void detectsNationalId() {
		assertEquals(PatientTransferIdentifierDetector.Kind.NATIONAL_ID,
				PatientTransferIdentifierDetector.detect("1199080021631043"));
		assertEquals("1199080021631043",
				PatientTransferIdentifierDetector.normalize(" 1199 0800 2163 1043 "));
	}

	@Test
	public void detectsUpidWithDashes() {
		assertEquals(PatientTransferIdentifierDetector.Kind.UPID,
				PatientTransferIdentifierDetector.detect("260204-0023-8464"));
		assertEquals("260204-0023-8464",
				PatientTransferIdentifierDetector.normalize("260204-0023-8464"));
	}

	@Test
	public void detectsUpidWithoutDashes() {
		assertEquals(PatientTransferIdentifierDetector.Kind.UPID,
				PatientTransferIdentifierDetector.detect("26020400238464"));
		assertEquals("260204-0023-8464",
				PatientTransferIdentifierDetector.normalize("26020400238464"));
	}

	@Test
	public void unknownForInvalid() {
		assertEquals(PatientTransferIdentifierDetector.Kind.UNKNOWN,
				PatientTransferIdentifierDetector.detect("abc"));
		assertNull(PatientTransferIdentifierDetector.normalize("   "));
	}
}
