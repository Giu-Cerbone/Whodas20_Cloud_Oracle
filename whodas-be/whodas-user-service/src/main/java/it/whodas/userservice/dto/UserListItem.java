package it.whodas.userservice.dto;

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
public class UserListItem {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String fiscalCode;
    private String status;
    private LocalDateTime registeredAt;
}