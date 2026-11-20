package it.whodas.userservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registered_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredUser {

    public static final String STATUS_ACTIVE = "ATTIVO";
    public static final String STATUS_EXPIRED = "SCADUTO";
    public static final String STATUS_DELETED = "CANCELLATO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "fiscal_code", nullable = false, unique = true, length = 16)
    private String fiscalCode;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "comune", length = 100)
    private String comune;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "ldap_uid", nullable = false, unique = true, length = 100)
    private String ldapUid;

    @Column(name = "keycloak_user_id", length = 100)
    private String keycloakUserId;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
