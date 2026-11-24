#!/usr/bin/env bash
# Parcours d'intégration Gestion Locative. Pré-requis : curl, jq, services démarrés.
set -u

# Bash xtrace prints function arguments, including passwords and bearer tokens.
# Disable it before reading credentials or building authenticated requests.
case $- in
  *x*)
    set +x
    printf 'Trace bash désactivée pour protéger les identifiants et jetons.\n' >&2
    ;;
esac

BIENS_URL=${BIENS_URL:-http://localhost:8081}
LOCATAIRES_URL=${LOCATAIRES_URL:-http://localhost:8082}
LOYERS_URL=${LOYERS_URL:-http://localhost:8083}
QUITTANCES_URL=${QUITTANCES_URL:-http://localhost:8084}
LOGIN_EMAIL=${LOGIN_EMAIL:-bailleur@gestion-locative.com}
LOGIN_PASSWORD=${LOGIN_PASSWORD:?LOGIN_PASSWORD est requis dans l’environnement}
TENANT_EMAIL=${TENANT_EMAIL:-}
TENANT_PASSWORD=${TENANT_PASSWORD:-}

for cmd in curl jq; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    printf 'Dépendance manquante : %s\n' "$cmd" >&2
    exit 2
  fi
done

tmpdir=$(mktemp -d)
trap 'rm -rf "$tmpdir"' EXIT
body="$tmpdir/body"
token=''
tenant_token=''
failed=0

request() {
  local method=$1 url=$2 expected=$3 data=${4:-} auth=${5:-yes}
  local -a args=(-sS -o "$body" -w '%{http_code}' -X "$method" "$url" -H 'Content-Type: application/json')
  if [[ "$auth" == yes && -n "$token" ]]; then args+=(-H "Authorization: Bearer $token"); fi
  if [[ "$auth" == tenant && -n "$tenant_token" ]]; then args+=(-H "Authorization: Bearer $tenant_token"); fi
  if [[ -n "$data" ]]; then args+=(--data "$data"); fi
  code=$(curl "${args[@]}") || code=000
  if [[ "$code" == "$expected" ]]; then result=OK; else result=KO; failed=1; fi
  printf '%-34s HTTP %s %s\n' "$step" "$code" "$result"
  if [[ "$result" == KO ]]; then
    local detail
    detail=$(jq -r 'if type == "object" then (.detail // .message // .title // empty) else empty end' "$body" 2>/dev/null || true)
    if [[ -n "$detail" ]]; then printf '  Détail serveur : %s\n' "$detail" >&2; fi
  fi
}

step='Connexion bailleur'
request POST "$BIENS_URL/api/auth/login" 200 "$(jq -nc --arg email "$LOGIN_EMAIL" --arg password "$LOGIN_PASSWORD" '{email:$email,password:$password}')" no
token=$(jq -r '.token // empty' "$body" 2>/dev/null || true)
if [[ -z "$token" ]]; then printf 'Token bailleur absent; arrêt du scénario.\n' >&2; exit 1; fi

step='Contrôle sans jeton (Biens)'
request GET "$BIENS_URL/api/biens" 401 '' none

suffix="$(date +%s)${RANDOM}"
step='Création du bien'
request POST "$BIENS_URL/api/biens" 201 "$(jq -nc --arg adresse "${SCENARIO_ADDRESS:-Adresse scénario $suffix}" '{adresse:$adresse,ville:"Rabat",type:"APPARTEMENT",surface:60,prixMensuel:5000,charges:0}')"
bien_id=$(jq -r '.id // .bienId // empty' "$body" 2>/dev/null || true)
if [[ -z "$bien_id" ]]; then printf 'Identifiant du bien absent; arrêt du scénario.\n' >&2; exit 1; fi

step='Création du locataire (CIN unique)'
cin="SC${suffix}"
request POST "$LOCATAIRES_URL/api/locataires" 201 "$(jq -nc --arg cin "$cin" --arg email "${SCENARIO_TENANT_EMAIL:-locataire-$suffix@example.test}" '{nom:"Scenario",prenom:"Locataire",cin:$cin,email:$email,telephone:"0600000000"}')"
locataire_id=$(jq -r '.id // .locataireId // empty' "$body" 2>/dev/null || true)
if [[ -z "$locataire_id" ]]; then printf 'Identifiant du locataire absent; arrêt du scénario.\n' >&2; exit 1; fi

step='Refus CIN déjà utilisé (409 attendu)'
request POST "$LOCATAIRES_URL/api/locataires" 409 "$(jq -nc --arg cin "$cin" '{nom:"Scenario",prenom:"Doublon",cin:$cin}')"

year=$(date +%Y); month=$(date +%-m); month_iso=$(date +%m)
step='Création du bail'
request POST "$LOYERS_URL/api/baux" 201 "$(jq -nc --arg bien "$bien_id" --arg loc "$locataire_id" --arg debut "$year-$month_iso-01" '{bienId:$bien,locataireId:$loc,dateDebut:$debut,montantLoyer:5000,depotGarantie:5000}')"
bail_id=$(jq -r '.id // .bailId // empty' "$body" 2>/dev/null || true)
if [[ -z "$bail_id" ]]; then printf 'Identifiant du bail absent; arrêt du scénario.\n' >&2; exit 1; fi

step='Refus second bail sur le même bien (409 attendu)'
request POST "$LOYERS_URL/api/baux" 409 "$(jq -nc --arg bien "$bien_id" --arg loc "$locataire_id" --arg debut "$year-$month_iso-01" '{bienId:$bien,locataireId:$loc,dateDebut:$debut,montantLoyer:5000}')"

step='Recherche de l’échéance du mois'
request GET "$LOYERS_URL/api/echeances?bailId=$bail_id" 200
echeance_id=$(jq -r --argjson mois "$month" --argjson annee "$year" '[.[] | select(.mois == $mois and .annee == $annee)][0].id // empty' "$body" 2>/dev/null || true)
if [[ -z "$echeance_id" ]]; then
  step='Génération de l’échéance du mois'
  request POST "$LOYERS_URL/api/echeances/generation/baux/$bail_id" 201 "$(jq -nc --argjson mois "$month" --argjson annee "$year" '{mois:$mois,annee:$annee}')"
  echeance_id=$(jq -r '.id // .echeanceId // empty' "$body" 2>/dev/null || true)
fi
if [[ -z "$echeance_id" ]]; then printf 'Identifiant de l’échéance absent; arrêt du scénario.\n' >&2; exit 1; fi

step='Enregistrement du paiement'
request POST "$LOYERS_URL/api/paiements" 201 "$(jq -nc --arg echeance "$echeance_id" '{echeanceId:$echeance,montant:5000,modePaiement:"VIREMENT"}')"

start="$year-$(printf '%02d' "$month")-01"
end=$(date -d "$start +1 month -1 day" +%F)
step='Préparation de la quittance'
request POST "$QUITTANCES_URL/api/v1/quittances" 201 "$(jq -nc --arg bail "$bail_id" --arg debut "$start" --arg fin "$end" '{bailId:$bail,periodeDebut:$debut,periodeFin:$fin}')"
quittance_id=$(jq -r '.id // .quittanceId // empty' "$body" 2>/dev/null || true)
if [[ -z "$quittance_id" ]]; then printf 'Identifiant de la quittance absent; arrêt du scénario.\n' >&2; exit 1; fi

step='Validation de la quittance'
request POST "$QUITTANCES_URL/api/v1/quittances/$quittance_id/valider" 200 '{"confirmation":true}'
step='Émission PDF de démonstration'
request POST "$QUITTANCES_URL/api/v1/quittances/$quittance_id/emission-demo" 200 '{"confirmation":true}'
step='Téléchargement PDF locataire (jeton locataire requis)'
if [[ -n "$TENANT_EMAIL" && -n "$TENANT_PASSWORD" ]]; then
  request POST "$BIENS_URL/api/auth/login" 200 "$(jq -nc --arg email "$TENANT_EMAIL" --arg password "$TENANT_PASSWORD" '{email:$email,password:$password}')" no
  tenant_token=$(jq -r '.token // empty' "$body" 2>/dev/null || true)
  request GET "$QUITTANCES_URL/api/v1/quittances/$quittance_id/emission-demo/pdf" 200 '' tenant
else
  printf '%-34s HTTP N/A %s\n' "$step" 'NON TESTÉ (définir TENANT_EMAIL et TENANT_PASSWORD)'
fi
step='Envoi e-mail capturé par Mailpit'
request POST "$QUITTANCES_URL/api/v1/quittances/$quittance_id/envois-mailpit" 200 "$(jq -nc --arg id "$(cat /proc/sys/kernel/random/uuid)" '{requeteId:$id,confirmation:true}')"

if (( failed )); then exit 1; fi
