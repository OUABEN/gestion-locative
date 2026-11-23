# Phase 6 - Controle de securite du module Quittances

**Periode :** 22-23 novembre 2026  
**Branche :** akram-jwt-commun-quittances  
**Commit de reference :** 4e2246a  
**Perimetre :** backend Quittances (Maven), frontend React partage (npm), frontend statique servi par Nginx temporaire (ZAP).

## 1. OWASP ZAP Baseline - frontend de test isole

Analyse passive sur une compilation statique de React dans un reseau Docker interne, sans port publie et sans acces au backend reel. Resultat : aucune alerte elevee ; 2 regles de niveau moyen ; 2 de niveau faible ; 1 informative.

| Niveau | Regles signalees |
|---|---|
| Moyen | CSP absente ; en-tete anti-clickjacking absent |
| Faible | Informations serveur dans l'en-tete Server ; X-Content-Type-Options manquant |
| Informatif | Application web moderne |

Ces constats portent sur les en-tetes du **serveur Nginx temporaire**, et ne qualifient pas le serveur backend ni un futur deploiement.

**Preuves :** [zap/scan-20261122-213543-90da4644e9/rapport-zap.md](zap/scan-20261122-213543-90da4644e9/rapport-zap.md), [zap/scan-20261122-213543-90da4644e9/rapport-zap.json](zap/scan-20261122-213543-90da4644e9/rapport-zap.json), [zap/scan-20261122-213543-90da4644e9/rapport-zap.html](zap/scan-20261122-213543-90da4644e9/rapport-zap.html).

## 2. Snyk Open Source - backend Maven

108 dependances analysees ; **18 vulnérabilités signalees : 5 critiques, 4 elevees, 8 moyennes, 1 faible**. Code Snyk 1 : vulnerabilites trouvees, pas une erreur de scan.

Priorites a evaluer :

- Spring Boot / Spring Security : plusieurs alertes critiques sur la gestion des chemins, la securite des utilisateurs et l'execution d'expressions ; examiner une version de maintenance corrigee de Spring Boot (Snyk mentionne 4.1.3), sans melanger des versions de starters.
- Jackson Databind : 2.21.7 expose selon Snyk ; correctif annonce en 2.21.8. Verifier la resolution Maven et la compatibilite.
- Flyway : dependance transitive Jackson 3.1.7 signalee ; Snyk recommande Flyway 13.5.0. **Migration majeure : ne pas appliquer automatiquement**, verifier d'abord les versions supportees et les migrations.
- JAXB jaxb-core : alerte elevee XXE sans correctif direct indique par Snyk ; examiner les conditions reelles d'exploitation et les mitigations.

**Preuve :** [snyk/scan-20261123-053703-e0f9adcfd4/rapport-snyk-backend.json](snyk/scan-20261123-053703-e0f9adcfd4/rapport-snyk-backend.json).

## 3. Snyk Open Source - frontend npm partage

94 dependances analysees ; **2 vulnerabilites signalees : 1 elevee et 1 moyenne**.

Les deux alertes concernent react-router@6.30.6, installe via react-router-dom@6.30.6 :

- Open Redirect (HIGH) : risque de redirection ouverte.
- Unsafe Reflection (MEDIUM) : selection non sure de classes ou de code a partir de donnees externes.

Snyk suggere react-router-dom@7.18.0 pour corriger ces deux alertes. Cette mise a niveau majeure doit etre evaluee avec les collegues avant toute modification du frontend commun.

**Preuve :** [snyk/frontend-20261123-054139-0aaa09be1e/rapport-snyk-frontend.json](snyk/frontend-20261123-054139-0aaa09be1e/rapport-snyk-frontend.json).

## 4. Synthese et limites

| Niveau Snyk | Backend | Frontend | Total |
|---|---:|---:|---:|
| Critique | 5 | 0 | 5 |
| Eleve | 4 | 1 | 5 |
| Moyen | 8 | 1 | 9 |
| Faible | 1 | 0 | 1 |
| **Total** | **18** | **2** | **20** |

Les alertes Snyk identifient des dependances potentiellement vulnerables ; leur exploitabilite dans les fonctions exposees par Quittances n'a pas ete demontree. Le scan ZAP est une exploration passive d'un frontend statique non authentifie ; il ne constitue **pas un test de penetration du backend**.

**Etat de correction :** aucune vulnerabilite corrigee dans cette phase. Aucun package ni fichier de configuration du projet n'a ete mis a jour.

## 5. Suite recommandee

1. Priorite haute : evaluer les correctifs Spring Boot/Spring Security et Jackson dans une copie de travail isolee ; verifier la resolution Maven puis les tests unitaires et d'integration en environnement de test.
2. Examiner le correctif Flyway et ses effets sur les migrations sans lancer celles-ci sur la base reelle.
3. Partager les deux alertes React Router avec l'equipe avant de modifier la navigation commune.
4. Mettre en place les en-tetes HTTP de securite dans la configuration du **futur deploiement**, puis refaire un ZAP Baseline sur cet environnement.
5. Reexecuter Snyk apres correctifs pour comparer les alertes avant/apres, en preservant les rapports initiaux.
