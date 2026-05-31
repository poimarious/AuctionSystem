package org.deptrai.auctionsystem.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class ValidationUtils {

  private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

  public static boolean isInvalidPassword(String password) {
    String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
    Pattern pattern = Pattern.compile(passwordRegex);
    return !pattern.matcher(password).matches();
  }

  public static boolean isValidEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      return false;
    }

    String regex = "^[A-Za-z0-9._%+-]" + // Bắt buộc gmail phải có ít nhất 1 kí tự trước @
            "+@" + // Bắt buộc phải có 1 kí tự @
            "(gmail\\.com|vnu\\.edu\\.vn)$"; // chỉ chấp nhận @gmail.com hoặc @vnu.edu.vn

    // check
    boolean isValid = email.matches(regex);

    if (!isValid) {
      logger.error("Email không hợp lệ hoặc sai định dạng: {}", email);
    }

    return isValid;
  }


}
