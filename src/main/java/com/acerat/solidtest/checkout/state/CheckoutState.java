package com.acerat.solidtest.checkout.state;

import com.acerat.solidtest.shoppingcart.Order;

import java.util.UUID;

public class CheckoutState {
    private Order order;

    public CheckoutState(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public void cardPaymentFailed(CardPaymentFailures noValidCreditCards) {
    }

    public void shipmentFailed(ShipmentFailures missingCustomerAddress) {
    }

    public void warehouseReservationFailed(WarehouseReservationFailures productNotFound) {
    }

    public void shipmentVerified() {
    }

    public void warehouseReservationSucceeded() {
    }

    public void cardPaymentCompletedUsing(UUID cardDetailsReference) {
    }

    public void failedToInvoiceCustomer(InvoiceFailures missingInvoiceAddress) {
    }

    public void invoiceSentSuccessfully(UUID invoiceId) {
    }

    public void shipmentActivationFailed(WarehouseSendFailures couldNotActivateShipment) {
    }

    public boolean isPaid() {
        return false;
    }

    public void shipmentActivated() {
    }
}
