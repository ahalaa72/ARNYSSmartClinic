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
package ca.openosp.openo.registration.service;

import ca.openosp.openo.registration.model.PatientRegistrationQueue;
import ca.openosp.openo.registration.model.RegistrationModuleConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for sending registration notification emails.
 *
 * <p>Supports multi-language email notifications including RTL languages (Arabic).
 * Sends approval and rejection emails to patients in their preferred language.</p>
 *
 * <p>Supported languages: English (en), French (fr), Arabic (ar), Hindi (hi), Mandarin (zh)</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Service
public class RegistrationNotificationService {

    private static final Logger logger = LogManager.getLogger(RegistrationNotificationService.class);

    /** Template base path */
    private static final String TEMPLATE_BASE_PATH = "/email-templates/registration/";

    /** Supported language codes */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "fr", "ar", "hi", "zh");

    /** RTL languages */
    private static final Set<String> RTL_LANGUAGES = Set.of("ar");

    /** Email enabled flag */
    private boolean enabled = true;

    /** From address */
    private String fromAddress = "noreply@clinic.example.com";

    /** From name */
    private String fromName = "Patient Registration";

    /** Subject lines by language */
    private static final Map<String, String> APPROVAL_SUBJECTS = Map.of(
            "en", "Your Registration at %s is Confirmed",
            "fr", "Votre inscription \u00e0 %s est confirm\u00e9e",
            "ar", "\u062a\u0645 \u062a\u0623\u0643\u064a\u062f \u062a\u0633\u062c\u064a\u0644\u0643 \u0641\u064a %s",
            "hi", "%s \u092e\u0947\u0902 \u0906\u092a\u0915\u093e \u092a\u0902\u091c\u0940\u0915\u0930\u0923 \u092a\u0941\u0937\u094d\u091f \u0939\u094b \u0917\u092f\u093e \u0939\u0948",
            "zh", "\u60a8\u5728%s\u7684\u6ce8\u518c\u5df2\u786e\u8ba4"
    );

    private static final Map<String, String> REJECTION_SUBJECTS = Map.of(
            "en", "Update on Your Registration at %s",
            "fr", "Mise \u00e0 jour concernant votre inscription \u00e0 %s",
            "ar", "\u062a\u062d\u062f\u064a\u062b \u0628\u0634\u0623\u0646 \u062a\u0633\u062c\u064a\u0644\u0643 \u0641\u064a %s",
            "hi", "%s \u092e\u0947\u0902 \u0906\u092a\u0915\u0947 \u092a\u0902\u091c\u0940\u0915\u0930\u0923 \u0915\u0947 \u092c\u093e\u0930\u0947 \u092e\u0947\u0902 \u0905\u092a\u0921\u0947\u091f",
            "zh", "\u5173\u4e8e\u60a8\u5728%s\u6ce8\u518c\u7684\u66f4\u65b0"
    );

    // ========================================================================
    // Public Methods
    // ========================================================================

    /**
     * Sends an approval email to the patient.
     *
     * @param queue PatientRegistrationQueue the approved registration
     * @param config RegistrationModuleConfig the clinic configuration
     */
    public void sendPatientApprovalEmail(PatientRegistrationQueue queue, RegistrationModuleConfig config) {
        if (!enabled || StringUtils.isBlank(queue.getEmail())) {
            logger.debug("Approval email not sent - disabled or no email address");
            return;
        }

        String language = mapSpokenLangToCode(queue.getSpokenLang());
        String clinicName = config != null ? config.getClinicName() : "Medical Clinic";

        String subject = String.format(getApprovalSubject(language), clinicName);
        String body = buildApprovalEmailBody(queue, config, language);

        sendEmail(queue.getEmail(), subject, body, language);
        logger.info("Sent approval email to patient for registration {}", queue.getId());
    }

    /**
     * Sends a rejection email to the patient.
     *
     * @param queue PatientRegistrationQueue the rejected registration
     * @param config RegistrationModuleConfig the clinic configuration
     */
    public void sendPatientRejectionEmail(PatientRegistrationQueue queue, RegistrationModuleConfig config) {
        if (!enabled || StringUtils.isBlank(queue.getEmail())) {
            logger.debug("Rejection email not sent - disabled or no email address");
            return;
        }

        String language = mapSpokenLangToCode(queue.getSpokenLang());
        String clinicName = config != null ? config.getClinicName() : "Medical Clinic";

        String subject = String.format(getRejectionSubject(language), clinicName);
        String body = buildRejectionEmailBody(queue, config, language);

        sendEmail(queue.getEmail(), subject, body, language);
        logger.info("Sent rejection email to patient for registration {}", queue.getId());
    }

    /**
     * Sends a staff notification about a new registration.
     *
     * @param queue PatientRegistrationQueue the new registration
     * @param config RegistrationModuleConfig the clinic configuration
     */
    public void notifyStaffNewRegistration(PatientRegistrationQueue queue, RegistrationModuleConfig config) {
        if (!enabled || config == null || StringUtils.isBlank(config.getStaffNotificationEmail())) {
            return;
        }

        String subject = "New Patient Registration Pending Review";
        String body = buildStaffNotificationBody(queue, config);

        sendEmail(config.getStaffNotificationEmail(), subject, body, "en");
        logger.info("Sent staff notification for new registration {}", queue.getId());
    }

    // ========================================================================
    // Email Body Builders
    // ========================================================================

    /**
     * Builds the approval email body.
     */
    private String buildApprovalEmailBody(PatientRegistrationQueue queue,
                                           RegistrationModuleConfig config,
                                           String language) {
        // Try to load template from resources
        String template = loadTemplate("approval", language);

        if (template != null) {
            return substituteVariables(template, queue, config);
        }

        // Fallback to inline template
        return buildInlineApprovalTemplate(queue, config, language);
    }

    /**
     * Builds the rejection email body.
     */
    private String buildRejectionEmailBody(PatientRegistrationQueue queue,
                                            RegistrationModuleConfig config,
                                            String language) {
        // Try to load template from resources
        String template = loadTemplate("rejection", language);

        if (template != null) {
            return substituteVariables(template, queue, config);
        }

        // Fallback to inline template
        return buildInlineRejectionTemplate(queue, config, language);
    }

    /**
     * Builds the staff notification body.
     */
    private String buildStaffNotificationBody(PatientRegistrationQueue queue,
                                               RegistrationModuleConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style=\"font-family: Arial, sans-serif;\">");
        sb.append("<h2>New Patient Registration</h2>");
        sb.append("<p>A new patient registration has been submitted and requires review.</p>");

        sb.append("<table style=\"border-collapse: collapse;\">");
        addTableRow(sb, "Name", queue.getFullName());
        addTableRow(sb, "Date of Birth", queue.getFullDateOfBirth());
        addTableRow(sb, "Health Card", queue.getMaskedHin());
        addTableRow(sb, "Email", queue.getEmail());
        addTableRow(sb, "Phone", queue.getCellPhone());
        addTableRow(sb, "Submitted At",
                queue.getSubmittedAt() != null ?
                        new SimpleDateFormat("yyyy-MM-dd HH:mm").format(queue.getSubmittedAt()) : "N/A");
        sb.append("</table>");

        if (queue.hasDuplicateWarning()) {
            sb.append("<p style=\"color: #dc3545;\"><strong>Warning:</strong> Potential duplicate detected!</p>");
        }

        sb.append("<p><a href=\"").append(buildReviewUrl(config, queue.getId()))
                .append("\">Click here to review this registration</a></p>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private void addTableRow(StringBuilder sb, String label, String value) {
        sb.append("<tr>")
                .append("<td style=\"padding: 5px 10px; font-weight: bold;\">").append(label).append(":</td>")
                .append("<td style=\"padding: 5px 10px;\">").append(value != null ? value : "N/A").append("</td>")
                .append("</tr>");
    }

    // ========================================================================
    // Inline Templates (Fallback)
    // ========================================================================

    private String buildInlineApprovalTemplate(PatientRegistrationQueue queue,
                                                RegistrationModuleConfig config,
                                                String language) {
        String clinicName = config != null ? config.getClinicName() : "Medical Clinic";
        String clinicPhone = config != null ? config.getClinicPhone() : "";
        boolean isRTL = RTL_LANGUAGES.contains(language);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body");
        if (isRTL) {
            sb.append(" dir=\"rtl\" style=\"text-align: right; font-family: Arial, sans-serif;\"");
        } else {
            sb.append(" style=\"font-family: Arial, sans-serif;\"");
        }
        sb.append(">");

        switch (language) {
            case "fr":
                sb.append("<p>Cher(e) ").append(queue.getFirstName()).append(",</p>");
                sb.append("<p>Votre inscription en tant que patient a \u00e9t\u00e9 approuv\u00e9e. ");
                sb.append("Vous \u00eates maintenant inscrit(e) \u00e0 ").append(clinicName).append(".</p>");
                sb.append("<p><strong>D\u00e9tails de l'inscription :</strong></p>");
                sb.append("<ul>");
                sb.append("<li>Nom : ").append(queue.getFullName()).append("</li>");
                sb.append("<li>Carte sant\u00e9 : ").append(queue.getMaskedHin()).append("</li>");
                sb.append("</ul>");
                sb.append("<p>Pour prendre votre premier rendez-vous, veuillez nous appeler");
                if (StringUtils.isNotBlank(clinicPhone)) {
                    sb.append(" au ").append(clinicPhone);
                }
                sb.append(".</p>");
                sb.append("<p>Cordialement,<br/>L'\u00e9quipe de ").append(clinicName).append("</p>");
                break;

            case "ar":
                sb.append("<p>\u0639\u0632\u064a\u0632\u064a/\u0639\u0632\u064a\u0632\u062a\u064a ").append(queue.getFirstName()).append("\u060c</p>");
                sb.append("<p>\u062a\u0645\u062a \u0627\u0644\u0645\u0648\u0627\u0641\u0642\u0629 \u0639\u0644\u0649 \u062a\u0633\u062c\u064a\u0644\u0643 \u0643\u0645\u0631\u064a\u0636. ");
                sb.append("\u0623\u0646\u062a \u0627\u0644\u0622\u0646 \u0645\u0633\u062c\u0644 \u0641\u064a ").append(clinicName).append(".</p>");
                sb.append("<p><strong>\u062a\u0641\u0627\u0635\u064a\u0644 \u0627\u0644\u062a\u0633\u062c\u064a\u0644:</strong></p>");
                sb.append("<ul>");
                sb.append("<li>\u0627\u0644\u0627\u0633\u0645: ").append(queue.getFullName()).append("</li>");
                sb.append("<li>\u0627\u0644\u0628\u0637\u0627\u0642\u0629 \u0627\u0644\u0635\u062d\u064a\u0629: ").append(queue.getMaskedHin()).append("</li>");
                sb.append("</ul>");
                sb.append("<p>\u0644\u062d\u062c\u0632 \u0645\u0648\u0639\u062f\u0643 \u0627\u0644\u0623\u0648\u0644\u060c \u064a\u0631\u062c\u0649 \u0627\u0644\u0627\u062a\u0635\u0627\u0644 \u0628\u0646\u0627");
                if (StringUtils.isNotBlank(clinicPhone)) {
                    sb.append(" \u0639\u0644\u0649 ").append(clinicPhone);
                }
                sb.append(".</p>");
                sb.append("<p>\u0645\u0639 \u0623\u0637\u064a\u0628 \u0627\u0644\u062a\u062d\u064a\u0627\u062a\u060c<br/>\u0641\u0631\u064a\u0642 ").append(clinicName).append("</p>");
                break;

            case "hi":
                sb.append("<p>\u092a\u094d\u0930\u093f\u092f ").append(queue.getFirstName()).append(",</p>");
                sb.append("<p>\u0906\u092a\u0915\u093e \u0930\u094b\u0917\u0940 \u092a\u0902\u091c\u0940\u0915\u0930\u0923 \u0938\u094d\u0935\u0940\u0915\u0943\u0924 \u0939\u094b \u0917\u092f\u093e \u0939\u0948\u0964 ");
                sb.append("\u0905\u092c \u0906\u092a ").append(clinicName).append(" \u092e\u0947\u0902 \u092a\u0902\u091c\u0940\u0915\u0943\u0924 \u0939\u0948\u0902\u0964</p>");
                sb.append("<p><strong>\u092a\u0902\u091c\u0940\u0915\u0930\u0923 \u0935\u093f\u0935\u0930\u0923:</strong></p>");
                sb.append("<ul>");
                sb.append("<li>\u0928\u093e\u092e: ").append(queue.getFullName()).append("</li>");
                sb.append("<li>\u0938\u094d\u0935\u093e\u0938\u094d\u0925\u094d\u092f \u0915\u093e\u0930\u094d\u0921: ").append(queue.getMaskedHin()).append("</li>");
                sb.append("</ul>");
                sb.append("<p>\u0905\u092a\u0928\u0940 \u092a\u0939\u0932\u0940 \u0905\u092a\u0949\u0907\u0902\u091f\u092e\u0947\u0902\u091f \u092c\u0941\u0915 \u0915\u0930\u0928\u0947 \u0915\u0947 \u0932\u093f\u090f\u060c \u0915\u0943\u092a\u092f\u093e \u0939\u092e\u0947\u0902 \u0915\u0949\u0932 \u0915\u0930\u0947\u0902");
                if (StringUtils.isNotBlank(clinicPhone)) {
                    sb.append(" ").append(clinicPhone).append(" \u092a\u0930");
                }
                sb.append("\u0964</p>");
                sb.append("<p>\u0938\u093e\u0926\u0930,<br/>").append(clinicName).append(" \u091f\u0940\u092e</p>");
                break;

            case "zh":
                sb.append("<p>\u5c0a\u656c\u7684 ").append(queue.getFirstName()).append("\uff1a</p>");
                sb.append("<p>\u60a8\u7684\u60a3\u8005\u6ce8\u518c\u5df2\u83b7\u6279\u51c6\u3002");
                sb.append("\u60a8\u73b0\u5df2\u5728").append(clinicName).append("\u6ce8\u518c\u3002</p>");
                sb.append("<p><strong>\u6ce8\u518c\u8be6\u60c5\uff1a</strong></p>");
                sb.append("<ul>");
                sb.append("<li>\u59d3\u540d\uff1a").append(queue.getFullName()).append("</li>");
                sb.append("<li>\u5065\u5eb7\u5361\uff1a").append(queue.getMaskedHin()).append("</li>");
                sb.append("</ul>");
                sb.append("<p>\u5982\u9700\u9884\u7ea6\u9996\u6b21\u5c31\u8bca\uff0c\u8bf7\u81f4\u7535");
                if (StringUtils.isNotBlank(clinicPhone)) {
                    sb.append(" ").append(clinicPhone);
                }
                sb.append("\u3002</p>");
                sb.append("<p>\u6b64\u81f4\u656c\u793c\uff0c<br/>").append(clinicName).append("\u56e2\u961f</p>");
                break;

            default: // English
                sb.append("<p>Dear ").append(queue.getFirstName()).append(",</p>");
                sb.append("<p>Your patient registration has been approved. ");
                sb.append("You are now registered at ").append(clinicName).append(".</p>");
                sb.append("<p><strong>Registration Details:</strong></p>");
                sb.append("<ul>");
                sb.append("<li>Name: ").append(queue.getFullName()).append("</li>");
                sb.append("<li>Health Card: ").append(queue.getMaskedHin()).append("</li>");
                sb.append("</ul>");
                sb.append("<p>To book your first appointment, please call us");
                if (StringUtils.isNotBlank(clinicPhone)) {
                    sb.append(" at ").append(clinicPhone);
                }
                sb.append(".</p>");
                sb.append("<p>Best regards,<br/>").append(clinicName).append(" Team</p>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String buildInlineRejectionTemplate(PatientRegistrationQueue queue,
                                                 RegistrationModuleConfig config,
                                                 String language) {
        String clinicName = config != null ? config.getClinicName() : "Medical Clinic";
        String reason = StringUtils.defaultIfBlank(queue.getRejectionReason(), "Unable to verify information");
        boolean isRTL = RTL_LANGUAGES.contains(language);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body");
        if (isRTL) {
            sb.append(" dir=\"rtl\" style=\"text-align: right; font-family: Arial, sans-serif;\"");
        } else {
            sb.append(" style=\"font-family: Arial, sans-serif;\"");
        }
        sb.append(">");

        switch (language) {
            case "fr":
                sb.append("<p>Cher(e) ").append(queue.getFirstName()).append(",</p>");
                sb.append("<p>Nous n'avons pas pu compl\u00e9ter votre inscription pour le moment.</p>");
                sb.append("<p><strong>Raison :</strong> ").append(reason).append("</p>");
                sb.append("<p>Si vous pensez qu'il s'agit d'une erreur, veuillez visiter notre clinique en personne.</p>");
                sb.append("<p>Cordialement,<br/>L'\u00e9quipe de ").append(clinicName).append("</p>");
                break;

            case "ar":
                sb.append("<p>\u0639\u0632\u064a\u0632\u064a/\u0639\u0632\u064a\u0632\u062a\u064a ").append(queue.getFirstName()).append("\u060c</p>");
                sb.append("<p>\u0644\u0645 \u0646\u062a\u0645\u0643\u0646 \u0645\u0646 \u0625\u0643\u0645\u0627\u0644 \u062a\u0633\u062c\u064a\u0644\u0643 \u0641\u064a \u0647\u0630\u0627 \u0627\u0644\u0648\u0642\u062a.</p>");
                sb.append("<p><strong>\u0627\u0644\u0633\u0628\u0628:</strong> ").append(reason).append("</p>");
                sb.append("<p>\u0625\u0630\u0627 \u0643\u0646\u062a \u062a\u0639\u062a\u0642\u062f \u0623\u0646 \u0647\u0630\u0627 \u062e\u0637\u0623\u060c \u064a\u0631\u062c\u0649 \u0632\u064a\u0627\u0631\u0629 \u0639\u064a\u0627\u062f\u062a\u0646\u0627 \u0634\u062e\u0635\u064a\u064b\u0627.</p>");
                sb.append("<p>\u0645\u0639 \u0623\u0637\u064a\u0628 \u0627\u0644\u062a\u062d\u064a\u0627\u062a\u060c<br/>\u0641\u0631\u064a\u0642 ").append(clinicName).append("</p>");
                break;

            case "hi":
                sb.append("<p>\u092a\u094d\u0930\u093f\u092f ").append(queue.getFirstName()).append(",</p>");
                sb.append("<p>\u0939\u092e \u0907\u0938 \u0938\u092e\u092f \u0906\u092a\u0915\u093e \u092a\u0902\u091c\u0940\u0915\u0930\u0923 \u092a\u0942\u0930\u093e \u0915\u0930\u0928\u0947 \u092e\u0947\u0902 \u0905\u0938\u092e\u0930\u094d\u0925 \u0925\u0947\u0964</p>");
                sb.append("<p><strong>\u0915\u093e\u0930\u0923:</strong> ").append(reason).append("</p>");
                sb.append("<p>\u092f\u0926\u093f \u0906\u092a\u0915\u094b \u0932\u0917\u0924\u093e \u0939\u0948 \u0915\u093f \u092f\u0939 \u090f\u0915 \u0924\u094d\u0930\u0941\u091f\u093f \u0939\u0948\u060c \u0915\u0943\u092a\u092f\u093e \u0935\u094d\u092f\u0915\u094d\u0924\u093f\u0917\u0924 \u0930\u0942\u092a \u0938\u0947 \u0939\u092e\u093e\u0930\u0947 \u0915\u094d\u0932\u093f\u0928\u093f\u0915 \u092e\u0947\u0902 \u0906\u090f\u0902\u0964</p>");
                sb.append("<p>\u0938\u093e\u0926\u0930,<br/>").append(clinicName).append(" \u091f\u0940\u092e</p>");
                break;

            case "zh":
                sb.append("<p>\u5c0a\u656c\u7684 ").append(queue.getFirstName()).append("\uff1a</p>");
                sb.append("<p>\u6211\u4eec\u76ee\u524d\u65e0\u6cd5\u5b8c\u6210\u60a8\u7684\u60a3\u8005\u6ce8\u518c\u3002</p>");
                sb.append("<p><strong>\u539f\u56e0\uff1a</strong> ").append(reason).append("</p>");
                sb.append("<p>\u5982\u679c\u60a8\u8ba4\u4e3a\u8fd9\u662f\u9519\u8bef\uff0c\u8bf7\u4eb2\u81ea\u5230\u6211\u4eec\u7684\u8bca\u6240\u3002</p>");
                sb.append("<p>\u6b64\u81f4\u656c\u793c\uff0c<br/>").append(clinicName).append("\u56e2\u961f</p>");
                break;

            default: // English
                sb.append("<p>Dear ").append(queue.getFirstName()).append(",</p>");
                sb.append("<p>We were unable to complete your patient registration at this time.</p>");
                sb.append("<p><strong>Reason:</strong> ").append(reason).append("</p>");
                sb.append("<p>If you believe this is an error, please visit our clinic in person.</p>");
                sb.append("<p>Best regards,<br/>").append(clinicName).append(" Team</p>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Maps spoken language name to language code.
     */
    private String mapSpokenLangToCode(String spokenLang) {
        if (StringUtils.isBlank(spokenLang)) {
            return "en";
        }

        String lower = spokenLang.toLowerCase().trim();

        if (lower.contains("french") || lower.contains("fran\u00e7ais")) return "fr";
        if (lower.contains("arabic") || lower.contains("\u0627\u0644\u0639\u0631\u0628\u064a\u0629")) return "ar";
        if (lower.contains("hindi") || lower.contains("\u0939\u093f\u0928\u094d\u0926\u0940")) return "hi";
        if (lower.contains("mandarin") || lower.contains("chinese") || lower.contains("\u4e2d\u6587")) return "zh";

        return "en";
    }

    private String getApprovalSubject(String language) {
        return APPROVAL_SUBJECTS.getOrDefault(language, APPROVAL_SUBJECTS.get("en"));
    }

    private String getRejectionSubject(String language) {
        return REJECTION_SUBJECTS.getOrDefault(language, REJECTION_SUBJECTS.get("en"));
    }

    /**
     * Loads email template from resources.
     */
    private String loadTemplate(String type, String language) {
        String path = TEMPLATE_BASE_PATH + type + "/" + language + ".html";

        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            logger.debug("Template not found at {}, using inline template", path);
        }

        return null;
    }

    /**
     * Substitutes variables in template.
     */
    private String substituteVariables(String template,
                                        PatientRegistrationQueue queue,
                                        RegistrationModuleConfig config) {
        String result = template;

        result = result.replace("{{firstName}}", StringUtils.defaultString(queue.getFirstName()));
        result = result.replace("{{lastName}}", StringUtils.defaultString(queue.getLastName()));
        result = result.replace("{{fullName}}", StringUtils.defaultString(queue.getFullName()));
        result = result.replace("{{maskedHin}}", StringUtils.defaultString(queue.getMaskedHin()));
        result = result.replace("{{email}}", StringUtils.defaultString(queue.getEmail()));

        if (config != null) {
            result = result.replace("{{clinicName}}", StringUtils.defaultString(config.getClinicName()));
            result = result.replace("{{clinicPhone}}", StringUtils.defaultString(config.getClinicPhone()));
            result = result.replace("{{clinicAddress}}", StringUtils.defaultString(config.getClinicAddress()));
        }

        result = result.replace("{{rejectionReason}}",
                StringUtils.defaultString(queue.getRejectionReason(), "Unable to verify information"));

        if (queue.getReviewedAt() != null) {
            result = result.replace("{{registeredDate}}",
                    new SimpleDateFormat("yyyy-MM-dd").format(queue.getReviewedAt()));
        }

        return result;
    }

    /**
     * Builds the review URL for staff.
     */
    private String buildReviewUrl(RegistrationModuleConfig config, Integer queueId) {
        String baseUrl = config != null && config.getQrCodeUrl() != null ?
                config.getQrCodeUrl().replace("/registration/start", "") :
                "";
        return baseUrl + "/admin/registration/review.jsp?id=" + queueId;
    }

    /**
     * Sends an email (placeholder - integrate with actual email service).
     */
    private void sendEmail(String to, String subject, String body, String language) {
        // TODO: Integrate with actual email sending service (e.g., JavaMail, SendGrid)
        // For now, just log the attempt
        logger.info("Would send email to: {}, subject: {}, language: {}", to, subject, language);

        // Example integration with OpenO's email service:
        // EmailUtils.sendHtmlEmail(fromAddress, fromName, to, subject, body);
    }

    // ========================================================================
    // Configuration
    // ========================================================================

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
