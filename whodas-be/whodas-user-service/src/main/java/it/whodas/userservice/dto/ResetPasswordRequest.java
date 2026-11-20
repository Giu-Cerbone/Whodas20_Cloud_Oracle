package it.whodas.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
public class ResetPasswordRequest {

    @NotBlank(message = "Token obbligatorio")
    private String token;

    @NotBlank(message = "Nuova password obbligatoria")
    @Size(min = 6, message = "La password deve essere di almeno 6 caratteri")
    private String newPassword;

    @NotBlank(message = "Conferma password obbligatoria")
    private String confirmPassword;
}