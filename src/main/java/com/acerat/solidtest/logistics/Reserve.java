package com.acerat.solidtest.logistics;

import java.util.UUID;

public interface Reserve {
    boolean isReservedInStock(UUID uniqueOrderLineReference, int qty);

    boolean tryReserveItems(UUID uniqueOrderLineReference, int qty);
}
