"use strict";

// Interface DarLoc : routes internes, JWT en mémoire et API existantes.
// Aucun paiement ni état documentaire n'est calculé ou modifié localement.
(() => {
    const $ = id => document.getElementById(id);
    const screen = $("screen");
    const modal = $("confirmation");
    const state = {
        token: "", owner: false, sequence: 0, controller: null,
        pending: false, lastHash: "#/documents", listHash: "#/documents",
        pdfUrl: null, modalAction: null, modalDocument: null,
        draft: {bailId: "", periodeDebut: "", periodeFin: ""},
        creationUnknown: false
    };
    const labels = {
        BROUILLON: "Brouillon", EN_ATTENTE_VALIDATION: "À valider",
        EN_ATTENTE_SIGNATURE: "En attente de signature", EMIS: "Émis",
        A_REEXAMINER: "À réexaminer", ANNULE: "Annulé", REMPLACE: "Remplacé",
        NON_SIGNE: "Non signé", EN_ATTENTE: "En attente", SIGNE_VERIFIE: "Signature vérifiée",
        QUITTANCE_INTEGRALE: "Quittance intégrale", RECU_PARTIEL: "Reçu partiel",
        SUCCES: "Succès", ECHEC: "Échec", VALIDATION: "Validation",
        ANNULATION: "Annulation", ARCHIVAGE_BROUILLON: "Archivage du brouillon",
        CREATION: "Création", CREATION_REMPLACEMENT: "Création du remplacement",
        REMPLACEMENT_PREPARE: "Remplacement préparé"
    };
    const moneyFormat = new Intl.NumberFormat("fr-MA", {style: "currency", currency: "MAD"});
    const timeFormat = new Intl.DateTimeFormat("fr-FR", {
        dateStyle: "short", timeStyle: "short", timeZone: "Africa/Casablanca"
    });
    const esc = value => String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[char]);
    const label = value => labels[value] || String(value || "Non renseigné").replaceAll("_", " ");
    const money = value => value !== null && value !== undefined && Number.isFinite(Number(value))
        ? moneyFormat.format(Number(value)) : "Non renseigné";
    function date(value) {
        const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value || "");
        return match ? `${match[3]}/${match[2]}/${match[1]}` : "Non renseignée";
    }
    function time(value) {
        const parsed = new Date(value);
        return value && !Number.isNaN(parsed.getTime()) ? timeFormat.format(parsed) : "Non renseigné";
    }
    function badge(value) {
        const kind = ["ANNULE", "REMPLACE", "A_REEXAMINER", "ECHEC"].includes(value)
            ? "cancelled" : ["EN_ATTENTE", "EN_ATTENTE_SIGNATURE", "EN_ATTENTE_VALIDATION"].includes(value)
                ? "wait" : ["EMIS", "SIGNE_VERIFIE", "SUCCES"].includes(value) ? "issued" : "";
        return `<span class="badge-state ${kind}">${esc(label(value))}</span>`;
    }
    function notice(text = "", error = false) {
        $("notice").textContent = text;
        $("notice").className = error ? "notice error" : "notice";
        $("notice").hidden = !text;
    }
    function view(html, title) {
        screen.innerHTML = html;
        screen.removeAttribute("aria-busy");
        $("breadcrumb").textContent = title;
        document.title = `DarLoc · ${title}`;
        screen.querySelector("h1")?.setAttribute("tabindex", "-1");
        screen.querySelector("h1")?.focus({preventScroll: true});
    }
    function loading(title) {
        screen.setAttribute("aria-busy", "true");
        screen.innerHTML = `<div class="loading-screen"><div class="loading-top" role="status"><span class="spinner" aria-hidden="true"></span>${esc(title)}</div><div aria-hidden="true"><div class="skeleton short"></div><div class="skeleton"></div><div class="skeleton block"></div></div></div>`;
    }
    function previewAllowed(doc) {
        return state.owner && !doc.emisLe && ["BROUILLON", "EN_ATTENTE_VALIDATION", "EN_ATTENTE_SIGNATURE"].includes(doc.statut);
    }
    function validationAllowed(doc) {
        return state.owner && !doc.emisLe && doc.statutSignature === "NON_SIGNE" &&
            ["BROUILLON", "EN_ATTENTE_VALIDATION"].includes(doc.statut);
    }
    function cancellationAllowed(doc) {
        return state.owner && !["ANNULE", "REMPLACE"].includes(doc.statut);
    }
    function releasePdf() {
        if (state.pdfUrl) URL.revokeObjectURL(state.pdfUrl);
        state.pdfUrl = null;
    }
    function ownerFromValidatedToken(token) {
        try {
            const part = token.split(".")[1].replaceAll("-", "+").replaceAll("_", "/");
            const bytes = Uint8Array.from(atob(part.padEnd(Math.ceil(part.length / 4) * 4, "=")), c => c.charCodeAt(0));
            const roles = JSON.parse(new TextDecoder().decode(bytes)).roles;
            return Array.isArray(roles) && roles.includes("BAILLEUR") && !roles.includes("LOCATAIRE");
        } catch { return false; }
    }
    function setAccount() {
        $("logout").hidden = !state.token;
        $("role-label").hidden = !state.token;
        $("role-label").textContent = state.owner ? "Bailleur" : "Compte connecté";
        $("nav-nouveau").hidden = !state.token || !state.owner;
    }
    function clearSession() {
        state.controller?.abort();
        state.token = "";
        state.owner = false;
        state.draft = {bailId: "", periodeDebut: "", periodeFin: ""};
        state.creationUnknown = false;
        state.modalAction = null;
        state.modalDocument = null;
        $("reason").value = "";
        if (modal.open) modal.close();
        releasePdf();
        setAccount();
    }
    class ApiError extends Error {
        constructor(message, status = 0, accepted = false) {
            super(message); this.status = status; this.accepted = accepted;
        }
    }
    async function api(path, options = {}) {
        const controller = new AbortController();
        const parent = options.signal;
        const relay = () => controller.abort();
        if (parent?.aborted) controller.abort();
        parent?.addEventListener("abort", relay, {once: true});
        let timeout = false;
        const timer = setTimeout(() => { timeout = true; controller.abort(); }, 30000);
        const method = options.method || "GET";
        try {
            const headers = {Authorization: `Bearer ${state.token}`, Accept: options.csv ? "text/csv" : options.pdf ? "application/pdf" : options.zip ? "application/zip" : "application/json"};
            if (options.body !== undefined) headers["Content-Type"] = "application/json";
            const response = await fetch(path, {
                method, headers, body: options.body === undefined ? undefined : JSON.stringify(options.body),
                credentials: "omit", cache: "no-store", redirect: "error", signal: controller.signal
            });
            if (response.status === 401) throw new ApiError("Votre accès a expiré. Reconnectez-vous avec un jeton valide.", 401);
            if (!response.ok) {
                let problem = null;
                try { problem = await response.json(); } catch { /* Réponse sans JSON exploitable. */ }
                throw new ApiError(`${problem?.detail || (response.status === 403 ? "Accès non autorisé." : "La demande n’a pas pu être traitée.")} (HTTP ${response.status})`, response.status);
            }
            if (options.noBody) return null;
            if (options.csv) {
                if (!(response.headers.get("Content-Type") || "").toLowerCase().startsWith("text/csv")) throw new ApiError("La réponse reçue n’est pas un CSV.");
                return await response.blob();
            }
            if (options.zip) {
                if (!(response.headers.get("Content-Type") || "").toLowerCase().startsWith("application/zip"))
                    throw new ApiError("La réponse reçue n’est pas une archive ZIP.");
                return await response.blob();
            }
            if (options.pdf) {
                if (!(response.headers.get("Content-Type") || "").toLowerCase().startsWith("application/pdf"))
                    throw new ApiError("La réponse reçue n’est pas un PDF.");
                return await response.blob();
            }
            try { return await response.json(); }
            catch {
                if (parent?.aborted) throw new DOMException("Navigation interrompue", "AbortError");
                throw new ApiError(method === "POST"
                    ? "Opération acceptée, mais réponse illisible. Consultez la liste avant toute nouvelle tentative."
                    : "La réponse JSON du serveur est illisible.", response.status, method === "POST");
            }
        } catch (error) {
            if (parent?.aborted) throw new DOMException("Navigation interrompue", "AbortError");
            if (error instanceof ApiError) throw error;
            throw new ApiError((timeout ? "Le serveur met trop de temps à répondre." : "Connexion au serveur interrompue.") +
                (method === "POST" ? " Le résultat de l’opération est inconnu. Consultez le document avant de réessayer." : ""));
        } finally {
            clearTimeout(timer);
            parent?.removeEventListener("abort", relay);
        }
    }
    function pageNumber(value) {
        return /^\d{1,6}$/.test(value || "0") ? Number(value || 0) : 0;
    }
    function parseRoute() {
        const raw = location.hash.slice(1) || "/documents";
        const [path, query = ""] = raw.split("?");
        const params = new URLSearchParams(query);
        if (path === "/documents") return {kind: "list", page: pageNumber(params.get("page")), filters: Object.fromEntries(["bailId", "numero", "statut", "debut", "fin"].map(k => [k, params.get(k) || ""])), sort: ["creeLe", "periodeDebut", "totalPaye"].includes(params.get("tri")) ? params.get("tri") : "creeLe"};
        if (path === "/tableau-de-bord") return {kind: "tableau"};
        if (path === "/guide") return {kind: "guide"};
        if (path === "/nouveau") return {kind: "create"};
        const match = /^\/documents\/([a-fA-F0-9-]{36})(?:\/(historique|preview|archives|remplacer|versements|signature|envois|controle))?$/.exec(path);
        if (match) return {kind: match[2] || "detail", id: match[1], page: pageNumber(params.get("page"))};
        return {kind: "missing"};
    }
    function go(hash, replace = false) {
        if (state.pending) return;
        if (replace) { history.replaceState(null, "", hash); route(); }
        else if (location.hash === hash) route();
        else location.hash = hash;
    }
    function docPath(id, suffix = "") { return `#/documents/${encodeURIComponent(id)}${suffix}`; }
    function listPath(page, sort, filters = parseRoute().filters || {}) {
        const p = new URLSearchParams({page: String(page), tri: sort});
        for (const [k,v] of Object.entries(filters)) if(v) p.set(k,v);
        return `#/documents?${p}`;
    }
    function tabs(doc, active) {
        return `<nav class="tabs" aria-label="Navigation du document">
            ${state.owner ? `<a href="${docPath(doc.id, "/controle")}" ${active === "controle" ? 'class="active" aria-current="page"' : ""}>Contrôle des paiements</a>` : ""}
            ${state.owner ? `<a href="${docPath(doc.id, "/envois")}" ${active === "envois" ? 'class="active" aria-current="page"' : ""}>Envois</a>` : ""}
            ${(state.owner && doc.statut === "EN_ATTENTE_SIGNATURE") || String(doc.numero || "").startsWith("DEMO-QT-") ? `<a href="${docPath(doc.id, "/signature")}" ${active === "signature" ? 'class="active" aria-current="page"' : ""}>Signature et PDF</a>` : ""}
            <a href="${docPath(doc.id, "/versements")}" ${active === "versements" ? 'class="active" aria-current="page"' : ""}>Versements</a>
            <a href="${docPath(doc.id)}" ${active === "detail" ? 'class="active" aria-current="page"' : ""}>Détails</a>
            <a href="${docPath(doc.id, "/historique")}" ${active === "historique" ? 'class="active" aria-current="page"' : ""}>Historique</a>
            ${previewAllowed(doc) ? `<a href="${docPath(doc.id, "/preview")}" ${active === "preview" ? 'class="active" aria-current="page"' : ""}>Prévisualisation</a>` : ""}
            ${state.owner ? `<a href="${docPath(doc.id, "/archives")}" ${active === "archives" ? 'class="active" aria-current="page"' : ""}>Archives</a>` : ""}
        </nav>`;
    }
    function docHeader(doc, title, buttons = "") {
        return `<a class="back-link" href="${esc(state.listHash)}">← Retour aux documents</a>
            <div class="heading"><div><p class="eyebrow">${esc(label(doc.typeDocument))}</p><h1>${esc(title)}</h1><p>Bail ${esc(doc.bailId)} · ${esc(date(doc.periodeDebut))} — ${esc(date(doc.periodeFin))}</p></div><div class="actions">${buttons}</div></div>`;
    }
    function fact(name, value, wide = false) {
        return `<div${wide ? ' class="wide"' : ""}><dt>${esc(name)}</dt><dd>${esc(value || "Non renseigné")}</dd></div>`;
    }
    function renderLogin() {
        view(`<div class="login-layout"><div class="login-intro"><p class="eyebrow">DARLOC / QUITTANCES</p><h1>Votre espace locatif.</h1><p>Retrouvez vos documents et suivez leur traitement.</p></div>
            <section class="card card-pad" aria-labelledby="login-title"><h2 id="login-title">Connexion locale</h2><p class="muted">Saisissez le jeton fourni par votre outil local.</p>
            <form id="login-form"><label for="jeton">Jeton d’accès JWT</label><input id="jeton" type="password" autocomplete="off" spellcheck="false" maxlength="16384" required><button class="button primary" type="submit">Accéder à mon espace</button></form><p class="muted" style="font-size:12px;margin:16px 0 0">Un rechargement de la page nécessite une reconnexion.</p></section></div>`, "Connexion");
        $("login-form").addEventListener("submit", async event => {
            event.preventDefault();
            if (state.pending) return;
            const value = $("jeton").value.trim();
            if (!value) return;
            const generation = state.sequence;
            state.token = value;
            $("jeton").value = "";
            busy(true, "Connexion en cours");
            notice();
            try {
                await api("/api/v1/identite");
                state.owner = ownerFromValidatedToken(value);
                setAccount();
                busy(false);
                if (generation === state.sequence) route();
            } catch (error) {
                busy(false); clearSession(); renderLogin(); notice(error.message, true);
            }
        });
    }
    async function renderList(r, signal, current) {
        const params = new URLSearchParams({page: String(r.page), taille: "10", tri: r.sort, sens: "desc"});
        for (const [k,v] of Object.entries(r.filters)) if(v) params.set(k,v);
        const data = await api(`/api/v1/quittances?${params}`, {signal});
        if (!current()) return;
        if (!Array.isArray(data.contenu) || !Number.isInteger(data.totalPages)) throw new ApiError("Format de liste incorrect.");
        state.listHash = listPath(r.page, r.sort);
        $("nav-documents").href = state.listHash;
        state.creationUnknown = false;
        const rows = data.contenu.map(doc => `<tr><td>${esc(doc.numero || "Non attribuée")}<small>${esc(doc.bailId)}</small></td>
            <td>${esc([doc.prenomLocataire, doc.nomLocataire].filter(Boolean).join(" ") || doc.locataireId)}<small>${esc(doc.adresseBien || "")}</small></td>
            <td>${esc(date(doc.periodeDebut))}<small>au ${esc(date(doc.periodeFin))}</small></td><td>${esc(label(doc.typeDocument))}</td>
            <td class="money">${esc(money(doc.totalPaye))}</td><td>${badge(doc.statut)}</td><td><a class="button secondary small" href="${docPath(doc.id)}" aria-label="Consulter le document du bail ${esc(doc.bailId)}">Consulter</a></td></tr>`).join("");
        view(`<div class="heading"><div><p class="eyebrow">ESPACE DOCUMENTAIRE</p><h1>Quittances et reçus</h1><p>${esc(data.totalElements)} document(s) accessible(s)</p></div><div class="actions"><button id="export-csv" class="button secondary">Exporter en CSV</button><button id="refresh" class="button secondary">Actualiser</button>${state.owner ? '<a class="button primary" href="#/nouveau">＋ Nouveau document</a>' : ""}</div></div>
            <section class="card card-pad" style="margin-bottom:20px"><form id="filters"><div class="form-grid"><div><label for="filter-bailId">Identifiant du bail</label><input id="filter-bailId" maxlength="100" value="${esc(r.filters.bailId)}"></div><div><label for="filter-numero">Numéro exact</label><input id="filter-numero" maxlength="100" value="${esc(r.filters.numero)}"></div><div><label for="filter-statut">Statut</label><select id="filter-statut"><option value="">Tous</option>${["BROUILLON","EN_ATTENTE_VALIDATION","EN_ATTENTE_SIGNATURE","EMIS","A_REEXAMINER","ANNULE","REMPLACE"].map(v=>`<option value="${v}" ${r.filters.statut===v ? "selected" : ""}>${esc(label(v))}</option>`).join("")}</select></div><div><label for="filter-debut">Période à partir du</label><input id="filter-debut" type="date" value="${esc(r.filters.debut)}"></div><div><label for="filter-fin">Période jusqu’au</label><input id="filter-fin" type="date" value="${esc(r.filters.fin)}"></div></div><div class="form-footer"><button type="button" id="reset-filters" class="button secondary">Effacer</button><button class="button primary">Rechercher</button></div></form></section>
            <section class="card"><div class="list-toolbar"><strong>Mes documents</strong><div class="sort-control"><label for="sort">Trier par</label><select id="sort"><option value="creeLe">Création récente</option><option value="periodeDebut">Période récente</option><option value="totalPaye">Montant décroissant</option></select></div></div>
            <div class="table-scroll"><table><caption class="visually-hidden">Documents accessibles à votre compte</caption><thead><tr><th>Référence</th><th>Locataire et bien</th><th>Période</th><th>Type</th><th class="money">Total versé</th><th>État</th><th>Action</th></tr></thead><tbody>${rows}</tbody></table></div>
            ${rows ? "" : '<div class="empty"><h2>Aucun document à afficher</h2><p>Les documents accessibles à votre compte apparaîtront ici.</p></div>'}
            <nav class="pagination-bar" aria-label="Pages de documents"><button id="previous" class="button secondary small" ${r.page <= 0 ? "disabled" : ""}>Précédent</button><span>${data.totalPages ? `Page ${r.page + 1} sur ${data.totalPages}` : "Aucune page"}</span><button id="next" class="button secondary small" ${r.page + 1 >= data.totalPages ? "disabled" : ""}>Suivant</button></nav></section>`, "Documents");
        $("filters").addEventListener("submit", event => {
            event.preventDefault();
            const filters = Object.fromEntries(["bailId","numero","statut","debut","fin"].map(k=>[k,$("filter-"+k).value.trim()]));
            if(filters.debut && filters.fin && filters.debut > filters.fin) { notice("Intervalle de dates invalide.",true); return; }
            go(listPath(0,r.sort,filters));
        });
        $("reset-filters").addEventListener("click",()=>go(listPath(0,r.sort,{})));
        $("sort").value = r.sort;
        $("sort").addEventListener("change", e => go(listPath(0, e.target.value)));
        $("refresh").addEventListener("click", () => route());
        $("export-csv").addEventListener("click", async () => {
            const bouton = $("export-csv"); bouton.disabled = true; bouton.textContent = "Export en cours…";
            const filtres = new URLSearchParams({tri: r.sort, sens: "desc"});
            for (const [k,v] of Object.entries(r.filters)) if(v) filtres.set(k,v);
            try {
                const blob = await api(`/api/v1/quittances/export.csv?${filtres}`, {signal, csv: true});
                if (!current()) return;
                telecharger(blob, "quittances.csv"); notice("Export de toutes les pages correspondant aux filtres appliqués.");
            } catch (error) { if (current()) { if(error.status === 401) { clearSession(); renderLogin(); } notice(error.message, true); } }
            finally { if (bouton.isConnected) { bouton.disabled = false; bouton.textContent = "Exporter en CSV"; } }
        });
        $("previous").addEventListener("click", () => go(listPath(Math.max(0, r.page - 1), r.sort)));
        $("next").addEventListener("click", () => go(listPath(r.page + 1, r.sort)));
    }
    function renderCreate() {
        if (!state.owner) throw new ApiError("La préparation est réservée au bailleur habilité.", 403);
        const d = state.draft;
        view(`<a class="back-link" href="${esc(state.listHash)}">← Retour aux documents</a><div class="heading"><div><p class="eyebrow">PRÉPARATION</p><h1>Nouveau document</h1><p>Choisissez un bail existant et sa période.</p></div></div>
            <div class="split-form"><section class="card card-pad"><form id="create-form"><div class="form-grid"><div class="wide"><label for="bail">Identifiant du bail</label><input id="bail" maxlength="100" required value="${esc(d.bailId)}" aria-describedby="bail-help"><small id="bail-help">Identifiant fourni par le service chargé des baux.</small></div>
            <div><label for="start">Début de période</label><input id="start" type="date" required value="${esc(d.periodeDebut)}"></div><div><label for="end">Fin de période</label><input id="end" type="date" required value="${esc(d.periodeFin)}"></div></div>
            <div class="form-footer"><a class="button secondary" href="${esc(state.listHash)}">Retour à la liste</a><button id="create-submit" class="button primary" type="submit" ${state.creationUnknown ? "disabled" : ""}>Préparer le brouillon</button></div></form></section>
            <aside class="note-card"><h2>Avant de commencer</h2><p>Le bail doit déjà exister. Aucun paiement n’est enregistré depuis cet écran.</p><p>Les montants seront récupérés et vérifiés par le serveur.</p><p>Le document sera un <strong>brouillon non signé</strong>.</p></aside></div>`, "Nouveau document");
        const saveDraft = () => { state.draft = {bailId: $("bail").value, periodeDebut: $("start").value, periodeFin: $("end").value}; };
        $("create-form").addEventListener("input", saveDraft);
        if (state.creationUnknown) notice("Le résultat de la dernière création est incertain. Consultez la liste avant de recommencer.", true);
        $("create-form").addEventListener("submit", async event => {
            event.preventDefault();
            if (state.pending || state.creationUnknown) return;
            saveDraft();
            const body = {...state.draft, bailId: state.draft.bailId.trim()};
            if (!body.bailId || !body.periodeDebut || !body.periodeFin || body.periodeDebut > body.periodeFin) {
                notice("Vérifiez le bail et les dates : la fin doit être postérieure ou égale au début.", true); return;
            }
            busy(true, "Préparation du brouillon"); notice();
            try {
                const result = await api("/api/v1/quittances", {method: "POST", body});
                if (!result?.id || !/^[a-fA-F0-9-]{36}$/.test(result.id)) throw new ApiError("Création acceptée, mais identifiant absent ou invalide. Consultez la liste avant de recommencer.", 201, true);
                state.draft = {bailId: "", periodeDebut: "", periodeFin: ""};
                busy(false);
                history.pushState(null, "", docPath(result.id));
                const shown = await route();
                notice(shown ? "Brouillon créé. Vérifiez les montants et la prévisualisation." : "Brouillon créé, mais son affichage a échoué. Consultez la liste avant toute nouvelle création.", !shown);
            } catch (error) {
                busy(false);
                if (error.status === 401) { clearSession(); renderLogin(); }
                else if (!error.status || error.accepted || error.status >= 500) { state.creationUnknown = true; $("create-submit").disabled = true; }
                notice(error.message, true);
            }
        });
    }
    async function getDocument(id, signal) {
        const doc = await api(`/api/v1/quittances/${encodeURIComponent(id)}`, {signal});
        if (!doc || doc.id !== id) throw new ApiError("La réponse ne correspond pas au document demandé.");
        return doc;
    }
    async function renderDetail(r, signal, current) {
        const doc = await getDocument(r.id, signal);
        if (!current()) return;
        const buttons = `${state.owner && doc.statut === "EN_ATTENTE_SIGNATURE" ? `<a class="button primary" href="${docPath(doc.id, "/signature")}">Signer et émettre</a>` : ""}${state.owner && doc.statut === "ANNULE" ? `<a class="button primary" href="${docPath(doc.id, "/remplacer")}">Préparer un remplacement</a>` : ""}${validationAllowed(doc) ? '<button id="validate" class="button primary">Valider</button>' : ""}${cancellationAllowed(doc) ? '<button id="cancel-document" class="button danger">Annuler le document</button>' : ""}`;
        view(`${docHeader(doc, doc.numero || "Document sans numéro", buttons)}${tabs(doc, "detail")}${doc.documentPrecedentId && /^[a-fA-F0-9-]{36}$/.test(doc.documentPrecedentId) ? `<section class="note-card replacement-origin"><h2>Document de remplacement</h2><p>${esc(doc.motifRemplacement)}</p><a href="${docPath(doc.documentPrecedentId)}">Consulter le document d’origine</a></section>` : ""}
            <div class="detail-grid"><section class="card"><h2 class="card-title">Parties et bien</h2><dl class="facts">${fact("Bail", doc.bailId)}${fact("Bailleur", doc.nomBailleur)}${fact("Locataire", [doc.prenomLocataire, doc.nomLocataire].filter(Boolean).join(" "))}${fact("Type de document", label(doc.typeDocument))}${fact("Adresse du bien", doc.adresseBien, true)}</dl></section>
            <section class="card"><h2 class="card-title">Période et montants</h2><div class="amounts"><div class="amount-line"><span>Période</span><strong>${esc(date(doc.periodeDebut))} — ${esc(date(doc.periodeFin))}</strong></div>${[["Loyer dû",doc.loyerDu],["Charges dues",doc.chargesDues],["Loyer payé",doc.loyerPaye],["Charges payées",doc.chargesPayees]].map(([name,value]) => `<div class="amount-line"><span>${name}</span><strong>${esc(money(value))}</strong></div>`).join("")}<div class="amount-total"><span>Total versé</span><strong>${esc(money(doc.totalPaye))}</strong></div></div></section></div>
            <section class="card detail-meta"><h2 class="card-title">État du document</h2><dl class="facts"><div><dt>Document</dt><dd>${badge(doc.statut)}</dd></div><div><dt>Signature</dt><dd>${badge(doc.statutSignature)}${String(doc.numero || "").startsWith("DEMO-QT-") ? "<small>Certificat local de démonstration</small>" : ""}</dd></div>${fact("Émission", doc.emisLe ? time(doc.emisLe) : "Non émis")}${fact("Création · heure du Maroc", time(doc.creeLe))}${fact("Identifiant", doc.id, true)}</dl></section>`, "Détails du document");
        $("validate")?.addEventListener("click", () => openConfirmation("valider", doc));
        $("cancel-document")?.addEventListener("click", () => openConfirmation("annuler", doc));
    }
    async function renderReplacement(r, signal, current) {
        if (!state.owner) throw new ApiError("Remplacement réservé au bailleur.", 403);
        const doc = await getDocument(r.id, signal);
        if (!current()) return;
        if (doc.statut !== "ANNULE") throw new ApiError("Annulez d’abord le document avec un motif.", 409);
        let blocked = false;
        view(`${docHeader(doc, "Remplacer le document")}<section class="card card-pad"><p>Les données du bail et les paiements seront relus. Le document initial reste annulé ; le nouveau sera un brouillon non signé.</p><form id="replacement-form"><label for="replacement-reason">Motif du remplacement</label><textarea id="replacement-reason" rows="5" maxlength="2000" required></textarea><label><input id="replacement-confirm" type="checkbox" required> Je confirme la préparation du remplacement.</label><div class="form-footer"><a class="button secondary" href="${docPath(doc.id)}">Retour au document</a><button class="button primary" id="replacement-submit">Préparer le remplacement</button></div></form></section>`, "Remplacement");
        $("replacement-form").addEventListener("submit", async event => {
            event.preventDefault();
            if (state.pending || blocked) return;
            const motif = $("replacement-reason").value.trim();
            if (!motif || !$("replacement-confirm").checked) { notice("Saisissez un motif et confirmez.", true); return; }
            busy(true, "Préparation du remplacement…"); notice();
            try {
                const result = await api(`/api/v1/quittances/${r.id}/remplacer`, {method: "POST", body: {motif, confirmation: true}});
                if (!result || !/^[a-fA-F0-9-]{36}$/.test(result.id) || result.id === r.id || result.documentPrecedentId !== r.id || result.statut !== "BROUILLON" || result.statutSignature !== "NON_SIGNE") throw new ApiError("Réponse incohérente. Consultez la liste avant de réessayer.", 201, true);
                busy(false); await goAfterMutation(result.id, "Remplacement préparé : brouillon non signé.");
            } catch(error) {
                busy(false);
                if (error.status === 401) { clearSession(); renderLogin(); }
                else if (!error.status || error.status >= 500 || error.status === 409 || error.accepted) { blocked = true; $("replacement-submit").disabled = true; }
                notice(error.message, true);
            }
        });
    }
    async function renderPayments(r, signal, current) {
        const [doc, data] = await Promise.all([
            getDocument(r.id, signal),
            api(`/api/v1/quittances/${encodeURIComponent(r.id)}/affectations`, {signal})
        ]);
        if (!current()) return;
        if (!Array.isArray(data) || data.some(a => !a || a.devise !== "MAD" || !a.affectationId || !a.paiementId)) throw new ApiError("La liste des affectations reçue est invalide.");
        view(`${docHeader(doc, "Versements associés")}${tabs(doc, "versements")}
            <p class="archive-note">Affectations conservées lors de la préparation du document. Leur statut affiché correspond à cette date.</p>
            <section class="card"><div class="list-toolbar"><strong>${data.length} affectation(s)</strong></div>
            ${data.length ? `<div class="table-scroll"><table><caption class="visually-hidden">Versements affectés au document</caption><thead><tr><th>Paiement</th><th>Affectation</th><th>Période</th><th>Nature</th><th class="money">Montant</th><th>Statut à la préparation</th></tr></thead><tbody>${data.map(a=>`<tr><td>${esc(a.paiementId)}</td><td>${esc(a.affectationId)}</td><td>${esc(date(a.periodeDebut))}<small>au ${esc(date(a.periodeFin))}</small></td><td>${esc(a.nature === "LOYER" ? "Loyer" : a.nature === "CHARGES" ? "Charges" : a.nature)}</td><td class="money">${esc(money(a.montant))}</td><td>${esc(label(a.statutSourceALaPreparation))}</td></tr>`).join("")}</tbody></table></div>` : '<div class="empty"><h2>Aucune affectation enregistrée</h2><p>Aucun détail de versement n’est disponible pour ce document.</p></div>'}</section>`, "Versements associés");
    }
    async function renderHistory(r, signal, current) {
        const [doc, data] = await Promise.all([
            getDocument(r.id, signal),
            api(`/api/v1/quittances/${encodeURIComponent(r.id)}/historique?page=${r.page}&taille=10`, {signal})
        ]);
        if (!current()) return;
        if (!Array.isArray(data.contenu) || !Number.isInteger(data.totalPages)) throw new ApiError("Format d’historique incorrect.");
        view(`${docHeader(doc, "Historique du document")}${tabs(doc, "historique")}
            <section class="card"><div class="list-toolbar"><strong>${esc(data.totalElements)} événement(s)</strong><small>Heure du Maroc</small></div>
            ${data.contenu.length ? `<ol class="timeline">${data.contenu.map(ev => `<li><div class="event-head"><strong>${esc(label(ev.operation))}</strong><time>${esc(time(ev.horodatage))}</time></div><div class="event-meta">${badge(ev.resultat)}${ev.statutAvant || ev.statutApres ? `<span>${esc(ev.statutAvant ? label(ev.statutAvant) : "—")} → ${esc(ev.statutApres ? label(ev.statutApres) : "—")}</span>` : ""}</div>${ev.motif ? `<p class="event-reason">${esc(ev.motif)}</p>` : ""}</li>`).join("")}</ol>` : '<div class="empty"><h2>Aucun événement enregistré</h2><p>Les opérations tracées apparaîtront ici.</p></div>'}
            <nav class="pagination-bar" aria-label="Pages de l’historique"><button id="history-previous" class="button secondary small" ${r.page <= 0 ? "disabled" : ""}>Précédent</button><span>${data.totalPages ? `Page ${r.page + 1} sur ${data.totalPages}` : "Aucune page"}</span><button id="history-next" class="button secondary small" ${r.page + 1 >= data.totalPages ? "disabled" : ""}>Suivant</button></nav></section>`, "Historique");
        $("history-previous").addEventListener("click", () => go(docPath(r.id, `/historique?page=${Math.max(0,r.page-1)}`)));
        $("history-next").addEventListener("click", () => go(docPath(r.id, `/historique?page=${r.page+1}`)));
    }
    async function renderPreview(r, signal, current) {
        const doc = await getDocument(r.id, signal);
        if (!current()) return;
        if (!previewAllowed(doc)) throw new ApiError("La prévisualisation de ce brouillon n’est pas accessible dans son état actuel.", 403);
        const blob = await api(`/api/v1/quittances/${encodeURIComponent(r.id)}/preview`, {signal, pdf: true});
        if (!current()) return;
        state.pdfUrl = URL.createObjectURL(blob);
        view(`${docHeader(doc, "Prévisualisation")}${tabs(doc, "preview")}
            <div class="pdf-toolbar"><div><strong>Brouillon non signé</strong><br><small>Ce PDF n’est pas un document émis.</small></div><button id="download-pdf" class="button primary">Télécharger le PDF</button></div>
            <iframe id="pdf-frame" class="pdf-frame" title="Prévisualisation du brouillon PDF"></iframe>
            <p class="pdf-help">Si votre navigateur n’affiche pas le PDF, utilisez le bouton de téléchargement.</p>`, "Prévisualisation");
        $("pdf-frame").src = state.pdfUrl;
        $("download-pdf").addEventListener("click", () => {
            const a = document.createElement("a"); a.href = state.pdfUrl; a.download = `brouillon-${doc.id}.pdf`;
            document.body.appendChild(a); a.click(); a.remove();
            notice("Le téléchargement a été demandé au navigateur.");
        });
    }
    async function renderArchives(r, signal, current) {
        if (!state.owner) throw new ApiError("Les archives de brouillons sont réservées au bailleur.", 403);
        const [doc, data] = await Promise.all([
            getDocument(r.id, signal),
            api(`/api/v1/quittances/${encodeURIComponent(r.id)}/archives-brouillon?page=${r.page}&taille=10`, {signal})
        ]);
        if (!current()) return;
        if (!Array.isArray(data.contenu) || !Number.isInteger(data.totalPages)
                || data.contenu.some(a => !a || !/^[a-fA-F0-9-]{36}$/.test(a.id)
                    || a.quittanceId !== r.id || a.nature !== "BROUILLON")) {
            throw new ApiError("La liste des archives reçue est invalide.");
        }
        const buttons = previewAllowed(doc)
            ? '<button id="archive-document" class="button primary">Archiver le brouillon</button>' : "";
        view(`${docHeader(doc, "Archives du document", buttons)}${tabs(doc, "archives")}
            <p class="archive-note">Copies PDF conservées à la date d’archivage. Ces brouillons sont non signés et non émis.</p>
            <section class="card"><div class="list-toolbar"><strong>${esc(data.totalElements)} archive(s)</strong><button id="refresh-archives" class="button secondary small">Actualiser</button></div>
            ${data.contenu.length ? `<ul class="archive-list">${data.contenu.map(a => `<li class="archive-item">
                <div class="archive-heading"><div><h2>Brouillon · version ${esc(a.versionDocument)}</h2><p>${esc(time(a.creeLe))} · ${esc(a.tailleOctets)} octets</p></div><button class="button secondary" data-download-archive="${esc(a.id)}">Télécharger</button></div>
                <details><summary>Empreinte SHA-256</summary><code class="archive-hash">${esc(a.sha256)}</code><small>L’empreinte contrôle l’intégrité du fichier ; elle ne constitue pas une signature.</small></details>
            </li>`).join("")}</ul>` : '<div class="empty"><h2>Aucun brouillon archivé</h2><p>Archivez une version pour pouvoir retrouver ce même fichier plus tard.</p></div>'}
            <nav class="pagination-bar" aria-label="Pages des archives"><button id="archives-previous" class="button secondary small" ${r.page <= 0 ? "disabled" : ""}>Précédent</button><span>${data.totalPages ? `Page ${r.page + 1} sur ${data.totalPages}` : "Aucune page"}</span><button id="archives-next" class="button secondary small" ${r.page + 1 >= data.totalPages ? "disabled" : ""}>Suivant</button></nav></section>`, "Archives");
        $("refresh-archives").addEventListener("click", () => route());
        $("archives-previous").addEventListener("click", () => go(docPath(r.id, `/archives?page=${Math.max(0, r.page-1)}`)));
        $("archives-next").addEventListener("click", () => go(docPath(r.id, `/archives?page=${r.page+1}`)));
        $("archive-document")?.addEventListener("click", async () => {
            if (state.pending) return;
            busy(true, "Archivage du brouillon…"); notice();
            let message = "Brouillon archivé. Le document reste non signé et non émis.";
            let failed = false;
            try {
                await api(`/api/v1/quittances/${encodeURIComponent(r.id)}/archives-brouillon`, {method: "POST", noBody: true});
            } catch (error) {
                if (error.status === 401) {
                    busy(false); clearSession(); renderLogin(); notice(error.message, true); return;
                }
                failed = true;
                message = error.status === 409
                    ? "Cette version est déjà archivée ou le document a changé. Consultez la liste actualisée."
                    : `${error.message} Vérifiez les archives avant de recommencer.`;
            } finally { busy(false); }
            history.replaceState(null, "", docPath(r.id, "/archives"));
            const loaded = await route();
            notice(message + (loaded ? "" : " La liste n’a pas pu être rechargée."), failed || !loaded);
        });
        document.querySelectorAll("[data-download-archive]").forEach(button => {
            button.addEventListener("click", async () => {
                if (state.pending) return;
                const archiveId = button.dataset.downloadArchive;
                busy(true, "Vérification et téléchargement…"); notice();
                try {
                    const blob = await api(`/api/v1/quittances/${encodeURIComponent(r.id)}/archives-brouillon/${encodeURIComponent(archiveId)}/pdf`, {pdf: true});
                    releasePdf(); state.pdfUrl = URL.createObjectURL(blob);
                    const link = document.createElement("a");
                    link.href = state.pdfUrl; link.download = `brouillon-${r.id}-${archiveId}.pdf`;
                    busy(false);
                    document.body.appendChild(link); link.click(); link.remove();
                    notice("Le téléchargement a été demandé au navigateur après contrôle d’intégrité côté serveur.");
                } catch (error) {
                    if (error.status === 401) { clearSession(); renderLogin(); }
                    notice(error.message, true);
                } finally { busy(false); }
            });
        });
    }
    async function renderSignature(r, signal, current) {
        const doc = await getDocument(r.id, signal);
        if (!current()) return;
        const emitted = String(doc.numero || "").startsWith("DEMO-QT-") && !!doc.emisLe;
        const allowed = state.owner && doc.statut === "EN_ATTENTE_SIGNATURE" && doc.statutSignature === "EN_ATTENTE" && !doc.emisLe;
        if (!emitted && !allowed) throw new ApiError("Ce document n’est pas en attente de signature.", 409);
        view(`${docHeader(doc, "Signature et PDF")}${tabs(doc, "signature")}
            <section class="card card-pad"><h2>${emitted ? "Document de démonstration émis" : "Émettre le document de démonstration"}</h2>
            <p>Certificat local de test. La signature est conservée séparément du PDF et ne certifie pas l’identité du bailleur.</p>
            ${emitted ? `<p>Référence : <strong>${esc(doc.numero)}</strong> · ${esc(label(doc.statut))}</p>
                ${["ANNULE","REMPLACE","A_REEXAMINER"].includes(doc.statut) ? '<p class="notice error">Ce document est annulé, remplacé ou à réexaminer. Son fichier historique reste inchangé.</p>' : ""}
                <div class="actions"><button id="final-pdf" class="button primary">Télécharger le PDF</button><button id="proof-zip" class="button secondary">Télécharger la preuve</button><button id="verify-signature" class="button secondary">Vérifier l’intégrité</button></div>
                <p id="signature-result" role="status"></p>` : `<form id="emission-form"><p>La période et les montants resteront figés. Les paiements seront contrôlés avant l’émission.</p>
                <label><input type="checkbox" id="emission-confirm" required> Je confirme l’émission avec le certificat de démonstration.</label>
                <div class="form-footer"><a class="button secondary" href="${docPath(doc.id, "/preview")}">Revoir le document</a><button class="button primary" id="emit-document">Signer et émettre — démonstration</button></div></form>`}
            </section>`, "Signature et PDF");
        $("emission-form")?.addEventListener("submit", async event => {
            event.preventDefault();
            if (state.pending || !$("emission-confirm").checked) return;
            busy(true, "Vérification, signature et émission…"); notice();
            try {
                const result = await api(`/api/v1/quittances/${r.id}/emission-demo`, {method: "POST", body: {confirmation: true}});
                if (result.id !== r.id || result.statut !== "EMIS" || result.modeSignature !== "DEMONSTRATION")
                    throw new ApiError("Réponse inattendue. Consultez le document avant de réessayer.", 200, true);
                busy(false); await route(); notice("Document émis avec le certificat de démonstration.");
            } catch (error) {
                busy(false);
                if (error.status === 401) { clearSession(); renderLogin(); }
                else { await route(); }
                notice(error.message, true);
            }
        });
        for (const [buttonId, suffix, extension] of [["final-pdf", "pdf", "pdf"], ["proof-zip", "preuve", "zip"]]) {
            $(buttonId)?.addEventListener("click", async () => {
                if (state.pending) return;
                busy(true, "Contrôle du document…"); notice();
                try {
                    const blob = await api(`/api/v1/quittances/${r.id}/emission-demo/${suffix}`, {pdf: extension === "pdf", zip: extension === "zip"});
                    releasePdf(); state.pdfUrl = URL.createObjectURL(blob);
                    const link = document.createElement("a"); link.href = state.pdfUrl; link.download = `${doc.numero}.${extension}`;
                    busy(false); document.body.appendChild(link); link.click(); link.remove();
                    notice("Téléchargement demandé après vérification côté serveur.");
                } catch (error) {
                    busy(false);
                    if (error.status === 401) { clearSession(); renderLogin(); }
                    notice(error.message, true);
                } finally { busy(false); }
            });
        }
        $("verify-signature")?.addEventListener("click", async () => {
            if (state.pending) return;
            busy(true, "Vérification de la signature…"); notice();
            try {
                const result = await api(`/api/v1/quittances/${r.id}/emission-demo/verification`);
                if (!result.integriteValide || !result.signatureValide || result.modeSignature !== "DEMONSTRATION")
                    throw new ApiError("La preuve n’a pas été validée.");
                $("signature-result").textContent = `Intégrité et signature de test vérifiées. SHA-256 : ${result.sha256}`;
                $("signature-result").style.overflowWrap = "anywhere";
            } catch (error) {
                if (error.status === 401) { clearSession(); renderLogin(); }
                notice(error.message, true);
            } finally { busy(false); }
        });
    }
    async function renderEnvois(r, signal, current) {
        if (!state.owner) throw new ApiError("Envois réservés au bailleur.",403);
        const [doc,data]=await Promise.all([getDocument(r.id,signal),api(`/api/v1/quittances/${r.id}/envois-mailpit?page=${r.page}&taille=10`,{signal})]);
        if (!current()) return;
        if (!Array.isArray(data.contenu) || !Number.isInteger(data.totalPages)) throw new ApiError("Historique d’envoi invalide.");
        const names={EN_ATTENTE_ENVOI:"Traitement en cours",ENVOYE:"Accepté par Mailpit",ECHEC_ENVOI:"Échec — aucun message transmis",INDETERMINE:"Résultat incertain — vérifier Mailpit"};
        const canSend=doc.statut === "EMIS" && String(doc.numero || "").startsWith("DEMO-QT-") &&
            (data.totalElements === 0 || (r.page === 0 && data.contenu.length>0 && data.contenu.every(e=>e.statut === "ECHEC_ENVOI")));
        view(`${docHeader(doc,"Envois du document")}${tabs(doc,"envois")}
            <section class="card card-pad" style="margin-bottom:24px"><h2>Envoi de test avec Mailpit</h2>
            <p>Le PDF est envoyé vers une boîte fictive locale. Aucun email n’est adressé au véritable locataire.</p>
            ${canSend ? '<form id="send-mailpit"><label><input id="send-confirm" type="checkbox" required> Je confirme l’envoi du PDF dans Mailpit.</label><div class="form-footer"><button class="button primary">Envoyer dans Mailpit</button></div></form>' : '<p>L’envoi nécessite un document émis actif et aucune tentative en cours, acceptée ou incertaine.</p>'}
            <button id="refresh-mails" class="button secondary">Actualiser les envois</button></section>
            <section class="card"><div class="list-toolbar"><strong>${esc(data.totalElements)} tentative(s)</strong></div>
            ${data.contenu.length ? `<div class="table-scroll"><table><thead><tr><th>Date</th><th>Destinataire de test</th><th>Résultat</th></tr></thead><tbody>${data.contenu.map(e=>`<tr><td>${esc(time(e.creeLe))}</td><td style="overflow-wrap:anywhere;max-width:300px">${esc(e.destinataire)}</td><td>${esc(names[e.statut] || e.statut)}${e.erreurCode ? `<small>${esc(e.erreurCode)}</small>` : ""}</td></tr>`).join("")}</tbody></table></div>` : '<div class="empty"><h2>Aucun envoi</h2><p>Les tentatives apparaîtront ici.</p></div>'}
            <nav class="pagination-bar" aria-label="Pages des envois"><button id="mail-prev" class="button secondary" ${r.page<=0?"disabled":""}>Précédent</button><span>${data.totalPages ? `Page ${r.page+1} sur ${data.totalPages}` : "Aucune page"}</span><button id="mail-next" class="button secondary" ${r.page+1>=data.totalPages?"disabled":""}>Suivant</button></nav></section>`,"Envois");
        $("refresh-mails").addEventListener("click",()=>route());
        $("mail-prev").addEventListener("click",()=>go(docPath(r.id,`/envois?page=${Math.max(0,r.page-1)}`)));
        $("mail-next").addEventListener("click",()=>go(docPath(r.id,`/envois?page=${r.page+1}`)));
        const requeteId=crypto.randomUUID();
        let submitted=false;
        $("send-mailpit")?.addEventListener("submit",async event=>{
            event.preventDefault();
            if(state.pending || submitted || !$("send-confirm").checked)return;
            submitted=true;busy(true,"Envoi vers Mailpit…");notice();
            try {
                const result=await api(`/api/v1/quittances/${r.id}/envois-mailpit`,{method:"POST",body:{requeteId,confirmation:true}});
                busy(false);await route();
                notice(result.statut === "ENVOYE" ? "Message accepté par Mailpit. Consultez la boîte locale sur le port 8025." : "Tentative enregistrée. Actualisez son état avant toute nouvelle action.");
            }catch(error){
                busy(false);
                if(error.status===401){clearSession();renderLogin();}else{await route();}
                notice(error.message,true);
            }finally{busy(false);}
        });
    }
    function telecharger(blob, nom) {
        const url = URL.createObjectURL(blob);
        const lien = document.createElement("a"); lien.href = url; lien.download = nom;
        document.body.append(lien); lien.click(); lien.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }
    async function renderTableau(r, signal, current) {
        const data = await api("/api/v1/quittances/tableau-de-bord", {signal});
        if (!current()) return;
        const etats = ["BROUILLON", "EN_ATTENTE_VALIDATION", "EN_ATTENTE_SIGNATURE", "EMIS", "A_REEXAMINER", "ANNULE", "REMPLACE"];
        const nombre = (statut, type) => data.groupes.filter(g => (!statut || g.statut === statut) && (!type || g.typeDocument === type)).reduce((n,g) => n + g.nombre, 0);
        view(`<div class="heading"><div><p class="eyebrow">VUE D’ENSEMBLE</p><h1>Tableau de bord</h1><p>Vos documents, tous baux et toutes périodes.</p></div><button id="refresh-dashboard" class="button secondary">Actualiser</button></div>
            <section class="dashboard-summary" aria-label="Nombre de documents">
                <div class="card card-pad dashboard-total"><span>Documents accessibles</span><strong>${esc(data.totalDocuments)}</strong></div>
                <div class="card card-pad"><span>Quittances intégrales</span><strong class="dashboard-number">${esc(nombre(null,"QUITTANCE_INTEGRALE"))}</strong></div>
                <div class="card card-pad"><span>Reçus partiels</span><strong class="dashboard-number">${esc(nombre(null,"RECU_PARTIEL"))}</strong></div>
            </section>
            <section class="card card-pad"><h2>Documents par état</h2><p>Ouvrez un état pour retrouver les documents correspondants.</p>
            <div class="dashboard-states">${etats.map(statut => `<a class="dashboard-state" href="${esc(listPath(0,"creeLe",{statut}))}">${badge(statut)}<strong>${esc(nombre(statut))}</strong><span>Consulter →</span></a>`).join("")}</div>
            <p class="dashboard-note">Chaque document compte une fois dans son état actuel. Les documents annulés et remplacés restent visibles. Ces compteurs ne mesurent pas les loyers encaissés.</p>
            <small>Actualisé le ${esc(time(data.consulteLe))}${state.owner ? "" : " · Uniquement les documents émis accessibles à votre compte."}</small></section>`, "Tableau de bord");
        $("refresh-dashboard").addEventListener("click", () => route());
    }
    async function renderControle(r, signal, current) {
        if (!state.owner) throw new ApiError("Le contrôle des paiements est réservé au bailleur.", 403);
        const doc = await getDocument(r.id, signal);
        if (!current()) return;
        view(`${docHeader(doc,"Contrôle des paiements")}${tabs(doc,"controle")}
            <section class="card card-pad"><h2>Comparer avec le service Paiements</h2>
            <p>Vérifiez si les versements ont changé depuis la préparation du document.</p>
            <p>Ce contrôle consulte les paiements actuels. Il ne modifie ni le document, ni son statut, ni son PDF.</p>
            <button id="control-payments" class="button primary">Lancer le contrôle</button>
            <div id="control-result" class="control-result" role="status" aria-live="polite"></div></section>`, "Contrôle des paiements");
        $("control-payments").addEventListener("click", async () => {
            const bouton = $("control-payments"), zone = $("control-result");
            bouton.disabled = true; bouton.textContent = "Comparaison en cours…"; zone.innerHTML = ""; notice();
            try {
                const rapport = await api(`/api/v1/quittances/${r.id}/controle-paiements`, {signal});
                if (!current()) return;
                const conforme = rapport.resultat === "CONFORME";
                const valeur = a => a ? `${a.paiementId} · ${a.nature} · ${money(a.montant)} · ${a.statutSourceALaPreparation}` : "Absente";
                zone.innerHTML = `<div class="notice ${conforme ? "" : "error"}"><strong>${conforme ? "Aucun écart détecté" : "Écarts détectés : document à contrôler"}</strong><p>Comparaison du ${esc(time(rapport.verifieLe))}.</p></div>
                ${conforme ? "<p>Les affectations conservées correspondent à la réponse actuelle du service Paiements.</p>" : `<div class="table-scroll"><table><caption>Différences entre le document et les paiements actuels</caption><thead><tr><th>Affectation</th><th>Écart</th><th>Dans le document</th><th>Actuellement</th></tr></thead><tbody>${rapport.ecarts.map(e => `<tr><td>${esc(e.affectationId)}</td><td>${esc(e.detail)}</td><td>${esc(valeur(e.avant))}</td><td>${esc(valeur(e.maintenant))}</td></tr>`).join("")}</tbody></table></div><p>Vérifiez l’écart avec le responsable des paiements. Si le document est incorrect, utilisez l’annulation motivée puis préparez son remplacement depuis les détails.</p>`}
                <small>Résultat ponctuel, non enregistré. Relancez le contrôle après toute modification des paiements. Le bail et les coordonnées ne sont pas comparés ici.</small>`;
            } catch(error) {
                if (current()) { if(error.status === 401) { clearSession(); renderLogin(); } else zone.textContent = "Contrôle non abouti. Aucun résultat de conformité disponible."; notice(error.message, true); }
            } finally { if(bouton.isConnected) { bouton.disabled = false; bouton.textContent = "Relancer le contrôle"; } }
        });
    }
    function renderGuide() {
        const actions = [
            ["Nouveau document", "Choisir un bail et une période. Le serveur récupère le locataire, le bien, les sommes dues et les paiements confirmés, puis prépare un brouillon."],
            ["Consulter et filtrer", "Retrouver un document par bail, numéro exact, état ou période. Trier les résultats et parcourir les pages."],
            ["Versements", "Voir les affectations retenues lors de la préparation. Une affectation est la part d’un paiement attribuée à un bail, une période et un poste (loyer ou charges)."],
            ["Prévisualisation", "Lire ou télécharger un PDF de brouillon avant validation. Ce document n’est pas encore émis."],
            ["Valider", "Confirmer le brouillon après vérification. Il passe en attente de signature. Cela ne l’envoie pas au locataire."],
            ["Signature et PDF", "Émettre le document avec le certificat local de test. L’application conserve le PDF, lui attribue un numéro et crée une signature séparée."],
            ["Télécharger la preuve", "Récupérer les éléments de la signature de test. Le certificat local ne prouve pas une identité certifiée par un tiers."],
            ["Vérifier l’intégrité", "Contrôler les octets du PDF et sa signature. Cela ne vérifie pas si les paiements ont changé depuis."],
            ["Envois", "Envoyer le PDF émis dans la boîte locale Mailpit et consulter les tentatives. Aucun vrai email n’est envoyé au locataire avec ce mode."],
            ["Annuler", "Invalider un document avec un motif conservé dans l’historique. Le document n’est pas supprimé et aucun paiement n’est remboursé."],
            ["Remplacer", "Préparer un nouveau brouillon lié à un document annulé, à partir des données actuelles. Il doit ensuite être vérifié, validé et émis. L’ancien reste annulé."],
            ["Historique", "Consulter les opérations enregistrées et leurs dates. Un contrôle ponctuel des paiements n’ajoute pas d’événement à cet historique."],
            ["Archives", "Conserver et télécharger une version PDF d’un brouillon. Une archive de brouillon ne vaut pas émission."],
            ["Tableau de bord", "Compter les documents par état et par type. Il ne s’agit pas du chiffre d’affaires : un brouillon, un document annulé et son remplacement ne doivent pas être additionnés comme des encaissements."],
            ["Exporter en CSV", "Télécharger toutes les pages de la liste filtrée, dans la limite de 5 000 documents. Cliquer d’abord sur Rechercher pour appliquer les filtres."],
            ["Contrôle des paiements", "Comparer à la demande les affectations conservées et les données actuelles. Un écart est signalé sans changement automatique du document."],
            ["Se déconnecter", "Effacer le jeton de la mémoire du navigateur. Cette version utilise un JWT local ; après rechargement, il faut le recoller s’il est encore valide."]
        ];
        view(`<div class="heading"><div><p class="eyebrow">MODE D’EMPLOI</p><h1>Comprendre les actions</h1><p>Votre module transforme des paiements confirmés en documents consultables et traçables.</p></div></div>
            <section class="card card-pad guide-intro"><h2>Quittance ou reçu partiel ?</h2><p>Pour 3 000 MAD de loyer et 200 MAD de charges : 3 200 MAD payés donnent une quittance intégrale. 1 500 MAD payés donnent un reçu partiel ; il reste 1 700 MAD à régler.</p><p>Vous n’enregistrez pas les paiements ici. Le module Paiements fournit les versements ; votre module produit le document correspondant.</p><h2>Parcours habituel</h2><ol><li>Créer un brouillon avec le bail et la période.</li><li>Vérifier les détails, les versements et la prévisualisation.</li><li>Valider, puis émettre dans « Signature et PDF ».</li><li>Télécharger le PDF et, si nécessaire, l’envoyer dans Mailpit.</li><li>En cas de changement, contrôler les paiements puis décider d’une annulation et d’un remplacement.</li></ol></section>
            <section class="guide-actions">${actions.map(([titre,texte]) => `<details class="card card-pad"><summary>${esc(titre)}</summary><p>${esc(texte)}</p></details>`).join("")}</section>
            <section class="card card-pad"><h2>Qui peut agir ?</h2><p>Le bailleur prépare et gère ses documents. Le locataire consulte les documents déjà émis qui lui sont associés. Les contrôles côté serveur protègent les données même si une URL est modifiée manuellement.</p><h2>Ce qui reste simulé</h2><p>Les services des collègues, le certificat de signature et les emails sont actuellement locaux. Leur remplacement par les services réels nécessite une intégration et des vérifications supplémentaires.</p></section>`, "Comprendre les actions");
    }

    function busy(value, text = "Enregistrement en cours") {
        state.pending = value;
        $("busy-label").textContent = text;
        $("busy-overlay").hidden = !value || modal.open;
        $("confirmation-form").querySelectorAll("button,textarea").forEach(el => { el.disabled = value; });
        if (modal.open) {
            $("submit-confirmation").textContent = value ? "Enregistrement…" : (state.modalAction === "annuler" ? "Confirmer l’annulation" : "Valider le document");
            $("confirmation-form").setAttribute("aria-busy", String(value));
        }
    }
    function closeConfirmation() {
        if (state.pending) return;
        modal.close();
        state.modalAction = null; state.modalDocument = null;
        $("reason").value = "";
    }
    function openConfirmation(action, doc) {
        if (state.pending) return;
        state.modalAction = action; state.modalDocument = doc;
        const cancel = action === "annuler";
        $("confirmation-title").textContent = cancel ? "Annuler le document" : "Valider le document";
        $("confirmation-description").textContent = cancel
            ? "Le document et son historique seront conservés. Indiquez le motif de l’annulation."
            : "Confirmez avoir vérifié le bail, la période et les montants. Cette validation place le document en attente de signature ; elle ne le signe pas.";
        $("reason-group").hidden = !cancel; $("reason").required = cancel; $("reason").value = "";
        $("modal-notice").hidden = true;
        $("submit-confirmation").textContent = cancel ? "Confirmer l’annulation" : "Valider le document";
        $("submit-confirmation").className = cancel ? "button danger" : "button primary";
        modal.showModal();
        (cancel ? $("reason") : $("cancel-confirmation")).focus();
    }
    $("confirmation-form").addEventListener("submit", async event => {
        event.preventDefault();
        if (state.pending || !state.modalDocument || !state.owner) return;
        const id = state.modalDocument.id;
        const action = state.modalAction;
        const motif = $("reason").value.trim();
        if (action === "annuler" && (!motif || motif.length > 2000)) {
            $("modal-notice").textContent = "Saisissez un motif de 1 à 2 000 caractères.";
            $("modal-notice").hidden = false; $("reason").focus(); return;
        }
        busy(true); $("modal-notice").hidden = true;
        try {
            await api(`/api/v1/quittances/${encodeURIComponent(id)}/${action}`, {
                method: "POST", noBody: true,
                body: action === "annuler" ? {motif, confirmation: true} : {confirmation: true}
            });
            busy(false); closeConfirmation();
            await goAfterMutation(id, action === "annuler" ? "Annulation enregistrée." : "Validation enregistrée.");
        } catch (error) {
            busy(false);
            if (error.status === 401) { closeConfirmation(); clearSession(); renderLogin(); notice(error.message, true); }
            else if (!error.status || error.status >= 500 || error.accepted || error.status === 409) {
                closeConfirmation(); await route(); notice(`${error.message} Vérifiez l’état affiché avant une nouvelle action.`, true);
            } else {
                $("modal-notice").textContent = error.message; $("modal-notice").hidden = false;
            }
        }
    });
    async function goAfterMutation(id, text) {
        history.replaceState(null, "", docPath(id));
        const ok = await route();
        notice(ok ? text : `${text} Les détails n’ont pas pu être rechargés. Utilisez Réessayer ou revenez à la liste.`, !ok);
    }
    $("cancel-confirmation").addEventListener("click", closeConfirmation);
    $("close-confirmation").addEventListener("click", closeConfirmation);
    modal.addEventListener("cancel", e => { e.preventDefault(); closeConfirmation(); });
    document.addEventListener("click", event => {
        if (state.pending && event.target.closest("a")) event.preventDefault();
    }, true);
    $("logout").addEventListener("click", () => {
        if (state.pending) return;
        clearSession(); route(); notice("Vous êtes déconnecté.");
    });
    async function route() {
        if (state.pending) {
            history.replaceState(null, "", state.lastHash);
            return false;
        }
        state.lastHash = location.hash || "#/documents";
        if (modal.open) closeConfirmation();
        state.controller?.abort();
        state.controller = new AbortController();
        const signal = state.controller.signal;
        const generation = ++state.sequence;
        const current = () => generation === state.sequence && !signal.aborted;
        releasePdf(); notice();
        window.scrollTo({top: 0, behavior: "instant"});
        const r = parseRoute();
        for (const [id, active] of [["nav-documents", !["create", "tableau", "guide"].includes(r.kind)], ["nav-nouveau", r.kind === "create"], ["nav-tableau", r.kind === "tableau"], ["nav-guide", r.kind === "guide"]]) {
            $(id).classList.toggle("active", active);
            if (active) $(id).setAttribute("aria-current", "page"); else $(id).removeAttribute("aria-current");
        }
        setAccount();
        if (!state.token) { renderLogin(); return false; }
        loading(r.kind === "preview" ? "Génération de la prévisualisation…" : "Chargement…");
        try {
            if (r.kind === "list") await renderList(r, signal, current);
            else if (r.kind === "tableau") await renderTableau(r, signal, current);
            else if (r.kind === "guide") renderGuide();
            else if (r.kind === "controle") await renderControle(r, signal, current);
            else if (r.kind === "create") renderCreate();
            else if (r.kind === "detail") await renderDetail(r, signal, current);
            else if (r.kind === "historique") await renderHistory(r, signal, current);
            else if (r.kind === "preview") await renderPreview(r, signal, current);
            else if (r.kind === "remplacer") await renderReplacement(r, signal, current);
            else if (r.kind === "envois") await renderEnvois(r, signal, current);
            else if (r.kind === "signature") await renderSignature(r, signal, current);
            else if (r.kind === "versements") await renderPayments(r, signal, current);
            else if (r.kind === "archives") await renderArchives(r, signal, current);
            else throw new ApiError("Cette page n’existe pas.", 404);
            return current();
        } catch (error) {
            if (!current() || error.name === "AbortError") return false;
            if (error.status === 401) { clearSession(); renderLogin(); notice(error.message, true); return false; }
            view(`<div class="card empty"><p class="eyebrow">${esc(error.status || "CONNEXION")}</p><h1>Impossible d’afficher cette page</h1><p>${esc(error.message)}</p><div class="actions" style="justify-content:center"><a class="button secondary" href="${esc(state.listHash)}">Retour aux documents</a><button id="retry" class="button primary">Réessayer</button></div></div>`, "Erreur");
            $("retry").addEventListener("click", () => route());
            return false;
        }
    }
    window.addEventListener("hashchange", route);
    window.addEventListener("beforeunload", event => {
        if (state.pending) { event.preventDefault(); event.returnValue = ""; }
    });
    // Supprime une éventuelle ancienne URL contenant un jeton soumis par formulaire.
    if (new URLSearchParams(location.search).has("jeton")) history.replaceState(null, "", location.pathname + location.hash);
    if (!location.hash) history.replaceState(null, "", "#/documents");
    route();
})();
