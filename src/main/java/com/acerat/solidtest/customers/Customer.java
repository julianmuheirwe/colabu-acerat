package com.acerat.solidtest.customers;

import java.util.UUID;

public class Customer {
    public CustomerConfiguration getConfiguration() {
        return null;
    }

    public UUID getCustomerId() {
        return null;
    }

    public byte[] getCustomerSecret() {
        return new byte[0];
    }

    public Address getShippingAddress() {
        return null;
    }

    public Address getInvoiceAddress() {
        return null;
    }
}
