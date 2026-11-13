import React, { useState } from 'react';
import { X, Copy, Check, Terminal, ShieldCheck, Send, CheckCircle2, FileText, ArrowRight } from 'lucide-react';

export default function QuittanceModal({ isOpen, onClose, locataire }) {
  if (!isOpen || !locataire) return null;

  const [copied, setCopied] = useState(false);
  const [testSent, setTestSent] = useState(false);

  const quittanceData = {
    id: locataire.id,
    nom: locataire.nom,
    prenom: locataire.prenom,
    nomComplet: `${locataire.prenom} ${locataire.nom}`,
    cin: locataire.cin,
    telephone: locataire.telephone,
    email: locataire.email,
    bienLoue: {
      titre: locataire.bien?.titre,
      ville: locataire.bien?.ville,
      loyerMensuelDH: locataire.loyerMensuel
    },
    statutBail: locataire.statutBail
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(JSON.stringify(quittanceData, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop select-none">
      <div className="bg-white rounded-card shadow-2xl border border-slate-200 max-w-2xl w-full max-h-[92vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        
        {/* Header */}
        <div className="px-6 py-4 bg-midnight text-white flex items-center justify-between border-b border-midnight-border">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-lime flex items-center justify-center text-midnight font-bold shadow-lime-sm">
              <FileText className="w-5 h-5 stroke-[2.5]" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="font-extrabold text-sm text-white">Passerelle Inter-Microservices Quittances</h3>
                <span className="bg-lime text-midnight text-[10px] font-mono font-extrabold px-2 py-0.5 rounded-full">
                  Module 04 (Akram)
                </span>
              </div>
              <p className="text-[11px] text-slate-400">Endpoint : GET /api/v1/locataires/:id/quittance-summary</p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-midnight-elevated transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto space-y-4 text-xs flex-1">
          
          {/* Principle of Least Privilege Notice */}
          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex items-start space-x-3 text-slate-700">
            <ShieldCheck className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-slate-900 block">Sécurité & Principe du Moindre Privilège :</span>
              <p className="text-[11px] text-slate-500 mt-0.5 leading-relaxed">
                Les pièces justificatives privées et données financières sensibles sont rigoureusement filtrées. Seules les données requises pour la génération de la quittance PDF sont retournées.
              </p>
            </div>
          </div>

          {/* Terminal Console */}
          <div className="rounded-xl bg-midnight p-4 border border-midnight-border text-white space-y-3 font-mono shadow-inner">
            <div className="flex items-center justify-between pb-2 border-b border-midnight-border/70 text-[11px]">
              <div className="flex items-center space-x-2 text-slate-400">
                <Terminal className="w-4 h-4 text-lime" />
                <span className="text-lime font-bold">200 OK</span>
                <span>• application/json</span>
              </div>
              <button
                onClick={copyToClipboard}
                className="px-2.5 py-1 rounded bg-midnight-elevated border border-midnight-border hover:border-lime text-slate-300 text-[11px] flex items-center space-x-1.5 transition-colors"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-lime" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'Copié !' : 'Copier JSON'}</span>
              </button>
            </div>

            <pre className="text-emerald-400 text-xs overflow-x-auto leading-relaxed p-1">
              {JSON.stringify(quittanceData, null, 2)}
            </pre>
          </div>

          {/* Test simulation button */}
          <div className="p-3 rounded-xl bg-slate-100 flex items-center justify-between">
            <span className="text-[11px] text-slate-600 font-medium">
              Simuler l'appel API depuis le module Quittances d'Akram :
            </span>
            <button
              onClick={() => setTestSent(true)}
              className="px-3 py-1.5 rounded-button bg-midnight hover:bg-black text-lime font-mono font-bold text-xs flex items-center space-x-1.5 transition-colors"
            >
              <Send className="w-3 h-3" />
              <span>Tester la requête HTTP</span>
            </button>
          </div>

          {testSent && (
            <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 flex items-center space-x-2 animate-in fade-in">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              <span className="font-semibold text-xs">
                Requête REST exécutée avec succès en 14ms (HTTP 200 OK) — Données injectées dans le template PDF.
              </span>
            </div>
          )}

        </div>

        {/* Footer */}
        <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 bg-midnight text-white hover:bg-black rounded-button text-xs font-bold transition-colors"
          >
            Fermer
          </button>
        </div>

      </div>
    </div>
  );
}
