import React from 'react';
import { Users, ShieldCheck, Database, GitBranch } from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab, locatairesCount }) {
  return (
    <header className="bg-navy-700 text-white shadow-md border-b border-navy-800 sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          
          {/* Logo & Titres */}
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-lg bg-navy-600 flex items-center justify-center border border-navy-500 shadow-inner">
              <Users className="w-6 h-6 text-sky-300" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-lg tracking-tight text-white">Gestion Locative</span>
                <span className="bg-navy-900 text-sky-300 text-xs font-semibold px-2.5 py-0.5 rounded-full border border-sky-400/30">
                  Groupe 16
                </span>
                <span className="hidden sm:inline-block bg-emerald-950/80 text-emerald-300 text-[11px] font-medium px-2 py-0.5 rounded border border-emerald-600/30">
                  Sujet n° 16
                </span>
              </div>
              <p className="text-xs text-slate-300 flex items-center space-x-1.5 mt-0.5">
                <span className="font-semibold text-white">Partie 02 : Gestion des locataires</span>
                <span>•</span>
                <span className="text-sky-200">LAARIF Ahmed</span>
              </p>
            </div>
          </div>

          {/* Navigation & Statut Backend */}
          <div className="flex items-center space-x-4">
            <div className="hidden md:flex items-center bg-navy-800/80 rounded-lg p-1 border border-navy-600/50">
              <button
                onClick={() => setActiveTab('repertoire')}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${
                  activeTab === 'repertoire'
                    ? 'bg-sky-500 text-white shadow'
                    : 'text-slate-300 hover:text-white hover:bg-navy-700'
                }`}
              >
                Répertoire ({locatairesCount})
              </button>
              <button
                onClick={() => setActiveTab('interop')}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all flex items-center space-x-1 ${
                  activeTab === 'interop'
                    ? 'bg-sky-500 text-white shadow'
                    : 'text-slate-300 hover:text-white hover:bg-navy-700'
                }`}
              >
                <GitBranch className="w-3.5 h-3.5" />
                <span>Interopérabilité Quittances</span>
              </button>
            </div>

            {/* Statut API Microservice */}
            <div className="flex items-center space-x-2 bg-navy-800/70 px-3 py-1.5 rounded-full border border-navy-600 text-xs">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <span className="text-slate-200 font-mono text-[11px] hidden sm:inline">REST :8082 / :8080</span>
              <Database className="w-3.5 h-3.5 text-slate-400" />
            </div>
          </div>

        </div>
      </div>
    </header>
  );
}
