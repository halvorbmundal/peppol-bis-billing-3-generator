package peppol.bis.invoice3.domain;

import org.junit.jupiter.api.Test;
import peppol.bis.invoice3.api.PeppolBillingApi;
import peppol.bis.invoice3.validation.ValidationResult;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreditNotePaymentDueDateTest {

    private static final String DUE_DATE = "2020-11-30";

    @Test
    @SuppressWarnings("deprecation")
    void due_date_on_the_credit_note_is_rejected() {
        final ValidationResult result = validate(creditNote(paymentMeans()).withPaymentDueDate(DUE_DATE));

        assertFalse(result.isValid());
        assertThat(result.errors(), hasItem(containsString("PaymentDueDate")));
    }

    @Test
    void due_date_on_the_payment_means_is_accepted() {
        final CreditNote creditNote = creditNote(paymentMeans().withPaymentDueDate(DUE_DATE));

        final ValidationResult result = validate(creditNote);
        assertTrue(result.isValid(), () -> "Expected a valid credit note, got: " + result.errors());

        // directly after cbc:PaymentMeansCode, the position UBL 2.1 gives it in PaymentMeansType
        assertThat(creditNote.xmlRoot().toXML(), stringContainsInOrder(Arrays.asList(
            "<cac:PaymentMeans>"
            , "</cbc:PaymentMeansCode>"
            , "<cbc:PaymentDueDate>" + DUE_DATE + "</cbc:PaymentDueDate>"
            , "<cac:PayeeFinancialAccount>"
        )));
    }

    private static ValidationResult validate(CreditNote creditNote) {
        return PeppolBillingApi.create(creditNote).validate();
    }

    private static PaymentMeans paymentMeans() {
        return new PaymentMeans(new PaymentMeansCode("58"))
            .withPayeeFinancialAccount(new PayeeFinancialAccount("NO6960650514745"));
    }

    private static CreditNote creditNote(PaymentMeans paymentMeans) {
        final TaxScheme vat = new TaxScheme("VAT");

        final AccountingSupplierParty supplier = new AccountingSupplierParty(new Party(
            new EndpointID("916725280").withSchemeID("0192")
            , new PostalAddress(new Country("NO"))
            , new PartyLegalEntity("Acme Cargo AS")
        ).withPartyTaxScheme(new PartyTaxScheme("NO916725280MVA", vat)));

        final AccountingCustomerParty customer = new AccountingCustomerParty(new Party(
            new EndpointID("916725280").withSchemeID("0192")
            , new PostalAddress(new Country("NO"))
            , new PartyLegalEntity("Acme As")
        ));

        final TaxTotal taxTotal = new TaxTotal(new TaxAmount("25.00", "NOK")).withTaxSubtotal(new TaxSubtotal(
            new TaxableAmount("100.00", "NOK")
            , new TaxAmount("25.00", "NOK")
            , new TaxCategory("S", vat).withPercent("25")
        ));

        final LegalMonetaryTotal legalMonetaryTotal = new LegalMonetaryTotal(
            new LineExtensionAmount("100.00", "NOK")
            , new TaxExclusiveAmount("100.00", "NOK")
            , new TaxInclusiveAmount("125.00", "NOK")
            , new PayableAmount("125.00", "NOK")
        );

        final CreditNoteLine creditNoteLine = new CreditNoteLine(
            "1"
            , new CreditedQuantity("1", "STK")
            , new LineExtensionAmount("100.00", "NOK")
            , new Item("Frakt", new ClassifiedTaxCategory("S", vat).withPercent("25"))
            , new Price(new PriceAmount("100.00", "NOK"))
        );

        return new CreditNote("12345678910", "2020-11-19", "NOK", supplier, customer, taxTotal, legalMonetaryTotal)
            .withInvoiceTypeCode(381)
            .withBuyerReference("n/a")
            .withInvoiceLine(creditNoteLine)
            .withPaymentMeans(paymentMeans);
    }
}
