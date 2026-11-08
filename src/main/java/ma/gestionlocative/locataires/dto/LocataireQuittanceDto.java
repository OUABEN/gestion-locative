package ma.gestionlocative.locataires.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocataireQuittanceDto {
    private UUID id;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String cin;
    private String email;
    private String telephone;
}
