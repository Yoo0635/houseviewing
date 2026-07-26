package com.house.houseviewing.global.util;

public class AddressUtils {

    public static String extractDetailAddress(String originAddress) {
        if (!originAddress.contains(",")) {
            return null;
        }
        return originAddress.split(",")[1].trim(); //
    }
}
