package ma.gestionlocative.locataires.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocataireResponseDto {
    private UUID id;
    private String nom;
    private String prenom;
    private String cin;
    private String email;
    private String telephone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
