package it.whodas.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegistrationRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String firstName;

    @NotBlank
    @Size(max = 255)
    private String lastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotBlank
    @Size(min = 16, max = 16)
    private String fiscalCode;

    @Size(max = 255)
    private String address;

    @Size(max = 10)
    private String postalCode;

    @Size(max = 100)
    private String comune;

    @Size(max = 100)
    private String city;

    @Size(max = 20)
    private String phone;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
}
