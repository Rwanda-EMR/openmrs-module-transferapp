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

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.transferapp.api.dao.MaternityTransferDao;
import org.openmrs.module.transferapp.model.MaternityTransfer;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HibernateMaternityTransferDao implements MaternityTransferDao {

	private DbSessionFactory sessionFactory;

	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public MaternityTransfer saveMaternityTransfer(MaternityTransfer maternityTransfer) {
		getSession().saveOrUpdate(maternityTransfer);
		getSession().flush();
		return maternityTransfer;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<MaternityTransfer> getMaternityTransfersByPatient(Patient patient) {
		return getMaternityTransfersByPatient(patient, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<MaternityTransfer> getMaternityTransfersByPatient(Patient patient, Integer limit) {
		Criteria criteria = getSession().createCriteria(MaternityTransfer.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("voided", false));
		criteria.addOrder(Order.desc("dateCreated"));
		if (limit != null && limit.intValue() > 0) {
			criteria.setMaxResults(limit.intValue());
		}
		return criteria.list();
	}

	@Override
	public int countMaternityTransfersByPatient(Patient patient) {
		if (patient == null) {
			return 0;
		}
		Criteria criteria = getSession().createCriteria(MaternityTransfer.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("voided", false));
		criteria.setProjection(Projections.rowCount());
		Number count = (Number) criteria.uniqueResult();
		return count == null ? 0 : count.intValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<MaternityTransfer> getOutboundMaternityTransfersBySendingFacility(String sendingFacility,
			Integer patientId, Integer limit) {
		return getOutboundMaternityTransfersBySendingFacility(sendingFacility, patientId, limit, null, null, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<MaternityTransfer> getOutboundMaternityTransfersBySendingFacility(String sendingFacility,
			Integer patientId, Integer limit, Date startDate, Date endDate, String receivingFacilityCode) {
		if (StringUtils.isBlank(sendingFacility)) {
			return Collections.emptyList();
		}

		Criteria criteria = getSession().createCriteria(MaternityTransfer.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.add(Restrictions.eq("sendingFacility", sendingFacility.trim()));
		if (patientId != null) {
			criteria.createAlias("patient", "patientAlias");
			criteria.add(Restrictions.eq("patientAlias.patientId", patientId));
		}
		if (startDate != null) {
			criteria.add(Restrictions.sqlRestriction(
					"COALESCE({alias}.decision_to_transfer_at, {alias}.date_created) >= ?",
					startDate,
					StandardBasicTypes.TIMESTAMP));
		}
		if (endDate != null) {
			criteria.add(Restrictions.sqlRestriction(
					"COALESCE({alias}.decision_to_transfer_at, {alias}.date_created) <= ?",
					endDate,
					StandardBasicTypes.TIMESTAMP));
		}
		if (StringUtils.isNotBlank(receivingFacilityCode)) {
			criteria.add(Restrictions.eq("receivingFacilityCode", receivingFacilityCode.trim()));
		}
		criteria.addOrder(Order.desc("decisionToTransferAt"));
		criteria.addOrder(Order.desc("dateCreated"));
		if (limit != null && limit.intValue() > 0) {
			criteria.setMaxResults(limit.intValue());
		}
		return criteria.list();
	}

	@Override
	public int countOutboundMaternityTransfers(String sendingFacility, Date fromDate) {
		if (StringUtils.isBlank(sendingFacility)) {
			return 0;
		}

		Criteria criteria = getSession().createCriteria(MaternityTransfer.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.add(Restrictions.eq("sendingFacility", sendingFacility.trim()));
		if (fromDate != null) {
			criteria.add(Restrictions.or(
					Restrictions.and(
							Restrictions.isNotNull("decisionToTransferAt"),
							Restrictions.ge("decisionToTransferAt", fromDate)),
					Restrictions.and(
							Restrictions.isNull("decisionToTransferAt"),
							Restrictions.ge("dateCreated", fromDate))));
		}
		criteria.setProjection(Projections.rowCount());
		Number count = (Number) criteria.uniqueResult();
		return count == null ? 0 : count.intValue();
	}

	@Override
	public int countOutboundMaternityTransfers(String sendingFacility, Date fromDate, Boolean hieSent) {
		if (StringUtils.isBlank(sendingFacility)) {
			return 0;
		}

		Criteria criteria = getSession().createCriteria(MaternityTransfer.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.add(Restrictions.eq("sendingFacility", sendingFacility.trim()));
		if (fromDate != null) {
			criteria.add(Restrictions.or(
					Restrictions.and(
							Restrictions.isNotNull("decisionToTransferAt"),
							Restrictions.ge("decisionToTransferAt", fromDate)),
					Restrictions.and(
							Restrictions.isNull("decisionToTransferAt"),
							Restrictions.ge("dateCreated", fromDate))));
		}
		if (hieSent != null) {
			if (Boolean.TRUE.equals(hieSent)) {
				criteria.add(Restrictions.eq("hieSent", true));
			} else {
				criteria.add(Restrictions.or(
						Restrictions.eq("hieSent", false),
						Restrictions.isNull("hieSent")));
			}
		}
		criteria.setProjection(Projections.rowCount());
		Number count = (Number) criteria.uniqueResult();
		return count == null ? 0 : count.intValue();
	}

	@Override
	public MaternityTransfer getMaternityTransferByUuid(String uuid) {
		Criteria criteria = getSession().createCriteria(MaternityTransfer.class);
		criteria.add(Restrictions.eq("uuid", uuid));
		criteria.add(Restrictions.eq("voided", false));
		return (MaternityTransfer) criteria.uniqueResult();
	}

	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}

}
