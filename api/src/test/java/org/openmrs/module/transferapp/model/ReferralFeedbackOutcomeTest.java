/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 */
package org.openmrs.module.transferapp.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ReferralFeedbackOutcomeTest {

	@Test
	public void fromStoredValueAcceptsCodeAndLabel() {
		assertEquals(ReferralFeedbackOutcome.STABILIZED_CURED,
				ReferralFeedbackOutcome.fromStoredValue("STABILIZED_CURED"));
		assertEquals(ReferralFeedbackOutcome.TO_BE_FOLLOWED_UP,
				ReferralFeedbackOutcome.fromStoredValue("To be followed up"));
		assertEquals(ReferralFeedbackOutcome.REFERRED_TO_HIGH_LEVEL,
				ReferralFeedbackOutcome.fromStoredValue("referred to high level"));
		assertEquals(ReferralFeedbackOutcome.STABILIZED_CURED,
				ReferralFeedbackOutcome.fromStoredValue("STABILIZED"));
		assertEquals(ReferralFeedbackOutcome.STABILIZED_CURED,
				ReferralFeedbackOutcome.fromStoredValue("Stabilized"));
		assertNull(ReferralFeedbackOutcome.fromStoredValue(""));
		assertNull(ReferralFeedbackOutcome.fromStoredValue(null));
	}
}
