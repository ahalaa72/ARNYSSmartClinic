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
package ca.openosp.openo.registration.util;

import ca.openosp.openo.registration.model.PatientRegistrationQueue;
import ca.openosp.openo.registration.model.RegistrationModuleConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validation utility for patient registration data.
 *
 * <p>Provides comprehensive validation with inline error messages for each field.
 * Returns specific, user-friendly error messages instead of boolean flags.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Component
public class RegistrationValidator {

    // ========================================================================
    // Validation Patterns
    // ========================================================================

    /** Canadian postal code pattern (A1A 1A1 or A1A1A1) */
    private static final Pattern POSTAL_CODE_PATTERN =
            Pattern.compile("^[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$");

    /** Phone number pattern (10 digits, various formats) */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[\\d\\-\\(\\)\\s\\.]+$");

    /** Email pattern */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Ontario HIN pattern (10 digits) */
    private static final Pattern ONTARIO_HIN_PATTERN =
            Pattern.compile("^\\d{10}$");

    /** Name pattern (letters, spaces, hyphens, apostrophes) */
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z\\s\\-']+$");

    // ========================================================================
    // Validation Result Class
    // ========================================================================

    /**
     * Result of validation containing field-level errors.
     */
    public static class ValidationResult {
        private final Map<String, String> errors = new HashMap<>();
        private boolean valid = true;

        public void addError(String field, String message) {
            errors.put(field, message);
            valid = false;
        }

        public boolean isValid() {
            return valid;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public String getError(String field) {
            return errors.get(field);
        }

        public boolean hasError(String field) {
            return errors.containsKey(field);
        }
    }

    /**
     * Result of HIN validation with specific error message.
     */
    public static class HinValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public HinValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static HinValidationResult valid() {
            return new HinValidationResult(true, null);
        }

        public static HinValidationResult invalid(String message) {
            return new HinValidationResult(false, message);
        }
    }

    // ========================================================================
    // Full Registration Validation
    // ========================================================================

    /**
     * Validates a complete registration with default mandatory fields.
     *
     * @param queue PatientRegistrationQueue the registration to validate
     * @return ValidationResult containing any field errors
     */
    public ValidationResult validate(PatientRegistrationQueue queue) {
        return validate(queue, getDefaultMandatoryFields());
    }

    /**
     * Validates a complete registration with specified mandatory fields.
     *
     * @param queue PatientRegistrationQueue the registration to validate
     * @param mandatoryFields List of field names that are required
     * @return ValidationResult containing any field errors
     */
    public ValidationResult validate(PatientRegistrationQueue queue, List<String> mandatoryFields) {
        ValidationResult result = new ValidationResult();

        // Check mandatory fields
        for (String field : mandatoryFields) {
            String value = getFieldValue(queue, field);
            if (StringUtils.isBlank(value)) {
                result.addError(field, getFieldDisplayName(field) + " is required");
            }
        }

        // Validate specific field formats if present
        validateFieldFormats(queue, result);

        return result;
    }

    /**
     * Validates a registration using configuration from database.
     *
     * @param queue PatientRegistrationQueue the registration to validate
     * @param config RegistrationModuleConfig the configuration with mandatory fields
     * @return ValidationResult containing any field errors
     */
    public ValidationResult validate(PatientRegistrationQueue queue, RegistrationModuleConfig config) {
        return validate(queue, config.getMandatoryFieldsList());
    }

    // ========================================================================
    // Field Format Validation
    // ========================================================================

    /**
     * Validates formats of individual fields.
     */
    private void validateFieldFormats(PatientRegistrationQueue queue, ValidationResult result) {
        // First Name format
        if (StringUtils.isNotBlank(queue.getFirstName())) {
            if (queue.getFirstName().length() > 30) {
                result.addError("first_name", "First name cannot exceed 30 characters");
            } else if (!NAME_PATTERN.matcher(queue.getFirstName()).matches()) {
                result.addError("first_name", "First name contains invalid characters");
            }
        }

        // Last Name format
        if (StringUtils.isNotBlank(queue.getLastName())) {
            if (queue.getLastName().length() > 30) {
                result.addError("last_name", "Last name cannot exceed 30 characters");
            } else if (!NAME_PATTERN.matcher(queue.getLastName()).matches()) {
                result.addError("last_name", "Last name contains invalid characters");
            }
        }

        // Date of birth validation
        validateDateOfBirth(queue, result);

        // Sex validation
        if (StringUtils.isNotBlank(queue.getSex())) {
            if (!queue.getSex().matches("^[MFO]$")) {
                result.addError("sex", "Sex must be M (Male), F (Female), or O (Other)");
            }
        }

        // Email format
        if (StringUtils.isNotBlank(queue.getEmail())) {
            if (queue.getEmail().length() > 100) {
                result.addError("email", "Email cannot exceed 100 characters");
            } else if (!EMAIL_PATTERN.matcher(queue.getEmail()).matches()) {
                result.addError("email", "Please enter a valid email address");
            }
        }

        // Phone format
        if (StringUtils.isNotBlank(queue.getCellPhone())) {
            String digitsOnly = queue.getCellPhone().replaceAll("[^0-9]", "");
            if (digitsOnly.length() != 10) {
                result.addError("cell_phone", "Cell phone must be 10 digits");
            }
        }

        if (StringUtils.isNotBlank(queue.getPhone())) {
            String digitsOnly = queue.getPhone().replaceAll("[^0-9]", "");
            if (digitsOnly.length() != 10 && digitsOnly.length() != 0) {
                result.addError("phone", "Home phone must be 10 digits");
            }
        }

        // Postal code format
        if (StringUtils.isNotBlank(queue.getPostal())) {
            if (!POSTAL_CODE_PATTERN.matcher(queue.getPostal()).matches()) {
                result.addError("postal", "Postal code must be in format A1A 1A1");
            }
        }

        // HIN validation
        if (StringUtils.isNotBlank(queue.getHin())) {
            HinValidationResult hinResult = validateHin(queue.getHin(), queue.getHcType());
            if (!hinResult.isValid()) {
                result.addError("hin", hinResult.getErrorMessage());
            }

            // If HIN is provided, province is required
            if (StringUtils.isBlank(queue.getHcType())) {
                result.addError("hc_type", "Health card province is required when HIN is provided");
            }
        }

        // Address length
        if (StringUtils.isNotBlank(queue.getAddress()) && queue.getAddress().length() > 60) {
            result.addError("address", "Address cannot exceed 60 characters");
        }

        // City length
        if (StringUtils.isNotBlank(queue.getCity()) && queue.getCity().length() > 50) {
            result.addError("city", "City cannot exceed 50 characters");
        }
    }

    /**
     * Validates date of birth components.
     */
    private void validateDateOfBirth(PatientRegistrationQueue queue, ValidationResult result) {
        String year = queue.getYearOfBirth();
        String month = queue.getMonthOfBirth();
        String day = queue.getDateOfBirth();

        // If any component is present, all must be present
        boolean hasYear = StringUtils.isNotBlank(year);
        boolean hasMonth = StringUtils.isNotBlank(month);
        boolean hasDay = StringUtils.isNotBlank(day);

        if (hasYear || hasMonth || hasDay) {
            if (!hasYear) {
                result.addError("year_of_birth", "Year of birth is required");
            }
            if (!hasMonth) {
                result.addError("month_of_birth", "Month of birth is required");
            }
            if (!hasDay) {
                result.addError("date_of_birth", "Day of birth is required");
            }

            // Validate format and range
            if (hasYear && hasMonth && hasDay) {
                try {
                    int yearInt = Integer.parseInt(year);
                    int monthInt = Integer.parseInt(month);
                    int dayInt = Integer.parseInt(day);

                    // Year range check
                    int currentYear = LocalDate.now().getYear();
                    if (yearInt < 1900 || yearInt > currentYear) {
                        result.addError("year_of_birth", "Year must be between 1900 and " + currentYear);
                    }

                    // Month range check
                    if (monthInt < 1 || monthInt > 12) {
                        result.addError("month_of_birth", "Month must be between 1 and 12");
                    }

                    // Day range check
                    if (dayInt < 1 || dayInt > 31) {
                        result.addError("date_of_birth", "Day must be between 1 and 31");
                    }

                    // Validate the date is valid and not in the future
                    if (!result.hasError("year_of_birth") &&
                            !result.hasError("month_of_birth") &&
                            !result.hasError("date_of_birth")) {
                        try {
                            LocalDate dob = LocalDate.of(yearInt, monthInt, dayInt);
                            if (dob.isAfter(LocalDate.now())) {
                                result.addError("year_of_birth", "Date of birth cannot be in the future");
                            }
                        } catch (Exception e) {
                            result.addError("date_of_birth", "Invalid date");
                        }
                    }
                } catch (NumberFormatException e) {
                    if (hasYear && !year.matches("\\d+")) {
                        result.addError("year_of_birth", "Year must be a number");
                    }
                    if (hasMonth && !month.matches("\\d+")) {
                        result.addError("month_of_birth", "Month must be a number");
                    }
                    if (hasDay && !day.matches("\\d+")) {
                        result.addError("date_of_birth", "Day must be a number");
                    }
                }
            }
        }
    }

    // ========================================================================
    // HIN Validation
    // ========================================================================

    /**
     * Validates a Health Insurance Number (HIN) with inline error messages.
     *
     * @param hin String the health insurance number
     * @param province String the province code (ON, BC, AB, etc.)
     * @return HinValidationResult with specific error message if invalid
     */
    public HinValidationResult validateHin(String hin, String province) {
        // If no HIN provided, it's valid (optional field)
        if (StringUtils.isBlank(hin)) {
            return HinValidationResult.valid();
        }

        // Remove any spaces or hyphens
        String cleanHin = hin.replaceAll("[\\s\\-]", "");

        // Province-specific validation
        if ("ON".equalsIgnoreCase(province)) {
            return validateOntarioHin(cleanHin);
        }

        // For other provinces, just check it's alphanumeric and reasonable length
        if (cleanHin.length() < 5 || cleanHin.length() > 15) {
            return HinValidationResult.invalid("Health card number must be between 5 and 15 characters");
        }

        if (!cleanHin.matches("^[A-Za-z0-9]+$")) {
            return HinValidationResult.invalid("Health card number contains invalid characters");
        }

        return HinValidationResult.valid();
    }

    /**
     * Validates an Ontario Health Insurance Number (10 digits with MOD-10 checksum).
     *
     * @param hin String the health insurance number (cleaned)
     * @return HinValidationResult with specific error message if invalid
     */
    private HinValidationResult validateOntarioHin(String hin) {
        // Must be exactly 10 digits
        if (!ONTARIO_HIN_PATTERN.matcher(hin).matches()) {
            if (hin.length() != 10) {
                return HinValidationResult.invalid("Ontario health card number must be exactly 10 digits");
            }
            return HinValidationResult.invalid("Ontario health card number must contain only digits");
        }

        // MOD-10 (Luhn) checksum validation
        if (!validateMod10(hin)) {
            return HinValidationResult.invalid("Invalid health card number (checksum failed)");
        }

        return HinValidationResult.valid();
    }

    /**
     * Performs MOD-10 (Luhn algorithm) validation.
     *
     * <p>The algorithm:
     * 1. Double every second digit from right to left
     * 2. If doubling results in a number > 9, add the digits together
     * 3. Sum all digits
     * 4. If sum is divisible by 10, the number is valid</p>
     *
     * @param number String the number to validate
     * @return boolean true if the checksum is valid
     */
    public boolean validateMod10(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10);
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10) == 0;
    }

    // ========================================================================
    // Phone Number Formatting
    // ========================================================================

    /**
     * Formats a phone number to standard format (###-###-####).
     *
     * @param phone String the phone number to format
     * @return String the formatted phone number, or original if can't be formatted
     */
    public String formatPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return phone;
        }

        // Extract digits only
        String digitsOnly = phone.replaceAll("[^0-9]", "");

        if (digitsOnly.length() == 10) {
            return String.format("%s-%s-%s",
                    digitsOnly.substring(0, 3),
                    digitsOnly.substring(3, 6),
                    digitsOnly.substring(6, 10));
        }

        // Return original if can't be formatted
        return phone;
    }

    /**
     * Formats a postal code to standard format (A1A 1A1).
     *
     * @param postal String the postal code to format
     * @return String the formatted postal code
     */
    public String formatPostalCode(String postal) {
        if (StringUtils.isBlank(postal)) {
            return postal;
        }

        // Remove spaces and hyphens
        String cleaned = postal.replaceAll("[\\s\\-]", "").toUpperCase();

        if (cleaned.length() == 6) {
            return String.format("%s %s",
                    cleaned.substring(0, 3),
                    cleaned.substring(3, 6));
        }

        return postal;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Gets the value of a field by name.
     */
    private String getFieldValue(PatientRegistrationQueue queue, String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "first_name": return queue.getFirstName();
            case "last_name": return queue.getLastName();
            case "middle_names": return queue.getMiddleNames();
            case "pref_name": return queue.getPreferredName();
            case "title": return queue.getTitle();
            case "year_of_birth": return queue.getYearOfBirth();
            case "month_of_birth": return queue.getMonthOfBirth();
            case "date_of_birth": return queue.getDateOfBirth();
            case "sex": return queue.getSex();
            case "gender": return queue.getGender();
            case "pronoun": return queue.getPronoun();
            case "address": return queue.getAddress();
            case "city": return queue.getCity();
            case "province": return queue.getProvince();
            case "postal": return queue.getPostal();
            case "phone": return queue.getPhone();
            case "phone2": return queue.getPhone2();
            case "cell_phone": return queue.getCellPhone();
            case "email": return queue.getEmail();
            case "hin": return queue.getHin();
            case "ver": return queue.getVer();
            case "hc_type": return queue.getHcType();
            default: return null;
        }
    }

    /**
     * Gets a user-friendly display name for a field.
     */
    private String getFieldDisplayName(String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "first_name": return "First name";
            case "last_name": return "Last name";
            case "middle_names": return "Middle names";
            case "pref_name": return "Preferred name";
            case "year_of_birth": return "Year of birth";
            case "month_of_birth": return "Month of birth";
            case "date_of_birth": return "Day of birth";
            case "sex": return "Sex";
            case "gender": return "Gender";
            case "pronoun": return "Pronouns";
            case "address": return "Address";
            case "city": return "City";
            case "province": return "Province";
            case "postal": return "Postal code";
            case "phone": return "Home phone";
            case "phone2": return "Work phone";
            case "cell_phone": return "Cell phone";
            case "email": return "Email";
            case "hin": return "Health card number";
            case "ver": return "Health card version";
            case "hc_type": return "Health card province";
            default: return fieldName;
        }
    }

    /**
     * Returns the default list of mandatory fields.
     */
    private List<String> getDefaultMandatoryFields() {
        return List.of(
                "first_name",
                "last_name",
                "year_of_birth",
                "month_of_birth",
                "date_of_birth",
                "sex",
                "address",
                "city",
                "province",
                "postal",
                "cell_phone",
                "email"
        );
    }
}
