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
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.openmrs.Patient;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.transferapp.api.dao.PastTransfersDao;
import org.openmrs.module.transferapp.model.Transfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HibernatePastTransfersDao implements PastTransfersDao {

	private static final int IN_CLAUSE_BATCH = 500;

	private DbSessionFactory sessionFactory;

	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> findVisitsWithRegistrationInRange(Date start, Date end,
			Integer registrationEncounterTypeId, int offset, int maxResults) {
		if (start == null || end == null || registrationEncounterTypeId == null || maxResults <= 0) {
			return Collections.emptyList();
		}
		int safeOffset = offset < 0 ? 0 : offset;
		SQLQuery query = getSession().createSQLQuery(
				"SELECT DISTINCT v.visit_id AS visitId, v.patient_id AS patientId, "
						+ "v.date_started AS startDatetime, v.date_stopped AS stopDatetime "
						+ "FROM visit v "
						+ "INNER JOIN encounter e ON e.visit_id = v.visit_id AND e.voided = 0 "
						+ "WHERE v.voided = 0 "
						+ "AND v.date_started >= :start "
						+ "AND v.date_started <= :end "
						+ "AND e.encounter_type = :regType "
						+ "ORDER BY v.date_started DESC");
		query.setTimestamp("start", start);
		query.setTimestamp("end", end);
		query.setInteger("regType", registrationEncounterTypeId);
		query.setFirstResult(safeOffset);
		query.setMaxResults(maxResults);
		query.addScalar("visitId", StandardBasicTypes.INTEGER);
		query.addScalar("patientId", StandardBasicTypes.INTEGER);
		query.addScalar("startDatetime", StandardBasicTypes.TIMESTAMP);
		query.addScalar("stopDatetime", StandardBasicTypes.TIMESTAMP);
		List<Object[]> rows = query.list();
		return rows != null ? rows : Collections.<Object[]>emptyList();
	}

	@Override
	public int countVisitsWithRegistrationInRange(Date start, Date end, Integer registrationEncounterTypeId) {
		if (start == null || end == null || registrationEncounterTypeId == null) {
			return 0;
		}
		SQLQuery query = getSession().createSQLQuery(
				"SELECT COUNT(DISTINCT v.visit_id) AS totalCount "
						+ "FROM visit v "
						+ "INNER JOIN encounter e ON e.visit_id = v.visit_id AND e.voided = 0 "
						+ "WHERE v.voided = 0 "
						+ "AND v.date_started >= :start "
						+ "AND v.date_started <= :end "
						+ "AND e.encounter_type = :regType");
		query.setTimestamp("start", start);
		query.setTimestamp("end", end);
		query.setInteger("regType", registrationEncounterTypeId);
		query.addScalar("totalCount", StandardBasicTypes.LONG);
		Number count = (Number) query.uniqueResult();
		return count == null ? 0 : count.intValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Integer, List<String>> findTransferIdValuesByVisitIds(Collection<Integer> visitIds,
			Integer transferIdConceptId) {
		if (visitIds == null || visitIds.isEmpty() || transferIdConceptId == null) {
			return Collections.emptyMap();
		}
		Map<Integer, List<String>> result = new LinkedHashMap<Integer, List<String>>();
		List<Integer> idList = new ArrayList<Integer>(new LinkedHashSet<Integer>(visitIds));
		for (int offset = 0; offset < idList.size(); offset += IN_CLAUSE_BATCH) {
			List<Integer> batch = idList.subList(offset, Math.min(offset + IN_CLAUSE_BATCH, idList.size()));
			SQLQuery query = getSession().createSQLQuery(
					"SELECT e.visit_id AS visitId, o.value_text AS transferId "
							+ "FROM obs o "
							+ "INNER JOIN encounter e ON e.encounter_id = o.encounter_id AND e.voided = 0 "
							+ "WHERE o.voided = 0 "
							+ "AND o.concept_id = :conceptId "
							+ "AND e.visit_id IN (:visitIds) "
							+ "AND o.value_text IS NOT NULL "
							+ "AND o.value_text <> ''");
			query.setInteger("conceptId", transferIdConceptId);
			query.setParameterList("visitIds", batch);
			query.addScalar("visitId", StandardBasicTypes.INTEGER);
			query.addScalar("transferId", StandardBasicTypes.STRING);
			List<Object[]> rows = query.list();
			if (rows == null) {
				continue;
			}
			for (Object[] row : rows) {
				if (row == null || row.length < 2 || row[0] == null) {
					continue;
				}
				Integer visitId = (Integer) row[0];
				String transferId = StringUtils.trimToNull(row[1] != null ? String.valueOf(row[1]) : null);
				if (transferId == null) {
					continue;
				}
				List<String> values = result.get(visitId);
				if (values == null) {
					values = new ArrayList<String>();
					result.put(visitId, values);
				}
				if (!values.contains(transferId)) {
					values.add(transferId);
				}
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Integer, Patient> findPatientsByIds(Collection<Integer> patientIds) {
		if (patientIds == null || patientIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Integer, Patient> result = new LinkedHashMap<Integer, Patient>();
		List<Integer> idList = new ArrayList<Integer>(new LinkedHashSet<Integer>(patientIds));
		for (int offset = 0; offset < idList.size(); offset += IN_CLAUSE_BATCH) {
			List<Integer> batch = idList.subList(offset, Math.min(offset + IN_CLAUSE_BATCH, idList.size()));
			Query query = getSession().createQuery(
					"select distinct p from Patient p "
							+ "left join fetch p.names "
							+ "left join fetch p.identifiers "
							+ "where p.patientId in (:ids)");
			query.setParameterList("ids", batch);
			List<Patient> patients = query.list();
			if (patients == null) {
				continue;
			}
			for (Patient patient : patients) {
				if (patient != null && patient.getPatientId() != null) {
					result.put(patient.getPatientId(), patient);
				}
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Transfer> findTransfersByHieTransferIds(Collection<String> hieTransferIds) {
		if (hieTransferIds == null || hieTransferIds.isEmpty()) {
			return Collections.emptyList();
		}
		Set<String> normalized = new LinkedHashSet<String>();
		for (String id : hieTransferIds) {
			String value = StringUtils.trimToNull(id);
			if (value != null) {
				normalized.add(value);
			}
		}
		if (normalized.isEmpty()) {
			return Collections.emptyList();
		}
		List<Transfer> all = new ArrayList<Transfer>();
		List<String> idList = new ArrayList<String>(normalized);
		for (int offset = 0; offset < idList.size(); offset += IN_CLAUSE_BATCH) {
			List<String> batch = idList.subList(offset, Math.min(offset + IN_CLAUSE_BATCH, idList.size()));
			Criteria criteria = getSession().createCriteria(Transfer.class);
			criteria.add(Restrictions.eq("voided", false));
			criteria.add(Restrictions.in("hieTransferId", batch));
			criteria.createAlias("patient", "patientAlias");
			List<Transfer> found = criteria.list();
			if (found != null) {
				all.addAll(found);
			}
		}
		return all;
	}

	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
}
