package it.whodas.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.whodas.common.dto.NotificationDto;
import it.whodas.common.exception.WhodasException;
import it.whodas.notification.domain.Notification;
import it.whodas.notification.repository.NotificationRepository;

/**
 * Servizio di notifica: salva la notifica nel DB e la spinge in tempo reale
 * al front-end tramite SSE (Server-Sent Events).
 *
 * Il servizio non deve essere "chiamato" dal FE per ricevere notifiche:
 * il push avviene automaticamente tramite connessione SSE. Il FE puo' tuttavia
 * chiamare i metodi di lettura/gestione per la cronologia e l'aggiornamento
 * dello stato (letto, eliminato).
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final SseEmitterRegistry registry;

    public NotificationService(NotificationRepository repository, SseEmitterRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    /**
     * Crea una notifica, la salva nel DB e la invia in tempo reale al FE
     * tramite SSE.
     */
    public NotificationDto create(NotificationDto dto) {
        LocalDateTime now = LocalDateTime.now();

        Notification entity = new Notification();
        entity.setTipoObj(dto.getTipoObj() != null ? dto.getTipoObj() : "notifiche");
        entity.setTypeMessage(dto.getTypeMessage() != null ? dto.getTypeMessage() : "Info");
        entity.setOggetto(dto.getOggetto());
        entity.setDescription(dto.getDescription());
        entity.setDataSpedizione(dto.getDataSpedizione() != null ? dto.getDataSpedizione() : now);
        entity.setDataLettura(dto.getDataLettura());
        entity.setDataElimina(dto.getDataElimina());
        entity.setStato(dto.getStato() != null ? dto.getStato() : "SPEDITO");
        entity.setUserId(dto.getUserId());
        entity.setUsername(dto.getUsername());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        Notification saved = repository.save(entity);
        log.info("Notifica creata: id={} username={} oggetto={} stato={}",
                saved.getId(), saved.getUsername(), saved.getOggetto(), saved.getStato());

        NotificationDto result = toDto(saved);

        if (saved.getUsername() != null && !saved.getUsername().isBlank()) {
            registry.push(saved.getUsername(), result);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return repository.findByUsernameOrderByDataSpedizioneDesc(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> findUnread(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return repository.findByUsernameAndDataLetturaIsNullAndDataEliminaIsNullOrderByDataSpedizioneDesc(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        return repository.countByUsernameAndDataLetturaIsNullAndDataEliminaIsNull(username);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> findRead(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return repository.findByUsernameAndDataLetturaIsNotNullAndDataEliminaIsNullOrderByDataSpedizioneDesc(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> findDeleted(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return repository.findByUsernameAndDataEliminaIsNullOrderByDataSpedizioneDesc(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationDto findById(Long id) {
        Notification entity = repository.findById(id)
                .orElseThrow(() -> new WhodasException("Notification not found: " + id, "NOTIFICATION_NOT_FOUND"));
        return toDto(entity);
    }

    public NotificationDto markAsRead(Long id, String username) {
        if (username == null || username.isBlank()) {
            throw new WhodasException("Username is required", "VALIDATION_ERROR");
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = repository.markAsRead(id, username, now);
        if (updated == 0) {
            throw new WhodasException("Notification not found or not owned by user: " + id, "NOTIFICATION_NOT_FOUND");
        }
        log.info("Notifica letta: id={} username={}", id, username);
        return toDto(repository.findById(id)
                .orElseThrow(() -> new WhodasException("Notification not found: " + id, "NOTIFICATION_NOT_FOUND")));
    }

    public NotificationDto deleteLogical(Long id, String username) {
        if (username == null || username.isBlank()) {
            throw new WhodasException("Username is required", "VALIDATION_ERROR");
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = repository.deleteLogical(id, username, now);
        if (updated == 0) {
            throw new WhodasException("Notification not found or not owned by user: " + id, "NOTIFICATION_NOT_FOUND");
        }
        log.info("Notifica eliminata logicamente: id={} username={}", id, username);
        return toDto(repository.findById(id)
                .orElseThrow(() -> new WhodasException("Notification not found: " + id, "NOTIFICATION_NOT_FOUND")));
    }

    public void markAllAsRead(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int count = repository.markAllAsRead(username, now);
        log.info("Segnate lette {} notifiche per username={}", count, username);
    }

    private NotificationDto toDto(Notification entity) {
        NotificationDto dto = new NotificationDto();
        dto.setId(entity.getId());
        dto.setTipoObj(entity.getTipoObj());
        dto.setTypeMessage(entity.getTypeMessage());
        dto.setOggetto(entity.getOggetto());
        dto.setDescription(entity.getDescription());
        dto.setDataSpedizione(entity.getDataSpedizione());
        dto.setDataLettura(entity.getDataLettura());
        dto.setDataElimina(entity.getDataElimina());
        dto.setStato(entity.getStato());
        dto.setUserId(entity.getUserId());
        dto.setUsername(entity.getUsername());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
