package com.acerat.solidtest.shoppingcart;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Order {
    private UUID customerId;

    public UUID getCustomerId() {
        return customerId;
    }

    public List<OrderLine> getOrderLines() {
        return Arrays.asList();
    }
}
