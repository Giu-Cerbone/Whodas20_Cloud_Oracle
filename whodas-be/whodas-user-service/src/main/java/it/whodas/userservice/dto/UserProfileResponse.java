package it.whodas.userservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private String username;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String fiscalCode;
    private String address;
    private String postalCode;
    private String comune;
    private String city;
    private String phone;
    private String email;
    private String status;
    private LocalDateTime registeredAt;
    private Boolean passwordTemporary = false;
}