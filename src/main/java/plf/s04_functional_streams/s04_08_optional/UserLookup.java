package plf.s04_functional_streams.s04_08_optional;

import java.util.List;
import java.util.Optional;

public final class UserLookup {

    public record User(String id, String email, Optional<String> middleName) {}

    private final List<User> users;

    public UserLookup(List<User> users) {
        this.users = List.copyOf(users);
    }

    public Optional<User> findById(String id) {
        return users.stream().filter(u -> u.id().equals(id)).findFirst();
    }

    private int auditCalls = 0;

    public int auditCalls() {
        return auditCalls;
    }

    String audit(String reason) {
        auditCalls++;
        if (reason == null) {
            throw new NullPointerException("audit reason was null");
        }
        return "";
    }

    public String emailOrAudit(String id) {
        return findById(id)
                .map(User::email)
                .filter(e -> !e.isBlank())
                .orElse(audit("missing:" + id));   // <-- Task 1: this argument runs every call
    }

    public Optional<String> middleNameOf(String id) {
        throw new UnsupportedOperationException(
                "TODO Task 2: return the user's middleName; target type is Optional<String>");
    }
}
