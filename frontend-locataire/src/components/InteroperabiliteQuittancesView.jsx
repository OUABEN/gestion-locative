import React, { useState } from 'react';
import { GitBranch, ShieldCheck, Terminal, Copy, Check, ArrowRight, FileCheck, Layers } from 'lucide-react';

export default function InteroperabiliteQuittancesView({ locataires }) {
  const [selectedLocataireId, setSelectedLocataireId] = useState(locataires[0]?.id || '');
  const [copied, setCopied] = useState(false);
  const [activeTab, setActiveTab] = useState('summary'); // 'summary' | 'full'

  const selectedLocataire = locataires.find(l => l.id === selectedLocataireId) || locataires[0];

  const quittanceSummary = selectedLocataire ? {
    id: selectedLocataire.id,
    nom: selectedLocataire.nom,
    prenom: selectedLocataire.prenom,
    nomComplet: `${selectedLocataire.prenom} ${selectedLocataire.nom}`,
    cin: selectedLocataire.cin,
    email: selectedLocataire.email,
    telephone: selectedLocataire.telephone
  } : {};

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      
      {/* Bannière explicative de l'interopérabilité */}
      <div className="bg-gradient-to-r from-navy-800 to-navy-900 rounded-2xl p-6 text-white shadow-md border border-navy-700">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="inline-flex items-center space-x-1.5 px-2.5 py-0.5 rounded-full bg-sky-500/20 text-sky-300 text-xs font-semibold border border-sky-400/30">
              <GitBranch className="w-3.5 h-3.5" />
              <span>Contrat d'API Inter-Microservices</span>
            </div>
            <h2 className="text-xl font-bold tracking-tight">Passerelle : Module Quittances (EL BARKAOUI Akram)</h2>
            <p className="text-xs text-slate-300 max-w-2xl leading-relaxed">
              Le service « Gestion des locataires » expose l'endpoint sécurisé <code className="bg-navy-950 px-1.5 py-0.5 rounded text-sky-300 font-mono text-[11px]">GET /api/v1/locataires/:id/quittance-summary</code>. Il alimente la génération automatisée des quittances PDF avec les informations d'identité certifiées, tout en masquant rigoureusement les pièces justificatives privées.
            </p>
          </div>
          <div className="bg-navy-950/80 p-3.5 rounded-xl border border-navy-700/60 text-xs text-slate-300 flex-shrink-0">
            <div className="font-semibold text-white flex items-center space-x-1.5 mb-1">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Principe de Moindre Privilège</span>
            </div>
            <p className="text-[11px] text-slate-400">
              Aucun accès direct à la base de données SQL • Échanges purs en JSON
            </p>
          </div>
        </div>
      </div>

      {/* Sélecteur de locataire et comparaison des flux */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Liste des locataires sélectionnables */}
        <div className="bg-white rounded-xl border border-slate-200 p-4 card-shadow space-y-3">
          <h3 className="font-bold text-xs uppercase tracking-wider text-slate-500">
            Sélectionner un locataire (Échantillon)
          </h3>
          <div className="space-y-2">
            {locataires.map(l => (
              <button
                key={l.id}
                onClick={() => setSelectedLocataireId(l.id)}
                className={`w-full text-left p-3 rounded-xl border text-xs transition-all flex items-center justify-between ${
                  selectedLocataireId === l.id
                    ? 'border-navy-700 bg-navy-50/70 shadow-xs'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <div>
                  <div className="font-semibold text-slate-800">{l.prenom} {l.nom}</div>
                  <div className="font-mono text-[11px] text-slate-500 mt-0.5">CIN: {l.cin}</div>
                </div>
                <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                  l.hasBailActif ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'
                }`}>
                  {l.hasBailActif ? 'Bail Actif' : 'Sans bail'}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* Console de sortie JSON inter-services */}
        <div className="lg:col-span-2 bg-slate-900 rounded-xl border border-slate-800 shadow-xl overflow-hidden flex flex-col">
          
          {/* Header console */}
          <div className="px-4 py-3 bg-slate-950 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Terminal className="w-4 h-4 text-sky-400" />
              <span className="font-mono text-xs text-slate-300 font-semibold">
                GET http://localhost:8082/api/v1/locataires/{selectedLocataire?.id}/quittance-summary
              </span>
            </div>
            <button
              onClick={() => copyToClipboard(JSON.stringify(quittanceSummary, null, 2))}
              className="inline-flex items-center space-x-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-mono transition-colors"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'Copié' : 'Copier JSON'}</span>
            </button>
          </div>

          {/* Onglets de comparaison des données */}
          <div className="px-4 py-2 bg-slate-900/90 border-b border-slate-800 flex items-center justify-between text-xs">
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setActiveTab('summary')}
                className={`px-3 py-1 rounded font-medium transition-colors ${
                  activeTab === 'summary' ? 'bg-sky-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                Payload Transmis à Quittances (Épuré)
              </button>
              <button
                onClick={() => setActiveTab('full')}
                className={`px-3 py-1 rounded font-medium transition-colors ${
                  activeTab === 'full' ? 'bg-sky-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                Objet Référentiel Complet (Interne)
              </button>
            </div>
            <span className="text-[11px] text-emerald-400 font-mono">Status: 200 OK</span>
          </div>

          {/* Affichage du code JSON avec coloration syntaxique */}
          <div className="p-4 overflow-x-auto flex-1 font-mono text-xs text-emerald-400 leading-relaxed bg-black/40">
            <pre>
              {activeTab === 'summary'
                ? JSON.stringify(quittanceSummary, null, 2)
                : JSON.stringify(selectedLocataire, null, 2)}
            </pre>
          </div>

          {/* Note de pied */}
          <div className="px-4 py-2.5 bg-slate-950 border-t border-slate-800/70 text-[11px] text-slate-400 flex items-center justify-between">
            <span>Consommé par : <strong className="text-white">Service Quittances (Akram)</strong></span>
            <span>Format : application/json</span>
          </div>

        </div>

      </div>

    </div>
  );
}
