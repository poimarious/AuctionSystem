package org.deptrai.auctionsystem.utils;

import java.util.regex.Pattern;

public class ValidationUtils {

    public static boolean isValidPassword(String password){
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        Pattern pattern = Pattern.compile(passwordRegex);
        return pattern.matcher(password).matches();
    }

}
