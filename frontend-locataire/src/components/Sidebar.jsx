import React from 'react';
import { 
  LayoutDashboard, 
  Building2, 
  Users, 
  WalletCards, 
  FileText, 
  ChevronLeft, 
  ChevronRight, 
  Sparkles,
  ShieldCheck,
  ExternalLink
} from 'lucide-react';

export default function Sidebar({ isCollapsed, setIsCollapsed, currentModule, setCurrentModule, activeTab, setActiveTab }) {
  const navItems = [
    { id: 'dashboard', label: 'Tableau de bord', icon: LayoutDashboard, badge: 'Hub' },
    { id: 'biens', label: 'Biens immobiliers', icon: Building2 },
    { id: 'locataires', label: 'Locataires & Baux', icon: Users, active: true },
    { id: 'loyers', label: 'Loyers & Règlements', icon: WalletCards },
    { id: 'quittances', label: 'Quittances de loyer', icon: FileText, badge: 'Akram' },
  ];

  return (
    <aside 
      className={`fixed top-0 left-0 h-screen bg-midnight border-r border-midnight-border z-40 transition-all duration-300 flex flex-col justify-between select-none ${
        isCollapsed ? 'w-[82px]' : 'w-[280px]'
      }`}
    >
      {/* 1. Header Sidebar */}
      <div>
        <div className="h-[74px] flex items-center justify-between px-5 border-b border-midnight-border">
          <div className="flex items-center space-x-3 overflow-hidden">
            {/* Logo Lime */}
            <div className="w-[42px] h-[42px] rounded-[14px] bg-lime flex items-center justify-center flex-shrink-0 shadow-lime-sm">
              <span className="font-black text-midnight text-xl font-mono">L.</span>
            </div>
            {!isCollapsed && (
              <div className="truncate">
                <div className="flex items-center space-x-1.5">
                  <span className="font-extrabold text-white text-lg tracking-tight">LOCATIVE</span>
                  <span className="w-2 h-2 rounded-full bg-lime inline-block"></span>
                </div>
                <p className="text-[11px] text-slate-400 font-medium tracking-wide">Gestion Immobilière</p>
              </div>
            )}
          </div>

          <button 
            onClick={() => setIsCollapsed(!isCollapsed)}
            className="w-8 h-8 rounded-lg bg-midnight-elevated border border-midnight-border flex items-center justify-center text-slate-400 hover:text-white hover:border-lime transition-all"
            title={isCollapsed ? "Déplier le menu" : "Replier le menu"}
          >
            {isCollapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
          </button>
        </div>

        {/* 2. Navigation Principale */}
        <div className="p-4 space-y-6">
          <div>
            {!isCollapsed && (
              <p className="px-3 text-[10px] font-extrabold tracking-wider text-slate-500 uppercase mb-3">
                Navigation Principale
              </p>
            )}
            <nav className="space-y-1.5">
              {navItems.map((item) => {
                const Icon = item.icon;
                const isActive = item.id === 'locataires';

                return (
                  <button
                    key={item.id}
                    onClick={() => {
                      if (item.id === 'locataires') {
                        // Rester sur locataires
                      } else if (item.id === 'dashboard') {
                        setActiveTab('dashboard');
                      }
                    }}
                    className={`w-full flex items-center justify-between px-3.5 py-3 rounded-button font-medium text-xs transition-all ${
                      isActive
                        ? 'bg-lime text-midnight font-extrabold shadow-lime-glow'
                        : 'text-slate-400 hover:text-white hover:bg-midnight-elevated'
                    }`}
                  >
                    <div className="flex items-center space-x-3 truncate">
                      <Icon className={`w-4 h-4 flex-shrink-0 ${isActive ? 'text-midnight stroke-[2.4]' : 'text-slate-400'}`} />
                      {!isCollapsed && <span className="truncate">{item.label}</span>}
                    </div>

                    {!isCollapsed && item.badge && (
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        isActive 
                          ? 'bg-midnight text-lime font-mono' 
                          : 'bg-midnight-elevated text-slate-400 border border-midnight-border'
                      }`}>
                        {item.badge}
                      </span>
                    )}
                  </button>
                );
              })}
            </nav>
          </div>

          {/* Section Raccourcis Rapides du Module */}
          {!isCollapsed && (
            <div className="pt-2 border-t border-midnight-border">
              <p className="px-3 text-[10px] font-extrabold tracking-wider text-slate-500 uppercase mb-2">
                Gestion des Locataires
              </p>
              <div className="space-y-1">
                <button
                  onClick={() => setActiveTab('dashboard')}
                  className={`w-full text-left px-3 py-2 rounded-lg text-xs transition-colors flex items-center justify-between ${
                    activeTab === 'dashboard' ? 'text-lime font-bold bg-midnight-elevated' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <span>Vue Synthèse & KPIs</span>
                  {activeTab === 'dashboard' && <span className="w-1.5 h-1.5 rounded-full bg-lime"></span>}
                </button>
                <button
                  onClick={() => setActiveTab('list')}
                  className={`w-full text-left px-3 py-2 rounded-lg text-xs transition-colors flex items-center justify-between ${
                    activeTab === 'list' ? 'text-lime font-bold bg-midnight-elevated' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <span>Répertoire des Baux</span>
                  {activeTab === 'list' && <span className="w-1.5 h-1.5 rounded-full bg-lime"></span>}
                </button>
                <button
                  onClick={() => setActiveTab('new')}
                  className={`w-full text-left px-3 py-2 rounded-lg text-xs transition-colors flex items-center justify-between ${
                    activeTab === 'new' ? 'text-lime font-bold bg-midnight-elevated' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <span>+ Nouveau Contrat / Locataire</span>
                  {activeTab === 'new' && <span className="w-1.5 h-1.5 rounded-full bg-lime"></span>}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 3. Bottom Promo Card */}
      {!isCollapsed ? (
        <div className="p-4 border-t border-midnight-border">
          <div className="p-4 rounded-card bg-gradient-to-b from-midnight-card to-midnight border border-midnight-border relative overflow-hidden group">
            <div className="absolute top-0 right-0 w-28 h-28 bg-lime/10 rounded-full blur-2xl pointer-events-none"></div>
            
            <div className="flex items-center justify-between mb-2">
              <span className="bg-lime text-midnight text-[10px] font-extrabold px-2 py-0.5 rounded-full font-mono">
                PRO
              </span>
              <span className="text-[10px] text-slate-400 font-mono">Sujet n° 16</span>
            </div>

            <h4 className="text-white font-bold text-xs">Parc Immobilier Pro</h4>
            <p className="text-[11px] text-slate-400 mt-1 leading-snug">
              Architecture microservices PostgreSQL & Spring / Node.js
            </p>

            <div className="mt-3 pt-2.5 border-t border-midnight-border/60 flex items-center justify-between text-[10px] text-slate-500 font-mono">
              <span>v2.0 · Midnight & Lime</span>
              <span className="text-lime font-bold">100% Validé</span>
            </div>
          </div>
        </div>
      ) : (
        <div className="p-3 text-center border-t border-midnight-border">
          <div className="w-10 h-10 mx-auto rounded-xl bg-midnight-elevated border border-midnight-border flex items-center justify-center text-lime font-mono font-bold text-xs">
            P
          </div>
        </div>
      )}
    </aside>
  );
}
