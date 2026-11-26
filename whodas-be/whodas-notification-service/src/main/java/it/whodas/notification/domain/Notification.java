package it.whodas.notification.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entita' per la tabella notifications.
 *
 * Rappresenta una notifica inviata dal backend al front-end. La chiave primaria
 * 'id' e' generata da una sequence esplicita (notification_seq) come richiesto
 * dalla specifica: "una chiave unica legata a una sequenza".
 *
 * Le colonne riflettono la struttura JSON richiesta:
 * {tipo_obj, type_message, oggetto, description, dataSpedizione,
 *  dataLettura, dataElimina} piu le info dell'operatore utente collegato
 * (user_id, username) e lo stato della notifica
 * ('SPEDITO','LETTO','MODIFICATO','CANCELLATO').
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Notification entity")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
    @SequenceGenerator(name = "notification_seq", sequenceName = "notification_seq", allocationSize = 1)
    @Schema(description = "Notification ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "tipo_obj", nullable = false, length = 50)
    @Schema(description = "Object type", example = "notifiche", defaultValue = "notifiche")
    private String tipoObj = "notifiche";

    @Column(name = "type_message", nullable = false, length = 50)
    @Schema(description = "Message type", example = "INFO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String typeMessage;

    @Column(name = "oggetto", nullable = false, length = 255)
    @Schema(description = "Subject", example = "New assessment available", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oggetto;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @Schema(description = "Description", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Column(name = "data_spedizione", nullable = false)
    @Schema(description = "Sending timestamp", example = "2024-01-15T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime dataSpedizione;

    @Column(name = "data_lettura")
    @Schema(description = "Reading timestamp", example = "2024-01-15T11:00:00")
    private LocalDateTime dataLettura;

    @Column(name = "data_elimina")
    @Schema(description = "Deletion timestamp", example = "2024-01-15T12:00:00")
    private LocalDateTime dataElimina;

    @Column(name = "stato", nullable = false, length = 20)
    @Schema(description = "Status", example = "SPEDITO", defaultValue = "SPEDITO")
    private String stato = "SPEDITO";

    @Column(name = "user_id", length = 255)
    @Schema(description = "User ID", example = "user123")
    private String userId;

    @Column(name = "username", length = 255)
    @Schema(description = "Username", example = "mario.rossi")
    private String username;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}