package com.gestionlocative.quittances;

import com.gestionlocative.quittances.dto.ArchiveBrouillonResponse;
import com.gestionlocative.quittances.entity.Quittance;
import com.gestionlocative.quittances.repository.QuittanceRepository;
import com.gestionlocative.quittances.service.ConsultationArchivesBrouillonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConsultationArchivesBrouillonServiceTest {
    private QuittanceRepository repository;
    private JdbcTemplate jdbc;
    private ConsultationArchivesBrouillonService service;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void preparer() {
        repository = mock(QuittanceRepository.class);
        jdbc = mock(JdbcTemplate.class);
        service = new ConsultationArchivesBrouillonService(repository, jdbc);
    }

    @Test
    void refuseUneIdentiteAbsente() {
        assertThrows(AccessDeniedException.class, () -> service.consulter(null, id, 0, 10));
        verifyNoInteractions(repository, jdbc);
    }

    @Test
    void refuseUnLocataireAvantTouteLecture() {
        assertThrows(AccessDeniedException.class,
                () -> service.consulter(identite("LOCATAIRE", "LOC-1"), id, 0, 10));
        verifyNoInteractions(repository, jdbc);
    }

    @Test
    void refuseLeDocumentDunAutreBailleur() {
        when(repository.findByIdAndBailleurId(id, "BAIL-2")).thenReturn(Optional.empty());
        var erreur = assertThrows(ResponseStatusException.class,
                () -> service.consulter(identite("BAILLEUR", "BAIL-2"), id, 0, 10));
        assertEquals(404, erreur.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void refuseLesBornesDePaginationInvalides() {
        for (int[] limites : List.of(new int[]{-1, 10}, new int[]{1_000_001, 10},
                new int[]{0, 0}, new int[]{0, 101})) {
            var erreur = assertThrows(ResponseStatusException.class,
                    () -> service.consulter(identite("BAILLEUR", "BAIL-1"), id,
                            limites[0], limites[1]));
            assertEquals(400, erreur.getStatusCode().value());
        }
        verifyNoInteractions(repository, jdbc);
    }

    @Test
    void retourneUnePageVideSansArchive() {
        when(repository.findByIdAndBailleurId(id, "BAIL-1"))
                .thenReturn(Optional.of(mock(Quittance.class)));
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(id))).thenReturn(0L);
        doReturn(List.of()).when(jdbc).query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<ArchiveBrouillonResponse>>any(),
                eq(id), eq(10), eq(0L));
        var page = service.consulter(identite("BAILLEUR", "BAIL-1"), id, 0, 10);
        assertEquals(0, page.totalElements());
        assertEquals(0, page.totalPages());
        assertTrue(page.contenu().isEmpty());
    }

    @Test
    void retourneLaPageDemandeeAvecUnTriStable() {
        var archive = new ArchiveBrouillonResponse(UUID.randomUUID(), id, 2L,
                "BROUILLON", "a".repeat(64), 1500L, Instant.now());
        when(repository.findByIdAndBailleurId(id, "BAIL-1"))
                .thenReturn(Optional.of(mock(Quittance.class)));
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(id))).thenReturn(11L);
        doReturn(List.of(archive)).when(jdbc).query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<ArchiveBrouillonResponse>>any(),
                eq(id), eq(10), eq(10L));
        var page = service.consulter(identite("BAILLEUR", "BAIL-1"), id, 1, 10);
        assertEquals(11, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(1, page.page());
        assertEquals(List.of(archive), page.contenu());
        verify(jdbc).query(contains("ORDER BY cree_le DESC, id DESC"),
                org.mockito.ArgumentMatchers.<RowMapper<ArchiveBrouillonResponse>>any(),
                eq(id), eq(10), eq(10L));
        assertThrows(UnsupportedOperationException.class, () -> page.contenu().clear());
    }

    private JwtAuthenticationToken identite(String role, String identifiant) {
        Jwt jwt = Jwt.withTokenValue("test")
                .header("alg", "RS256").subject("test-utilisateur")
                .claim("BAILLEUR".equals(role) ? "bailleur_id" : "locataire_id", identifiant)
                .build();
        return new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
