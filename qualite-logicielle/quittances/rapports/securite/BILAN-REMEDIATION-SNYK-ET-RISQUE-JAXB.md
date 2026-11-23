# Bilan de remediation Snyk et risque JAXB residuel - Quittances

**Date de bilan :** 2026-11-23
**Branche :** `akram-jwt-commun-quittances`
**Commit evalue :** `7cd83f3`
**Perimetre :** backend `backend/backend_gestion_quittances` uniquement.
**Versions :** Spring Boot 4.1.3 ; Logback 1.6.5 ; Java 21.

## 1. Remediations effectuees

Deux changements cibles du POM Quittances ont ete enregistres localement :

- `4cfc9c4` : parent Spring Boot 4.1.2 vers 4.1.3.
- `7cd83f3` : ajout de la propriete Maven `logback.version=1.6.5`.

Aucun changement du frontend partage ni des autres modules n'est revendique par ce bilan. Les modifications n'ont pas ete poussees par les scripts de verification.

## 2. Scan Snyk Maven - avant et apres

| Etat | Critiques | Elevees | Moyennes | Faibles | Total |
|---|---:|---:|---:|---:|---:|
| Initial - Boot 4.1.2 | 5 | 4 | 8 | 1 | 18 |
| Final - commit `7cd83f3` | 0 | 1 | 0 | 0 | 1 |

**Bilan :** 17 alertes initiales ne sont plus detectees dans le scan final (reduction de 94,4 %). Le dernier scan a ete realise sur une copie dont le SHA-256 correspond au POM reel commite. Cela n'equivaut pas a une garantie de securite de l'application.

Preuve historique conservee : `snyk/scan-20261123-053703-e0f9adcfd4/rapport-snyk-backend.json`.
Preuve finale epuree : [PREUVE-SNYK-FINAL-7cd83f3.json](PREUVE-SNYK-FINAL-7cd83f3.json).
SHA-256 du JSON initial : `CFA1460F238ABE9DB84D166C049C093B08B8368A10974D412EB24CDB9EA9E18A`.
SHA-256 du JSON final original : `DC74AB9E5BECB62BC19AE87226305295EAB1382BD41D7D44785B3C5FAA9A668E` (original conserve uniquement dans `%TEMP%` du poste).

## 3. Risque residuel HIGH - JAXB XXE

- **Alerte :** `SNYK-JAVA-ORGGLASSFISHJAXB-20883142` (CVE-2026-92877).
- **Bibliotheque :** `org.glassfish.jaxb:jaxb-core` version `4.0.9`.
- **Gravite :** HIGH. **Statut : ouvert, non corrige.**
- **Version corrigee :** aucune version `fixedIn` indiquee dans le JSON Snyk de ce scan ; ce constat est date et doit etre re-verifie avant une future decision.
- **Origine :** dependance transitive introduite par Spring Data JPA / Hibernate et `jaxb-runtime`, non declaree directement dans le POM du module.

Chaine fournie par Snyk : `service-quittances` -> `spring-boot-starter-data-jpa` -> `spring-boot-data-jpa` -> `spring-boot-hibernate` -> `hibernate-core` -> `jaxb-runtime` -> `jaxb-core`.

**Audit statique :** 107 fichiers textuels de `src/main` inspectes ; aucun appel explicite JAXB / Unmarshaller, aucune factory XML, aucun endpoint ou format XML explicite, aucun mapping JAXB/Hibernate XML repere avec les motifs utilises.

**Limite :** ce controle ne couvre pas tous les traitements possibles des dependances transitives, dont Hibernate. L'absence d'usage direct detecte ne demontre pas que la vulnérabilite est inexploitable. Le risque n'est ni supprime ni formellement accepte.

**Mesures recommandees :** eviter les entrees XML non fiables ; si un traitement XML est ajoute, durcir les parseurs et desactiver les entites externes/DTD inappropriees ; surveiller les correctifs JAXB publies et les versions de dependances transitive ; analyser et tester toute mise a niveau ou exclusion avant application.

Avis Snyk : https://security.snyk.io/vuln/SNYK-JAVA-ORGGLASSFISHJAXB-20883142

## 4. Verification de non-regression du candidat final

Rapports JUnit XML et classpath Maven controles :

| Groupe | Classes | Tests reussis | Erreurs/echecs/ignores |
|---|---:|---:|---:|
| unit4 | 4 | 23/23 | 0 |
| unit5 | 5 | 38/38 | 0 |
| sans-db8 | 8 | 50/50 | 0 |
| spring4 | 4 | 26/26 | 0 |
| jpa8 | 8 | 38/38 | 0 |
| http3 | 1 | 3/3 | 0 |
| **Total** | **30** | **178/178** | **0** |

Tests effectues en six lots isoles, **pas** en une execution globale unique. Les classes necessitant une base ont utilise PostgreSQL jetable en reseau Docker prive et des cles JWT publiques de test. Aucun test de production ou de preproduction n'a ete realise. Les preuves JUnit originales restent dans `%TEMP%` du poste ; ce document n'en remplace pas les XML.

## 5. Limites de perimetre et suivi

Les alertes du **frontend partage** et les constats ZAP sur le **Nginx statique temporaire** figurent dans le rapport historique `RESULTATS-SECURITE.md` ; ils ne sont pas declares corriges ici. L'audit Snyk ne remplace ni un test de penetration authentifie ni l'analyse des chemins d'exploitation.

**Action ouverte :** suivre `jaxb-core`, verifier l'exposition aux traitements XML non fiables et reexecuter Snyk lorsqu'une correction compatible est disponible. Une decision formelle de traitement ou d'acceptation du risque HIGH appartient aux responsables du projet.

**Etat Git lors de l'evaluation :** commit `7cd83f3` ; `docs/integration/` a ete preserve. Ce rapport et la preuve epuree sont prepares **sans commit ni push** pour relecture.
