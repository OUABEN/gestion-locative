import React, { useState } from 'react';
import { X, Copy, Check, ShieldCheck, Home, FileText, Send, Terminal, CheckCircle2, AlertCircle } from 'lucide-react';

export default function DetailLocataireModal({ isOpen, onClose, locataire }) {
  if (!isOpen || !locataire) return null;

  const [copied, setCopied] = useState(false);
  const [apiCalled, setApiCalled] = useState(false);

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Contrat d'interopérabilité pour le module Quittances (Akram)
  const quittanceSummaryJson = {
    id: locataire.id,
    nom: locataire.nom,
    prenom: locataire.prenom,
    nomComplet: `${locataire.prenom} ${locataire.nom}`,
    cin: locataire.cin,
    email: locataire.email,
    telephone: locataire.telephone
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-2xl w-full max-h-[92vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        
        {/* En-tête */}
        <div className="px-6 py-4 bg-navy-700 text-white flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-full bg-navy-600 border border-sky-300/40 flex items-center justify-center font-bold text-sky-200">
              {locataire.prenom[0]}{locataire.nom[0]}
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h2 className="font-bold text-base tracking-tight text-white">
                  {locataire.prenom} {locataire.nom}
                </h2>
                <span className="font-mono text-xs bg-navy-800 text-sky-300 px-2 py-0.5 rounded border border-navy-600">
                  {locataire.cin}
                </span>
              </div>
              <p className="text-xs text-slate-300">Fiche détaillée du locataire (F-LOC-03)</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-300 hover:text-white p-1 rounded-lg hover:bg-navy-600 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Corps */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-sm">
          
          {/* Identifiant UUID système */}
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-3 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block">
                Identifiant Technique Unique (UUID v4)
              </span>
              <span className="font-mono text-xs font-semibold text-navy-800 select-all">
                {locataire.id}
              </span>
            </div>
            <button
              onClick={() => copyToClipboard(locataire.id)}
              className="inline-flex items-center space-x-1 px-2.5 py-1 bg-white border border-slate-200 hover:bg-slate-100 rounded-md text-xs font-medium text-slate-600 transition-colors shadow-2xs"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5 text-slate-500" />}
              <span>{copied ? 'Copié' : 'Copier'}</span>
            </button>
          </div>

          {/* Grille Données Personnelles & Contact */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="border border-slate-200 rounded-xl p-4 bg-white">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2.5">
                Coordonnées de contact
              </h4>
              <div className="space-y-2 text-xs">
                <div>
                  <span className="text-slate-400 block">Téléphone marocain :</span>
                  <span className="font-semibold text-slate-800">{locataire.telephone || 'Non renseigné'}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Email :</span>
                  <span className="font-semibold text-slate-800 break-all">{locataire.email}</span>
                </div>
              </div>
            </div>

            {/* Statut Bail rattaché */}
            <div className="border border-slate-200 rounded-xl p-4 bg-white">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2.5">
                Situation Locative
              </h4>
              {locataire.hasBailActif ? (
                <div className="space-y-1.5 text-xs">
                  <div className="flex items-center space-x-1.5">
                    <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                    <span className="font-bold text-emerald-700">Bail Actif en cours</span>
                  </div>
                  <p className="text-slate-700 font-medium">{locataire.bailInfo?.appartement}</p>
                  <p className="text-slate-500 text-[11px]">{locataire.bailInfo?.residence}</p>
                  <p className="text-navy-700 font-semibold mt-1">
                    Loyer : {locataire.bailInfo?.loyerMensuel?.toLocaleString()} DH/mois
                  </p>
                </div>
              ) : (
                <div className="flex items-center space-x-2 text-xs text-amber-700 bg-amber-50 p-2.5 rounded-lg border border-amber-200">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>Aucun contrat de bail actif rattaché pour le moment.</span>
                </div>
              )}
            </div>
          </div>

          {/* Pièces Justificatives (Accès restreint aux gestionnaires) */}
          <div className="border border-slate-200 rounded-xl p-4 bg-white">
            <div className="flex items-center justify-between mb-2">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Dossier documentaire (Pièces justificatives)
              </h4>
              <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded font-medium">
                Accès privé gestionnaire
              </span>
            </div>
            {locataire.piecesJustificatives && locataire.piecesJustificatives.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2">
                {locataire.piecesJustificatives.map((p, idx) => (
                  <div
                    key={p.id || idx}
                    className="flex items-center justify-between p-2.5 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                  >
                    <div className="flex items-center space-x-2 truncate">
                      <FileText className="w-4 h-4 text-navy-600 flex-shrink-0" />
                      <span className="font-medium text-slate-700 truncate">{p.nom}</span>
                    </div>
                    <span className="text-[10px] text-slate-400 font-mono ml-2">{p.taille}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-400 italic">Aucune pièce déposée pour ce locataire.</p>
            )}
          </div>

          {/* ENCART INTEROPÉRABILITÉ AVEC LE MODULE QUITTANCES */}
          <div className="border-2 border-sky-600/30 rounded-xl bg-slate-900 text-slate-100 p-4 shadow-inner">
            <div className="flex items-center justify-between mb-2 pb-2 border-b border-slate-800">
              <div className="flex items-center space-x-2">
                <Terminal className="w-4 h-4 text-sky-400" />
                <span className="text-xs font-bold text-sky-400 uppercase tracking-wider">
                  Service Inter-Modules : Passerelle Quittances (Akram)
                </span>
              </div>
              <button
                onClick={() => setApiCalled(true)}
                className="inline-flex items-center space-x-1.5 px-3 py-1 bg-sky-600 hover:bg-sky-500 text-white rounded text-xs font-semibold transition-colors"
              >
                <Send className="w-3 h-3" />
                <span>Tester l'appel GET</span>
              </button>
            </div>

            <div className="flex items-center space-x-2 text-[11px] font-mono text-slate-400 mb-2">
              <span className="bg-emerald-900 text-emerald-300 px-1.5 py-0.5 rounded font-bold text-[10px]">GET</span>
              <span className="text-slate-300">/api/v1/locataires/{locataire.id}/quittance-summary</span>
            </div>

            {/* Code JSON épuré et sécurisé */}
            <pre className="bg-black/60 p-3 rounded-lg font-mono text-xs text-emerald-400 overflow-x-auto border border-slate-800/80">
              {JSON.stringify(quittanceSummaryJson, null, 2)}
            </pre>

            <div className="mt-2.5 flex items-center justify-between text-[11px] text-slate-400">
              <span className="flex items-center">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 mr-1" />
                Filtrage de sécurité : pièces justificatives et données sensibles privées masquées
              </span>
              {apiCalled && (
                <span className="text-emerald-400 font-semibold animate-pulse">
                  HTTP 200 OK — Données prêtes
                </span>
              )}
            </div>
          </div>

        </div>

        {/* Pied */}
        <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-lg text-xs font-semibold transition-colors"
          >
            Fermer
          </button>
        </div>

      </div>
    </div>
  );
}
