package com.acerat.solidtest.checkout;

import com.acerat.solidtest.cardpayments.CardDetails;
import com.acerat.solidtest.cardpayments.CardPaymentService;
import com.acerat.solidtest.checkout.state.*;
import com.acerat.solidtest.configuration.ApplicationConfiguration;
import com.acerat.solidtest.customers.Address;
import com.acerat.solidtest.customers.Customer;
import com.acerat.solidtest.customers.CustomerPaymentMethod;
import com.acerat.solidtest.customers.CustomerRepository;
import com.acerat.solidtest.encryptedstores.Encryption;
import com.acerat.solidtest.encryptedstores.TrustStore;
import com.acerat.solidtest.invoicing.Invoice;
import com.acerat.solidtest.invoicing.InvoiceHandler;
import com.acerat.solidtest.logistics.ShipmentTracker;
import com.acerat.solidtest.logistics.Warehouse;
import com.acerat.solidtest.product.Product;
import com.acerat.solidtest.product.ProductStore;
import com.acerat.solidtest.shoppingcart.Order;
import com.acerat.solidtest.shoppingcart.OrderLine;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class CheckoutHandler {

    public CheckoutState checkoutState;
    ProductStore productStore;

    public CheckoutState checkout(CheckoutState checkoutState) {
        Order order = checkoutState.getOrder();
        this.checkoutState = new CheckoutState(order)
        // Get customer
        Customer customer = getCustomer(order);

        // Validate shipping information
        validateShippingInformation(customer);

        // Makes sure we reserve items in stock in case they have been released and that we don't charge customer twice before we ship out
        if(hasReservedProducts() && hasProcessedPayment(customer)){
            // Send reserved items
            shipOut(order);
        }
        return checkoutState;
    }

    /** 
     * This method allows you to retrieve the customer object to whom you are to process this checkout for
     * @getCustomer(Order order): Takes an order object and returns the customer
    */
    public Customer getCustomer(Order order){
        CustomerRepository customerRepository = new CustomerRepository(ApplicationConfiguration.getConnectionString());
        Customer customer = customerRepository.get(order.getCustomerId());
        return customer;
    }

    /** 
     * This method allows you to validate any information that is used while shipping out this order with respect to the customer's address
     * @validateShippingInformation()
    */
    public void validateShippingInformation(Customer customer){

        ShipmentTracker shipmentTracker = new ShipmentTracker(ApplicationConfiguration.getConnectionString());
        if (customer.getShippingAddress() == null) {
            checkoutState.shipmentFailed(ShipmentFailures.MISSING_CUSTOMER_ADDRESS);
            return this.checkoutState;
        }
        Address shipmentAddress = customer.getShippingAddress();
        if (
                shipmentAddress.getStreet() == null || shipmentAddress.getStreet().isEmpty() ||
                shipmentAddress.getZipCode() == null || shipmentAddress.getZipCode().isEmpty() ||
                shipmentAddress.getCity() == null || shipmentAddress.getCity().isEmpty()
        ) {
            checkoutState.shipmentFailed(ShipmentFailures.INVALID_CUSTOMER_ADDRESS);
            return this.checkoutState;
        }
        if (!shipmentTracker.canShipToDestination(shipmentAddress)) {
            checkoutState.shipmentFailed(ShipmentFailures.CANNOT_SHIP_TO_DESTINATION);
            return checkoutState;
        }
        checkoutState.shipmentVerified();

    }

    /** 
     * This method allows you to reserve all existing products in this order so as to avoid any race conditions when product orders exceed the products in stock
     * @hasReservedProducts(): Boolean
    */
    public Boolean hasReservedProducts(){

        Warehouse warehouse = new Warehouse(ApplicationConfiguration.getConnectionString());
        productStore = new ProductStore(ApplicationConfiguration.getConnectionString());
        for (OrderLine orderLine : order.getOrderLines()) {
            Product product = productStore.getById(orderLine.getProductId());
            if (product == null) {
                checkoutState.warehouseReservationFailed(WarehouseReservationFailures.PRODUCT_NOT_FOUND);
                return false;
            }
            if (!product.isStoredInWarehouse())
                continue;
            if (!warehouse.isReservedInStock(orderLine.getUniqueOrderLineReference(), orderLine.getQty())) {
                if (!warehouse.tryReserveItems(orderLine.getUniqueOrderLineReference(), orderLine.getQty())) {
                    checkoutState.warehouseReservationFailed(WarehouseReservationFailures.COULD_NOT_RESERVE_ITEMS_IN_STOCK);
                    return false;
                }
            }
        }
        checkoutState.warehouseReservationSucceeded();
        return true;
    }

    /** 
     * This method processes all payments that concern this order checkout whether it be through a card or invoice
     * @hasProcessedPayment(Customer customer): Boolean
    */
    public Boolean hasProcessedPayment(Customer customer){

        if (!checkoutState.isPaid()) {
            // If the customer is set up to pay by card use the card payment service
            if (customer.getConfiguration().getPaymentMenthod() == CustomerPaymentMethod.CARD) {
                // Decrypt card details for our customer
                TrustStore trustStore = new TrustStore(ApplicationConfiguration.getTrustStoreCredentials());
                byte[] encryptedCardDetails = trustStore.getCardDetailsByCustomerId(customer.getCustomerId());
                List<CardDetails> cardDetailsList = Encryption.decryptFromSecret(encryptedCardDetails, customer.getCustomerSecret());

                // Pick the currently valid credit card
                Optional<CardDetails> currentCardDetails = Optional.empty();
                for (CardDetails cardDetails : cardDetailsList) {
                    if (cardDetails.getExpiresAt().isAfter(LocalDate.now())) {
                        currentCardDetails = Optional.of(cardDetails);
                        break;
                    }
                }
                // If there is no valid card update checkout state
                if (!currentCardDetails.isPresent()) {
                    checkoutState.cardPaymentFailed(CardPaymentFailures.NO_VALID_CREDIT_CARDS);
                    return false;
                }

                CardPaymentService cardPaymentService = new CardPaymentService(ApplicationConfiguration.getCardPaymentConfiguration());
                CardPaymentResult cardPaymentResult = cardPaymentService.chargeCreditCard(currentCardDetails.get());
                if (!cardPaymentResult.succeeded()) {
                    checkoutState.cardPaymentFailed(CardPaymentFailures.COULD_NOT_COMPLETE_CARD_PAYMENT);
                    return false;
                }else{
                    checkoutState.cardPaymentCompletedUsing(currentCardDetails.get().getCardDetailsReference());
                    return true;
                }
                
            } else if (customer.getConfiguration().getPaymentMenthod() == CustomerPaymentMethod.INVOICE) {
                // Send invoice to customer
                Address invoiceAddress = customer.getInvoiceAddress();
                if (invoiceAddress == null) {
                    checkoutState.failedToInvoiceCustomer(InvoiceFailures.MISSING_INVOICE_ADDRESS);
                    return false;
                }
                if (
                        invoiceAddress.getStreet() == null || invoiceAddress.getStreet().isEmpty() ||
                        invoiceAddress.getZipCode() == null || invoiceAddress.getZipCode().isEmpty() ||
                        invoiceAddress.getCity() == null || invoiceAddress.getCity().isEmpty()
                ) {
                    checkoutState.failedToInvoiceCustomer(InvoiceFailures.INVALID_CUSTOMER_ADDRESS);
                    return false;
                }
                InvoiceHandler invoiceHandler = new InvoiceHandler(ApplicationConfiguration.getConnectionString());
                Invoice invoice = invoiceHandler.produceInvoice(order, customer);
                checkoutState.invoiceSentSuccessfully(invoice.getInvoiceId());
                return true;
            }
        }

    }

    /** 
     * Processes the shipping details, with respect to the products ordered and the customer's address
     * @shipOut(Order order, ProductStore productStore)
    */
    public void shipOut(Order order){
        for (OrderLine orderLine : order.getOrderLines()) {
            Product product = productStore.getById(orderLine.getProductId());
            if (product == null) {
                checkoutState.shipmentActivationFailed(WarehouseSendFailures.PRODUCT_NOT_FOUND);
            }
            if (!product.isStoredInWarehouse())
                continue;
            if (!warehouse.activateShipment(orderLine.getUniqueOrderLineReference())) {
                checkoutState.shipmentActivationFailed(WarehouseSendFailures.COULD_NOT_ACTIVATE_SHIPMENT);
                return checkoutState;
            }
        }
        checkoutState.shipmentActivated();
    }
}
