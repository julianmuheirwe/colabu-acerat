package com.acerat.solidtest.logistics;

import java.util.UUID;

public class Warehouse {
    public Warehouse(String connectionString) {
    }

    public boolean isReservedInStock(UUID uniqueOrderLineReference, int qty) {
        return false;
    }

    public boolean tryReserveItems(UUID uniqueOrderLineReference, int qty) {
        return false;
    }

    public boolean activateShipment(UUID uniqueOrderLineReference) {
        return false;
    }
}
