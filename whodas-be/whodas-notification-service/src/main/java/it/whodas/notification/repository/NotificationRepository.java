package it.whodas.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import it.whodas.notification.domain.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUsernameOrderByDataSpedizioneDesc(String username);

    List<Notification> findByUsernameAndDataLetturaIsNullAndDataEliminaIsNullOrderByDataSpedizioneDesc(String username);

    List<Notification> findByUsernameAndDataEliminaIsNullOrderByDataSpedizioneDesc(String username);

    List<Notification> findByUsernameAndDataLetturaIsNotNullAndDataEliminaIsNullOrderByDataSpedizioneDesc(String username);

    long countByUsernameAndDataLetturaIsNullAndDataEliminaIsNull(String username);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.dataLettura = :dataLettura, n.stato = 'LETTO', n.updatedAt = :dataLettura " +
           "WHERE n.id = :id AND n.username = :username AND n.dataElimina IS NULL")
    int markAsRead(@Param("id") Long id, @Param("username") String username, @Param("dataLettura") java.time.LocalDateTime dataLettura);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.dataElimina = :dataElimina, n.stato = 'CANCELLATO', n.updatedAt = :dataElimina " +
           "WHERE n.id = :id AND n.username = :username AND n.dataElimina IS NULL")
    int deleteLogical(@Param("id") Long id, @Param("username") String username, @Param("dataElimina") java.time.LocalDateTime dataElimina);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.dataLettura = :dataLettura, n.stato = 'LETTO', n.updatedAt = :dataLettura " +
           "WHERE n.username = :username AND n.dataElimina IS NULL AND n.dataLettura IS NULL")
    int markAllAsRead(@Param("username") String username, @Param("dataLettura") java.time.LocalDateTime dataLettura);
}
