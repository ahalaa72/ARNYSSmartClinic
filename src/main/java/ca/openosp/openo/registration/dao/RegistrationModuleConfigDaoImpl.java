/**
 * Copyright (c) 2025. OpenOSP. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for OpenOSP - Open Source Patient Registration Module.
 *
 * @since 2025-12-29
 */
package ca.openosp.openo.registration.dao;

import ca.openosp.openo.registration.model.RegistrationModuleConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Implementation of RegistrationModuleConfigDao using JPA/Hibernate.
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Repository
@Transactional
public class RegistrationModuleConfigDaoImpl implements RegistrationModuleConfigDao {

    private static final Logger logger = LogManager.getLogger(RegistrationModuleConfigDaoImpl.class);

    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager entityManager;

    @Override
    public RegistrationModuleConfig find(Integer id) {
        return entityManager.find(RegistrationModuleConfig.class, id);
    }

    @Override
    public RegistrationModuleConfig findByFacilityId(Integer facilityId) {
        try {
            TypedQuery<RegistrationModuleConfig> query = entityManager.createQuery(
                    "SELECT c FROM RegistrationModuleConfig c WHERE c.facilityId = :facilityId",
                    RegistrationModuleConfig.class);
            query.setParameter("facilityId", facilityId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public RegistrationModuleConfig getOrCreateForFacility(Integer facilityId) {
        RegistrationModuleConfig config = findByFacilityId(facilityId);
        if (config == null) {
            config = new RegistrationModuleConfig(facilityId);
            // Set default mandatory fields
            config.setMandatoryFields(
                    "[\"first_name\",\"last_name\",\"year_of_birth\",\"month_of_birth\"," +
                            "\"date_of_birth\",\"sex\",\"address\",\"city\",\"province\"," +
                            "\"postal\",\"cell_phone\",\"email\"]");
            persist(config);
            logger.info("Created default configuration for facility: {}", facilityId);
        }
        return config;
    }

    @Override
    public List<RegistrationModuleConfig> findAll() {
        TypedQuery<RegistrationModuleConfig> query = entityManager.createQuery(
                "SELECT c FROM RegistrationModuleConfig c ORDER BY c.facilityId",
                RegistrationModuleConfig.class);
        return query.getResultList();
    }

    @Override
    public List<RegistrationModuleConfig> findAllEnabled() {
        TypedQuery<RegistrationModuleConfig> query = entityManager.createQuery(
                "SELECT c FROM RegistrationModuleConfig c " +
                        "WHERE c.moduleEnabled = true ORDER BY c.facilityId",
                RegistrationModuleConfig.class);
        return query.getResultList();
    }

    @Override
    public void persist(RegistrationModuleConfig entity) {
        entityManager.persist(entity);
        logger.debug("Persisted RegistrationModuleConfig for facility: {}", entity.getFacilityId());
    }

    @Override
    public RegistrationModuleConfig merge(RegistrationModuleConfig entity) {
        return entityManager.merge(entity);
    }

    @Override
    public void remove(RegistrationModuleConfig entity) {
        if (entityManager.contains(entity)) {
            entityManager.remove(entity);
        } else {
            RegistrationModuleConfig attached = find(entity.getId());
            if (attached != null) {
                entityManager.remove(attached);
            }
        }
    }

    @Override
    public boolean isModuleEnabled(Integer facilityId) {
        RegistrationModuleConfig config = findByFacilityId(facilityId);
        return config != null && Boolean.TRUE.equals(config.getModuleEnabled());
    }
}
