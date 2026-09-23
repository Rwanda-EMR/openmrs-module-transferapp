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
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.transferapp.api.dao.ApproverDao;
import org.openmrs.module.transferapp.model.Approver;

import java.util.List;

public class HibernateApproverDao implements ApproverDao {

	private DbSessionFactory sessionFactory;

	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Approver> getApprovers(boolean includeVoided) {
		Criteria criteria = getSession().createCriteria(Approver.class);
		if (!includeVoided) {
			criteria.add(Restrictions.eq("voided", false));
		}
		criteria.addOrder(Order.asc("approverId"));
		return criteria.list();
	}

	@Override
	public Approver getApprover(Integer approverId) {
		if (approverId == null) {
			return null;
		}
		return (Approver) getSession().get(Approver.class, approverId);
	}

	@Override
	public Approver getApproverByUserId(Integer userId) {
		if (userId == null) {
			return null;
		}
		Criteria criteria = getSession().createCriteria(Approver.class);
		criteria.add(Restrictions.eq("userId", userId));
		criteria.addOrder(Order.asc("voided"));
		criteria.addOrder(Order.desc("approverId"));
		criteria.setMaxResults(1);
		return (Approver) criteria.uniqueResult();
	}

	@Override
	public Approver saveApprover(Approver approver) {
		getSession().saveOrUpdate(approver);
		return approver;
	}

	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
}
