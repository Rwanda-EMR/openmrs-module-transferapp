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
import org.openmrs.module.transferapp.model.OrphanTransferIdObs;
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
	public Integer findPatientIdByUpid(String upid) {
		String normalized = StringUtils.trimToNull(upid);
		if (normalized == null) {
			return null;
		}
		SQLQuery query = getSession().createSQLQuery(
				"SELECT pi.patient_id AS patientId "
						+ "FROM patient_identifier pi "
						+ "INNER JOIN patient_identifier_type pit "
						+ "ON pit.patient_identifier_type_id = pi.identifier_type "
						+ "WHERE pi.voided = 0 "
						+ "AND pi.identifier = :upid "
						+ "AND (UPPER(pit.name) = 'UPID' OR UPPER(pit.name) LIKE '%UPID%') "
						+ "ORDER BY CASE WHEN UPPER(pit.name) = 'UPID' THEN 0 ELSE 1 END, pi.patient_id ASC");
		query.setString("upid", normalized);
		query.setMaxResults(1);
		query.addScalar("patientId", StandardBasicTypes.INTEGER);
		Number patientId = (Number) query.uniqueResult();
		return patientId == null ? null : Integer.valueOf(patientId.intValue());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Integer findPatientIdByPrimaryCareId(String primaryCareId) {
		String normalized = StringUtils.trimToNull(primaryCareId);
		if (normalized == null) {
			return null;
		}
		SQLQuery query = getSession().createSQLQuery(
				"SELECT pi.patient_id AS patientId "
						+ "FROM patient_identifier pi "
						+ "INNER JOIN patient_identifier_type pit "
						+ "ON pit.patient_identifier_type_id = pi.identifier_type "
						+ "WHERE pi.voided = 0 "
						+ "AND pi.identifier = :primaryCareId "
						+ "AND (UPPER(pit.name) = 'PRIMARY CARE ID' "
						+ "OR UPPER(pit.name) = 'PRIMARY CARE' "
						+ "OR UPPER(pit.name) = 'PRIMARY CARE ID TYPE' "
						+ "OR UPPER(pit.name) LIKE '%PRIMARY CARE%') "
						+ "ORDER BY CASE "
						+ "WHEN UPPER(pit.name) = 'PRIMARY CARE ID' THEN 0 "
						+ "WHEN UPPER(pit.name) = 'PRIMARY CARE' THEN 1 "
						+ "WHEN UPPER(pit.name) = 'PRIMARY CARE ID TYPE' THEN 2 "
						+ "ELSE 3 END, pi.patient_id ASC");
		query.setString("primaryCareId", normalized);
		query.setMaxResults(1);
		query.addScalar("patientId", StandardBasicTypes.INTEGER);
		Number patientId = (Number) query.uniqueResult();
		return patientId == null ? null : Integer.valueOf(patientId.intValue());
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> findVisitsWithRegistrationInRange(Date start, Date end,
			Integer registrationEncounterTypeId, Integer patientId, Integer insuranceTypeConceptId,
			Collection<Integer> insuranceValueCodedIds, Collection<String> insuranceNamePatterns,
			boolean includeMissingInsurance, int offset, int maxResults) {
		if (start == null || end == null || registrationEncounterTypeId == null || maxResults <= 0) {
			return Collections.emptyList();
		}
		int safeOffset = offset < 0 ? 0 : offset;
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT v.visit_id AS visitId, v.patient_id AS patientId, ")
				.append("v.date_started AS startDatetime, v.date_stopped AS stopDatetime ")
				.append("FROM visit v ")
				.append("INNER JOIN encounter e ON e.visit_id = v.visit_id AND e.voided = 0 ")
				.append("WHERE v.voided = 0 ")
				.append("AND v.date_started >= :start ")
				.append("AND v.date_started <= :end ")
				.append("AND e.encounter_type = :regType ");
		if (patientId != null) {
			sql.append("AND v.patient_id = :patientId ");
		}
		appendInsuranceTypeFilter(sql, insuranceTypeConceptId, insuranceValueCodedIds, insuranceNamePatterns,
				includeMissingInsurance);
		sql.append("ORDER BY v.date_started DESC");
		SQLQuery query = getSession().createSQLQuery(sql.toString());
		bindVisitRangeParams(query, start, end, registrationEncounterTypeId, patientId);
		bindInsuranceTypeParams(query, insuranceTypeConceptId, insuranceValueCodedIds, insuranceNamePatterns,
				includeMissingInsurance);
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
	public int countVisitsWithRegistrationInRange(Date start, Date end, Integer registrationEncounterTypeId,
			Integer patientId, Integer insuranceTypeConceptId, Collection<Integer> insuranceValueCodedIds,
			Collection<String> insuranceNamePatterns, boolean includeMissingInsurance) {
		if (start == null || end == null || registrationEncounterTypeId == null) {
			return 0;
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(DISTINCT v.visit_id) AS totalCount ")
				.append("FROM visit v ")
				.append("INNER JOIN encounter e ON e.visit_id = v.visit_id AND e.voided = 0 ")
				.append("WHERE v.voided = 0 ")
				.append("AND v.date_started >= :start ")
				.append("AND v.date_started <= :end ")
				.append("AND e.encounter_type = :regType ");
		if (patientId != null) {
			sql.append("AND v.patient_id = :patientId ");
		}
		appendInsuranceTypeFilter(sql, insuranceTypeConceptId, insuranceValueCodedIds, insuranceNamePatterns,
				includeMissingInsurance);
		SQLQuery query = getSession().createSQLQuery(sql.toString());
		bindVisitRangeParams(query, start, end, registrationEncounterTypeId, patientId);
		bindInsuranceTypeParams(query, insuranceTypeConceptId, insuranceValueCodedIds, insuranceNamePatterns,
				includeMissingInsurance);
		query.addScalar("totalCount", StandardBasicTypes.LONG);
		Number count = (Number) query.uniqueResult();
		return count == null ? 0 : count.intValue();
	}

	private static void bindVisitRangeParams(SQLQuery query, Date start, Date end, Integer registrationEncounterTypeId,
			Integer patientId) {
		query.setTimestamp("start", start);
		query.setTimestamp("end", end);
		query.setInteger("regType", registrationEncounterTypeId);
		if (patientId != null) {
			query.setInteger("patientId", patientId);
		}
	}

	private static boolean hasInsuranceFilter(Integer insuranceTypeConceptId,
			Collection<Integer> insuranceValueCodedIds, Collection<String> insuranceNamePatterns,
			boolean includeMissingInsurance) {
		if (insuranceTypeConceptId == null) {
			return false;
		}
		boolean hasCoded = insuranceValueCodedIds != null && !insuranceValueCodedIds.isEmpty();
		boolean hasPatterns = insuranceNamePatterns != null && !insuranceNamePatterns.isEmpty();
		return hasCoded || hasPatterns || includeMissingInsurance;
	}

	private static void appendInsuranceTypeFilter(StringBuilder sql, Integer insuranceTypeConceptId,
			Collection<Integer> insuranceValueCodedIds, Collection<String> insuranceNamePatterns,
			boolean includeMissingInsurance) {
		if (!hasInsuranceFilter(insuranceTypeConceptId, insuranceValueCodedIds, insuranceNamePatterns,
				includeMissingInsurance)) {
			return;
		}
		boolean hasCoded = insuranceValueCodedIds != null && !insuranceValueCodedIds.isEmpty();
		boolean hasPatterns = insuranceNamePatterns != null && !insuranceNamePatterns.isEmpty();
		sql.append("AND (");
		if (hasCoded || hasPatterns) {
			sql.append("EXISTS (")
					.append("SELECT 1 FROM encounter ie ")
					.append("INNER JOIN obs io ON io.encounter_id = ie.encounter_id AND io.voided = 0 ")
					.append("LEFT JOIN concept_name icn ON icn.concept_id = io.value_coded ")
					.append("AND icn.voided = 0 ")
					.append("WHERE ie.visit_id = v.visit_id AND ie.voided = 0 ")
					.append("AND ie.encounter_type = :regType ")
					.append("AND io.concept_id = :insuranceTypeConceptId ")
					.append("AND (");
			boolean firstMatch = true;
			if (hasCoded) {
				sql.append("io.value_coded IN (:insuranceValueCodedIds)");
				firstMatch = false;
			}
			if (hasPatterns) {
				int index = 0;
				for (String ignored : insuranceNamePatterns) {
					if (!firstMatch) {
						sql.append(" OR ");
					}
					sql.append("LOWER(icn.name) LIKE :insuranceNamePattern").append(index);
					firstMatch = false;
					index++;
				}
			}
			sql.append(")) ");
		}
		if (includeMissingInsurance) {
			if (hasCoded || hasPatterns) {
				sql.append("OR ");
			}
			sql.append("NOT EXISTS (")
					.append("SELECT 1 FROM encounter me ")
					.append("INNER JOIN obs mo ON mo.encounter_id = me.encounter_id AND mo.voided = 0 ")
					.append("WHERE me.visit_id = v.visit_id AND me.voided = 0 ")
					.append("AND me.encounter_type = :regType ")
					.append("AND mo.concept_id = :insuranceTypeConceptId")
					.append(") ");
		}
		sql.append(") ");
	}

	private static void bindInsuranceTypeParams(SQLQuery query, Integer insuranceTypeConceptId,
			Collection<Integer> insuranceValueCodedIds, Collection<String> insuranceNamePatterns,
			boolean includeMissingInsurance) {
		if (!hasInsuranceFilter(insuranceTypeConceptId, insuranceValueCodedIds, insuranceNamePatterns,
				includeMissingInsurance)) {
			return;
		}
		query.setInteger("insuranceTypeConceptId", insuranceTypeConceptId);
		if (insuranceValueCodedIds != null && !insuranceValueCodedIds.isEmpty()) {
			query.setParameterList("insuranceValueCodedIds", insuranceValueCodedIds);
		}
		if (insuranceNamePatterns != null && !insuranceNamePatterns.isEmpty()) {
			int index = 0;
			for (String pattern : insuranceNamePatterns) {
				query.setString("insuranceNamePattern" + index, pattern);
				index++;
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> findTransferIdsOnVisitsStartedBetween(Integer patientId, Integer transferIdConceptId,
			Date rangeStart, Date beforeExclusive) {
		if (patientId == null || transferIdConceptId == null || rangeStart == null || beforeExclusive == null) {
			return Collections.emptyList();
		}
		SQLQuery query = getSession().createSQLQuery(
				"SELECT DISTINCT o.value_text AS transferId "
						+ "FROM obs o "
						+ "INNER JOIN encounter e ON e.encounter_id = o.encounter_id AND e.voided = 0 "
						+ "INNER JOIN visit v ON v.visit_id = e.visit_id AND v.voided = 0 "
						+ "WHERE o.voided = 0 "
						+ "AND o.concept_id = :conceptId "
						+ "AND v.patient_id = :patientId "
						+ "AND v.date_started >= :rangeStart "
						+ "AND v.date_started < :beforeExclusive "
						+ "AND o.value_text IS NOT NULL "
						+ "AND o.value_text <> ''");
		query.setInteger("conceptId", transferIdConceptId);
		query.setInteger("patientId", patientId);
		query.setTimestamp("rangeStart", rangeStart);
		query.setTimestamp("beforeExclusive", beforeExclusive);
		query.addScalar("transferId", StandardBasicTypes.STRING);
		List<String> rows = query.list();
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashSet<String> unique = new LinkedHashSet<String>();
		for (String row : rows) {
			String transferId = StringUtils.trimToNull(row);
			if (transferId != null) {
				unique.add(transferId);
			}
		}
		return new ArrayList<String>(unique);
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
	public Map<Integer, String[]> findInsuranceByVisitIds(Collection<Integer> visitIds,
			Integer insuranceTypeConceptId, Integer insuranceNumberConceptId,
			Integer registrationEncounterTypeId) {
		if (visitIds == null || visitIds.isEmpty()
				|| (insuranceTypeConceptId == null && insuranceNumberConceptId == null)) {
			return Collections.emptyMap();
		}
		List<Integer> conceptIds = new ArrayList<Integer>(2);
		if (insuranceTypeConceptId != null) {
			conceptIds.add(insuranceTypeConceptId);
		}
		if (insuranceNumberConceptId != null && !conceptIds.contains(insuranceNumberConceptId)) {
			conceptIds.add(insuranceNumberConceptId);
		}
		Map<Integer, String[]> result = new LinkedHashMap<Integer, String[]>();
		List<Integer> idList = new ArrayList<Integer>(new LinkedHashSet<Integer>(visitIds));
		for (int offset = 0; offset < idList.size(); offset += IN_CLAUSE_BATCH) {
			List<Integer> batch = idList.subList(offset, Math.min(offset + IN_CLAUSE_BATCH, idList.size()));
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT e.visit_id AS visitId, o.concept_id AS questionConceptId, ")
					.append("o.value_coded AS valueCoded, o.value_text AS valueText, ")
					.append("cn.name AS valueCodedName ")
					.append("FROM obs o ")
					.append("INNER JOIN encounter e ON e.encounter_id = o.encounter_id AND e.voided = 0 ")
					.append("LEFT JOIN concept_name cn ON cn.concept_id = o.value_coded ")
					.append("AND cn.voided = 0 AND cn.locale_preferred = 1 ")
					.append("WHERE o.voided = 0 ")
					.append("AND e.visit_id IN (:visitIds) ")
					.append("AND o.concept_id IN (:conceptIds) ");
			if (registrationEncounterTypeId != null) {
				sql.append("AND e.encounter_type = :regType ");
			}
			sql.append("ORDER BY e.encounter_datetime DESC, o.obs_datetime DESC, o.obs_id DESC");
			SQLQuery query = getSession().createSQLQuery(sql.toString());
			query.setParameterList("visitIds", batch);
			query.setParameterList("conceptIds", conceptIds);
			if (registrationEncounterTypeId != null) {
				query.setInteger("regType", registrationEncounterTypeId);
			}
			query.addScalar("visitId", StandardBasicTypes.INTEGER);
			query.addScalar("questionConceptId", StandardBasicTypes.INTEGER);
			query.addScalar("valueCoded", StandardBasicTypes.INTEGER);
			query.addScalar("valueText", StandardBasicTypes.STRING);
			query.addScalar("valueCodedName", StandardBasicTypes.STRING);
			List<Object[]> rows = query.list();
			if (rows == null) {
				continue;
			}
			for (Object[] row : rows) {
				if (row == null || row.length < 5 || row[0] == null || row[1] == null) {
					continue;
				}
				Integer visitId = (Integer) row[0];
				Integer questionConceptId = (Integer) row[1];
				Integer valueCoded = row[2] instanceof Number ? ((Number) row[2]).intValue() : null;
				String valueText = StringUtils.trimToNull(row[3] != null ? String.valueOf(row[3]) : null);
				String valueCodedName = StringUtils.trimToNull(row[4] != null ? String.valueOf(row[4]) : null);

				String[] slot = result.get(visitId);
				if (slot == null) {
					slot = new String[] { "", "" };
					result.put(visitId, slot);
				}
				if (insuranceTypeConceptId != null && insuranceTypeConceptId.equals(questionConceptId)
						&& StringUtils.isBlank(slot[0])) {
					if (StringUtils.isNotBlank(valueCodedName)) {
						slot[0] = valueCodedName;
					} else if (valueCoded != null) {
						slot[0] = String.valueOf(valueCoded);
					}
				}
				if (insuranceNumberConceptId != null && insuranceNumberConceptId.equals(questionConceptId)
						&& StringUtils.isBlank(slot[1]) && valueText != null) {
					slot[1] = valueText;
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

	@Override
	@SuppressWarnings("unchecked")
	public List<OrphanTransferIdObs> findOrphanTransferIdObs(Integer transferIdConceptId, Date createdOnOrAfter,
			int maxResults) {
		if (transferIdConceptId == null || createdOnOrAfter == null || maxResults <= 0) {
			return Collections.emptyList();
		}
		SQLQuery query = getSession().createSQLQuery(
				"SELECT o.obs_id AS obsId, o.person_id AS patientId, o.value_text AS hieTransferId, "
						+ "o.date_created AS dateCreated "
						+ "FROM obs o "
						+ "INNER JOIN patient p ON p.patient_id = o.person_id AND p.voided = 0 "
						+ "WHERE o.voided = 0 "
						+ "AND o.concept_id = :conceptId "
						+ "AND o.value_text IS NOT NULL "
						+ "AND TRIM(o.value_text) <> '' "
						+ "AND o.date_created >= :createdOnOrAfter "
						+ "AND NOT EXISTS ("
						+ "  SELECT 1 FROM transfers t "
						+ "  WHERE t.voided = 0 "
						+ "  AND t.patient_id = o.person_id "
						+ "  AND t.hie_transfer_id = TRIM(o.value_text)"
						+ ") "
						+ "ORDER BY o.date_created ASC, o.obs_id ASC");
		query.setInteger("conceptId", transferIdConceptId);
		query.setTimestamp("createdOnOrAfter", createdOnOrAfter);
		query.setMaxResults(maxResults);
		query.addScalar("obsId", StandardBasicTypes.INTEGER);
		query.addScalar("patientId", StandardBasicTypes.INTEGER);
		query.addScalar("hieTransferId", StandardBasicTypes.STRING);
		query.addScalar("dateCreated", StandardBasicTypes.TIMESTAMP);
		List<Object[]> rows = query.list();
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}
		List<OrphanTransferIdObs> result = new ArrayList<OrphanTransferIdObs>();
		for (Object[] row : rows) {
			if (row == null || row.length < 4) {
				continue;
			}
			String hieTransferId = StringUtils.trimToNull(row[2] != null ? String.valueOf(row[2]) : null);
			if (hieTransferId == null) {
				continue;
			}
			OrphanTransferIdObs orphan = new OrphanTransferIdObs();
			orphan.setObsId(row[0] instanceof Number ? Integer.valueOf(((Number) row[0]).intValue()) : null);
			orphan.setPatientId(row[1] instanceof Number ? Integer.valueOf(((Number) row[1]).intValue()) : null);
			orphan.setHieTransferId(hieTransferId);
			orphan.setDateCreated(row[3] instanceof Date ? (Date) row[3] : null);
			if (orphan.getPatientId() != null) {
				result.add(orphan);
			}
		}
		return result;
	}

	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
}
