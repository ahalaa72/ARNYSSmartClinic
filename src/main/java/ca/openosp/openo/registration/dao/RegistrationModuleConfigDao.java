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

import java.util.List;

/**
 * Data Access Object interface for RegistrationModuleConfig entity.
 *
 * <p>Provides CRUD operations for managing registration module configuration
 * per facility.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
public interface RegistrationModuleConfigDao {

    /**
     * Finds a configuration by its primary key.
     *
     * @param id Integer the config ID
     * @return RegistrationModuleConfig the config entity, or null if not found
     */
    RegistrationModuleConfig find(Integer id);

    /**
     * Finds the configuration for a specific facility.
     *
     * @param facilityId Integer the facility ID
     * @return RegistrationModuleConfig the config for the facility, or null if not found
     */
    RegistrationModuleConfig findByFacilityId(Integer facilityId);

    /**
     * Gets the configuration for a facility, creating a default one if it doesn't exist.
     *
     * @param facilityId Integer the facility ID
     * @return RegistrationModuleConfig the config for the facility
     */
    RegistrationModuleConfig getOrCreateForFacility(Integer facilityId);

    /**
     * Finds all configurations.
     *
     * @return List of all RegistrationModuleConfig entities
     */
    List<RegistrationModuleConfig> findAll();

    /**
     * Finds all enabled configurations.
     *
     * @return List of enabled RegistrationModuleConfig entities
     */
    List<RegistrationModuleConfig> findAllEnabled();

    /**
     * Persists a new configuration to the database.
     *
     * @param entity RegistrationModuleConfig the entity to persist
     */
    void persist(RegistrationModuleConfig entity);

    /**
     * Merges changes to an existing configuration.
     *
     * @param entity RegistrationModuleConfig the entity to merge
     * @return RegistrationModuleConfig the merged entity
     */
    RegistrationModuleConfig merge(RegistrationModuleConfig entity);

    /**
     * Removes a configuration from the database.
     *
     * @param entity RegistrationModuleConfig the entity to remove
     */
    void remove(RegistrationModuleConfig entity);

    /**
     * Checks if the module is enabled for a facility.
     *
     * @param facilityId Integer the facility ID
     * @return boolean true if the module is enabled
     */
    boolean isModuleEnabled(Integer facilityId);
}
