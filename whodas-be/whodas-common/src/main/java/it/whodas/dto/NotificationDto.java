package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Notification data transfer object")
public class NotificationDto {

    @Schema(description = "Notification ID", example = "1")
    private Long id;

    @Schema(description = "Object type", example = "notifiche", defaultValue = "notifiche")
    private String tipoObj = "notifiche";

    @Schema(description = "Message type", example = "INFO")
    private String typeMessage;

    @Schema(description = "Subject", example = "New assessment available")
    private String oggetto;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Sending timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime dataSpedizione;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "Reading timestamp", example = "2024-01-15T11:00:00")
    private LocalDateTime dataLettura;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "Deletion timestamp", example = "2024-01-15T12:00:00")
    private LocalDateTime dataElimina;

    @Schema(description = "Status", example = "SPEDITO")
    private String stato;

    @Schema(description = "User ID", example = "user123")
    private String userId;

    @Schema(description = "Username", example = "mario.rossi")
    private String username;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T11:00:00")
    private LocalDateTime updatedAt;

    public boolean isRead() {
        return dataLettura != null;
    }

    public boolean isDeleted() {
        return dataElimina != null;
    }
}
