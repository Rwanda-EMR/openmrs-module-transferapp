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
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.transferapp.api.dao.TransferDao;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HibernateTransferDao implements TransferDao {

	private DbSessionFactory sessionFactory;

	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Transfer saveTransfer(Transfer transfer) {
		getSession().saveOrUpdate(transfer);
		getSession().flush();
		return transfer;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Transfer> getTransfersByPatient(Patient patient) {
		return getTransfersByPatient(patient, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Transfer> getTransfersByPatient(Patient patient, Integer limit) {
		Criteria criteria = getSession().createCriteria(Transfer.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("voided", false));
		criteria.addOrder(Order.desc("dateCreated"));
		if (limit != null && limit.intValue() > 0) {
			criteria.setMaxResults(limit.intValue());
		}
		return criteria.list();
	}

	@Override
	public int countTransfersByPatient(Patient patient) {
		if (patient == null) {
			return 0;
		}
		Criteria criteria = getSession().createCriteria(Transfer.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("voided", false));
		criteria.setProjection(Projections.rowCount());
		Number count = (Number) criteria.uniqueResult();
		return count == null ? 0 : count.intValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Transfer> getOutboundTransfersBySendingFacility(String sendingFacility, Integer patientId, Integer limit) {
		return getOutboundTransfersBySendingFacility(sendingFacility, patientId, limit, null, null, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Transfer> getOutboundTransfersBySendingFacility(String sendingFacility, Integer patientId, Integer limit,
			Date startDate, Date endDate, String receivingFacilityCode) {
		if (StringUtils.isBlank(sendingFacility)) {
			return Collections.emptyList();
		}

		Criteria criteria = getSession().createCriteria(Transfer.class);
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
	@SuppressWarnings("unchecked")
	public List<Transfer> getAmbulanceVoucherTransfers(String sendingFacility, String ambulanceProviderFosaId,
			Date startDate, Date endDate, Integer firstResult, Integer maxResults) {
		Criteria criteria = createAmbulanceVoucherCriteria(sendingFacility, ambulanceProviderFosaId, startDate, endDate);
		if (criteria == null) {
			return Collections.emptyList();
		}
		criteria.addOrder(Order.desc("decisionToTransferAt"));
		criteria.addOrder(Order.desc("dateCreated"));
		if (firstResult != null && firstResult.intValue() > 0) {
			criteria.setFirstResult(firstResult.intValue());
		}
		if (maxResults != null && maxResults.intValue() > 0) {
			criteria.setMaxResults(maxResults.intValue());
		}
		return criteria.list();
	}

	@Override
	public int countAmbulanceVoucherTransfers(String sendingFacility, String ambulanceProviderFosaId,
			Date startDate, Date endDate) {
		Criteria criteria = createAmbulanceVoucherCriteria(sendingFacility, ambulanceProviderFosaId, startDate, endDate);
		if (criteria == null) {
			return 0;
		}
		criteria.setProjection(Projections.rowCount());
		Number count = (Number) criteria.uniqueResult();
		return count == null ? 0 : count.intValue();
	}

	private Criteria createAmbulanceVoucherCriteria(String sendingFacility, String ambulanceProviderFosaId,
			Date startDate, Date endDate) {
		String facility = StringUtils.trimToNull(sendingFacility);
		String providerFosaId = StringUtils.trimToNull(ambulanceProviderFosaId);
		if (facility == null && providerFosaId == null) {
			return null;
		}
		Criteria criteria = getSession().createCriteria(Transfer.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.add(Restrictions.isNotNull("ambulanceConsommationId"));
		if (facility != null && providerFosaId != null) {
			criteria.add(Restrictions.or(
					Restrictions.eq("sendingFacility", facility),
					Restrictions.eq("ambulanceProviderFosaId", providerFosaId)));
		} else if (facility != null) {
			criteria.add(Restrictions.eq("sendingFacility", facility));
		} else {
			criteria.add(Restrictions.eq("ambulanceProviderFosaId", providerFosaId));
		}
		if (startDate != null) {
			criteria.add(Restrictions.ge("dateCreated", startDate));
		}
		if (endDate != null) {
			criteria.add(Restrictions.le("dateCreated", endDate));
		}
		return criteria;
	}

	@Override
	public int countOutboundTransfers(String sendingFacility, Date fromDate, Boolean hieSent) {
		if (StringUtils.isBlank(sendingFacility)) {
			return 0;
		}

		Criteria criteria = getSession().createCriteria(Transfer.class);
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
	public Transfer getTransferByUuid(String uuid) {
		Criteria criteria = getSession().createCriteria(Transfer.class);
		criteria.add(Restrictions.eq("uuid", uuid));
		criteria.add(Restrictions.eq("voided", false));
		return (Transfer) criteria.uniqueResult();
	}

	@Override
	public Transfer getTransferByHieTransferId(Integer patientId, String hieTransferId) {
		if (patientId == null || StringUtils.isBlank(hieTransferId)) {
			return null;
		}
		Criteria criteria = getSession().createCriteria(Transfer.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.add(Restrictions.eq("hieTransferId", hieTransferId.trim()));
		criteria.createAlias("patient", "patientAlias");
		criteria.add(Restrictions.eq("patientAlias.patientId", patientId));
		criteria.setMaxResults(1);
		return (Transfer) criteria.uniqueResult();
	}

	@Override
	public PersonAddress getPreferredPersonAddress(Integer personId) {
		if (personId == null) {
			return null;
		}

		Criteria criteria = getSession().createCriteria(PersonAddress.class);
		criteria.add(Restrictions.eq("person.personId", personId));
		criteria.add(Restrictions.eq("voided", false));
		criteria.addOrder(Order.desc("preferred"));
		criteria.addOrder(Order.desc("personAddressId"));
		criteria.setMaxResults(1);
		return (PersonAddress) criteria.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PersonAttribute> getPersonAttributes(Integer personId) {
		if (personId == null) {
			return Collections.emptyList();
		}

		Criteria criteria = getSession().createCriteria(PersonAttribute.class);
		criteria.add(Restrictions.eq("person.personId", personId));
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}

	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}

}
