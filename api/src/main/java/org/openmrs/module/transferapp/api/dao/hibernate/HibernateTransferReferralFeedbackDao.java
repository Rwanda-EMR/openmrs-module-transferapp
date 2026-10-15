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
package org.openmrs.module.transferapp.api.dao.hibernate;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.transferapp.api.dao.TransferReferralFeedbackDao;
import org.openmrs.module.transferapp.model.TransferReferralFeedback;

import java.util.Date;

public class HibernateTransferReferralFeedbackDao implements TransferReferralFeedbackDao {

	private DbSessionFactory sessionFactory;

	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public TransferReferralFeedback save(TransferReferralFeedback feedback) {
		getSession().saveOrUpdate(feedback);
		getSession().flush();
		return feedback;
	}

	@Override
	public TransferReferralFeedback getByPatientAndHieTransferId(org.openmrs.Patient patient, String hieTransferId) {
		if (patient == null || StringUtils.isBlank(hieTransferId)) {
			return null;
		}
		Criteria criteria = getSession().createCriteria(TransferReferralFeedback.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("hieTransferId", hieTransferId.trim()));
		criteria.add(Restrictions.eq("voided", false));
		criteria.setMaxResults(1);
		@SuppressWarnings("unchecked")
		java.util.List<TransferReferralFeedback> matches = criteria.list();
		return matches == null || matches.isEmpty() ? null : matches.get(0);
	}

	@Override
	public TransferReferralFeedback getByPatientAndHieTransferId(Integer patientId, String hieTransferId) {
		if (patientId == null || StringUtils.isBlank(hieTransferId)) {
			return null;
		}
		Criteria criteria = getSession().createCriteria(TransferReferralFeedback.class);
		criteria.createAlias("patient", "patientAlias");
		criteria.add(Restrictions.eq("patientAlias.patientId", patientId));
		criteria.add(Restrictions.eq("hieTransferId", hieTransferId.trim()));
		criteria.add(Restrictions.eq("voided", false));
		criteria.setMaxResults(1);
		@SuppressWarnings("unchecked")
		java.util.List<TransferReferralFeedback> matches = criteria.list();
		return matches == null || matches.isEmpty() ? null : matches.get(0);
	}

	@Override
	public int countFeedback(boolean hieSentOnly) {
		return countFeedback(hieSentOnly, null, null);
	}

	@Override
	public int countFeedback(boolean hieSentOnly, Date fromDate, Date toDate) {
		Criteria criteria = getSession().createCriteria(TransferReferralFeedback.class);
		criteria.add(Restrictions.eq("voided", false));
		if (hieSentOnly) {
			criteria.add(Restrictions.eq("hieSent", true));
		}
		if (fromDate != null) {
			criteria.add(Restrictions.ge("dateCreated", fromDate));
		}
		if (toDate != null) {
			criteria.add(Restrictions.le("dateCreated", toDate));
		}
		criteria.setProjection(Projections.rowCount());
		Number count = (Number) criteria.uniqueResult();
		return count != null ? count.intValue() : 0;
	}

	@Override
	public java.util.Set<String> listHieSentFeedbackKeys() {
		return listFeedbackKeys(true);
	}

	@Override
	public java.util.Set<String> listFeedbackKeys() {
		return listFeedbackKeys(null);
	}

	@SuppressWarnings("unchecked")
	private java.util.Set<String> listFeedbackKeys(Boolean hieSentOnly) {
		Criteria criteria = getSession().createCriteria(TransferReferralFeedback.class);
		criteria.createAlias("patient", "patientAlias");
		criteria.add(Restrictions.eq("voided", false));
		if (Boolean.TRUE.equals(hieSentOnly)) {
			criteria.add(Restrictions.eq("hieSent", true));
		}
		criteria.setProjection(Projections.projectionList()
				.add(Projections.property("patientAlias.patientId"))
				.add(Projections.property("hieTransferId")));
		java.util.List<Object[]> rows = criteria.list();
		java.util.Set<String> keys = new java.util.HashSet<String>();
		if (rows == null || rows.isEmpty()) {
			return keys;
		}
		for (Object[] row : rows) {
			if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
				continue;
			}
			String transferId = String.valueOf(row[1]).trim();
			if (transferId.isEmpty()) {
				continue;
			}
			keys.add(row[0] + "|" + transferId);
		}
		return keys;
	}

	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
}
