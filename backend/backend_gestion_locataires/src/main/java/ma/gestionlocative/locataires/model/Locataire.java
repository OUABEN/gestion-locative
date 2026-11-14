package ma.gestionlocative.locataires.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "locataires", indexes = {
    @Index(name = "idx_locataire_cin", columnList = "cin", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Locataire {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @NotBlank(message = "Le CIN est obligatoire")
    @Column(name = "cin", nullable = false, unique = true, length = 20)
    private String cin;

    @Email(message = "Format d'email invalide")
    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Locataire(UUID id, String nom, String prenom, String email, String telephone, String cin) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.cin = cin;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.cin != null) {
            this.cin = this.cin.trim().toUpperCase();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        if (this.cin != null) {
            this.cin = this.cin.trim().toUpperCase();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Locataire locataire = (Locataire) o;
        return Objects.equals(id, locataire.id) || Objects.equals(cin, locataire.cin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cin);
    }
}
