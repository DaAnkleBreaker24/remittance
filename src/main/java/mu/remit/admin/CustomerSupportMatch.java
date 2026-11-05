package mu.remit.admin;

public record CustomerSupportMatch(
        String customerId,
        String fullName,
        String phoneMasked,
        String reference) {
}
