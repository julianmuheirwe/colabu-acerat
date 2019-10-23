package com.acerat.solidtest.configuration;

import com.acerat.solidtest.encryptedstores.TrustStoreCredentials;

public class ApplicationConfiguration {
    public static String getConnectionString() {
        return "some;connection-string";
    }

    public static CardPaymentConfiguration getCardPaymentConfiguration() {
        return null;
    }

    public static TrustStoreCredentials getTrustStoreCredentials() {
        return null;
    }
}
