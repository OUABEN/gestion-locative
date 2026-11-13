import React from 'react';
import { 
  Sparkles, 
  LayoutDashboard, 
  Users, 
  UserPlus, 
  ShieldCheck, 
  CheckCircle2, 
  ArrowUpRight 
} from 'lucide-react';

export default function HeroBanner({ activeTab, setActiveTab, totalLocataires, bauxActifs }) {
  return (
    <div className="relative rounded-hero bg-midnight p-7 sm:p-9 text-white overflow-hidden shadow-midnight-card border border-midnight-border mb-8">
      
      {/* Background radial glows & texture */}
      <div className="absolute -top-24 -left-24 w-96 h-96 bg-lime/10 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute top-1/2 -right-20 w-80 h-80 bg-lime/5 rounded-full blur-3xl pointer-events-none"></div>
      
      {/* Architectural subtle grid pattern */}
      <div 
        className="absolute inset-0 opacity-[0.035] pointer-events-none"
        style={{
          backgroundImage: `linear-gradient(#C6F432 1px, transparent 1px), linear-gradient(90deg, #C6F432 1px, transparent 1px)`,
          backgroundSize: '40px 40px'
        }}
      ></div>

      <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-8">
        
        {/* Left Side: Greeting, Eyebrow & Embedded 3-Tab Choice Bar */}
        <div className="space-y-4 max-w-2xl">
          
          {/* Eyebrow Pill */}
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-midnight-elevated border border-lime/30 text-lime text-xs font-mono font-bold tracking-wider">
            <span className="w-2 h-2 rounded-full bg-lime animate-pulse"></span>
            <span>ESPACE BAILLEUR · GESTION LOCATIVE</span>
          </div>

          {/* Title with Lime User Name */}
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight leading-none text-white">
            Bonjour, <span className="text-lime glow-lime-sm">Ahmed LAARIF</span>
          </h1>

          <p className="text-slate-300 text-xs sm:text-sm leading-relaxed max-w-xl font-normal">
            Supervision centralisée du portefeuille locatif, contrôle d'unicité absolue du CIN marocain et interopérabilité instantanée avec le module Quittances d'Akram.
          </p>

          {/* Embedded 3-Tab Choice Bar */}
          <div className="pt-2">
            <div className="inline-flex p-1.5 rounded-full bg-midnight-elevated border border-midnight-border shadow-inner max-w-full overflow-x-auto">
              <button
                onClick={() => setActiveTab('dashboard')}
                className={`px-4 py-2 rounded-full text-xs font-extrabold transition-all flex items-center space-x-2 whitespace-nowrap ${
                  activeTab === 'dashboard'
                    ? 'bg-lime text-midnight shadow-lime-glow'
                    : 'text-slate-300 hover:text-white hover:bg-midnight-card'
                }`}
              >
                <LayoutDashboard className="w-3.5 h-3.5" />
                <span>Tableau de bord</span>
              </button>

              <button
                onClick={() => setActiveTab('list')}
                className={`px-4 py-2 rounded-full text-xs font-extrabold transition-all flex items-center space-x-2 whitespace-nowrap ${
                  activeTab === 'list'
                    ? 'bg-lime text-midnight shadow-lime-glow'
                    : 'text-slate-300 hover:text-white hover:bg-midnight-card'
                }`}
              >
                <Users className="w-3.5 h-3.5" />
                <span>Afficher les locataires & baux</span>
                <span className={`text-[10px] font-mono px-1.5 py-0.2 rounded-full ${
                  activeTab === 'list' ? 'bg-midnight text-lime' : 'bg-midnight text-slate-400'
                }`}>
                  {totalLocataires}
                </span>
              </button>

              <button
                onClick={() => setActiveTab('new')}
                className={`px-4 py-2 rounded-full text-xs font-extrabold transition-all flex items-center space-x-2 whitespace-nowrap ${
                  activeTab === 'new'
                    ? 'bg-lime text-midnight shadow-lime-glow'
                    : 'text-slate-300 hover:text-white hover:bg-midnight-card'
                }`}
              >
                <UserPlus className="w-3.5 h-3.5" />
                <span>Nouveau locataire / bail</span>
              </button>
            </div>
          </div>

        </div>

        {/* Right Side: Floating Glass Stat Card */}
        <div className="lg:w-[320px] flex-shrink-0">
          <div className="p-5 rounded-card bg-midnight-card/90 border border-midnight-border backdrop-blur-xl shadow-2xl relative overflow-hidden group hover:border-lime/40 transition-all">
            
            {/* Live Synchronized Pill */}
            <div className="flex items-center justify-between mb-3">
              <span className="text-[10px] uppercase font-mono font-extrabold tracking-wider text-slate-400">
                Performance du Parc
              </span>
              <div className="flex items-center space-x-1.5 bg-midnight px-2 py-0.5 rounded-full border border-midnight-border text-[10px] font-mono text-lime">
                <span className="w-1.5 h-1.5 rounded-full bg-lime animate-ping"></span>
                <span>PostgreSQL Live</span>
              </div>
            </div>

            {/* Large 46px Metric in Lime */}
            <div className="flex items-baseline space-x-2">
              <span className="text-4xl sm:text-[46px] font-black text-lime font-mono tracking-tight leading-none tabular-nums">
                98.4 %
              </span>
              <span className="text-xs text-slate-300 font-bold flex items-center">
                <ArrowUpRight className="w-3.5 h-3.5 text-lime mr-0.5" />
                +2.1%
              </span>
            </div>

            <p className="text-xs font-semibold text-white mt-1">
              Taux d'occupation des biens
            </p>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {bauxActifs} baux actifs enregistrés sous contrat officiel
            </p>

            {/* Horizontal Lime Progress Bar */}
            <div className="w-full h-2 rounded-full bg-midnight-elevated mt-4 overflow-hidden p-0.5 border border-midnight-border">
              <div 
                className="h-full rounded-full bg-lime shadow-lime-glow transition-all duration-1000 ease-out"
                style={{ width: '98.4%' }}
              ></div>
            </div>

            <div className="flex items-center justify-between mt-2 text-[10px] font-mono text-slate-400">
              <span>Objectif Q4: 95%</span>
              <span className="text-lime font-bold">Surpassé ✓</span>
            </div>

          </div>
        </div>

      </div>

    </div>
  );
}
