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
package ca.openosp.openo.registration.web;

import ca.openosp.openo.registration.dao.RegistrationModuleConfigDao;
import ca.openosp.openo.registration.dao.RegistrationTokenDao;
import ca.openosp.openo.registration.model.RegistrationModuleConfig;
import ca.openosp.openo.registration.model.RegistrationToken;
import ca.openosp.openo.registration.service.RateLimitService;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;

/**
 * Servlet that handles the QR code scan entry point for patient registration.
 *
 * <p>When a patient scans the clinic's static QR code, they are directed to this servlet.
 * The servlet generates a unique session token and redirects the patient to the
 * registration form with that token.</p>
 *
 * <h3>Flow:</h3>
 * <ol>
 *   <li>Patient scans QR code pointing to /registration/start</li>
 *   <li>This servlet generates a unique 64-character session token</li>
 *   <li>Token is saved to database with 30-minute expiry</li>
 *   <li>Patient is redirected to /registration/form.jsp?token={token}</li>
 * </ol>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
public class RegistrationStartServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger(RegistrationStartServlet.class);

    private static final int TOKEN_LENGTH = 64;
    private static final int SESSION_EXPIRY_MINUTES = 30;
    private static final String HEX_CHARS = "0123456789abcdef";

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);

        logger.debug("Registration start request from IP: {}", clientIp);

        try {
            // Check rate limiting
            RateLimitService rateLimitService = SpringUtils.getBean(RateLimitService.class);
            if (rateLimitService != null && !rateLimitService.isSessionAllowed(clientIp)) {
                logger.warn("Rate limit exceeded for IP: {}", clientIp);
                response.sendRedirect(request.getContextPath() + "/registration/expired.jsp?reason=rate_limit");
                return;
            }

            // Get facility ID (default to 1 if not available)
            Integer facilityId = getFacilityId(request);

            // Check if registration module is enabled for this facility
            RegistrationModuleConfigDao configDao = SpringUtils.getBean(RegistrationModuleConfigDao.class);
            if (configDao != null && !configDao.isModuleEnabled(facilityId)) {
                logger.warn("Registration module not enabled for facility: {}", facilityId);
                response.sendRedirect(request.getContextPath() + "/registration/expired.jsp?reason=disabled");
                return;
            }

            // Generate unique token
            String token = generateSecureToken();

            // Calculate expiry time (30 minutes from now)
            Calendar cal = Calendar.getInstance();
            Date createdAt = cal.getTime();
            cal.add(Calendar.MINUTE, SESSION_EXPIRY_MINUTES);
            Date expiresAt = cal.getTime();

            // Create and save token
            RegistrationToken registrationToken = new RegistrationToken();
            registrationToken.setToken(token);
            registrationToken.setCreatedAt(createdAt);
            registrationToken.setExpiresAt(expiresAt);
            registrationToken.setFacilityId(facilityId);
            registrationToken.setIpAddress(clientIp);
            registrationToken.setUsed(false);

            RegistrationTokenDao tokenDao = SpringUtils.getBean(RegistrationTokenDao.class);
            tokenDao.persist(registrationToken);

            // Record session creation for rate limiting
            if (rateLimitService != null) {
                rateLimitService.recordSession(clientIp);
            }

            logger.info("Created registration session token for IP: {}, expires: {}",
                    clientIp, expiresAt);

            // Redirect to registration form
            String formUrl = request.getContextPath() + "/registration/form.jsp?token=" + token;
            response.sendRedirect(formUrl);

        } catch (Exception e) {
            logger.error("Error creating registration session", e);
            response.sendRedirect(request.getContextPath() + "/registration/expired.jsp?reason=error");
        }
    }

    /**
     * Generates a cryptographically secure random token.
     *
     * @return String a 64-character hexadecimal token
     */
    private String generateSecureToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        byte[] randomBytes = new byte[TOKEN_LENGTH / 2];
        secureRandom.nextBytes(randomBytes);

        for (byte b : randomBytes) {
            token.append(HEX_CHARS.charAt((b & 0xF0) >> 4));
            token.append(HEX_CHARS.charAt(b & 0x0F));
        }

        return token.toString();
    }

    /**
     * Gets the client IP address, considering X-Forwarded-For header.
     *
     * @param request HttpServletRequest the incoming request
     * @return String the client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Gets the facility ID from the request context.
     *
     * @param request HttpServletRequest the incoming request
     * @return Integer the facility ID (defaults to 1)
     */
    private Integer getFacilityId(HttpServletRequest request) {
        // Try to get from request parameter (if provided)
        String facilityParam = request.getParameter("facility");
        if (facilityParam != null) {
            try {
                return Integer.parseInt(facilityParam);
            } catch (NumberFormatException e) {
                logger.warn("Invalid facility parameter: {}", facilityParam);
            }
        }

        // Default facility ID
        return 1;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
