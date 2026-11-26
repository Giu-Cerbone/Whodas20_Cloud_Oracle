package it.whodas.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token refresh request")
public class RefreshRequest {

    @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiIs...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}