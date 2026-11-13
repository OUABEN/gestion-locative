import React from 'react';
import { 
  Menu, 
  X, 
  Database, 
  LogOut, 
  Sparkles,
  LayoutDashboard,
  Users,
  UserPlus
} from 'lucide-react';

export default function Topbar({ 
  isSidebarCollapsed, 
  setIsSidebarCollapsed, 
  activeTab, 
  setActiveTab,
  totalLocataires
}) {
  return (
    <header className="h-[74px] sticky top-0 z-30 glass-topbar border-b border-slate-200/80 px-6 flex items-center justify-between select-none">
      
      {/* 1. Left: Menu Toggle + Segmented Quick-Choice Tabs */}
      <div className="flex items-center space-x-4">
        
        {/* Toggle Sidebar Button */}
        <button
          onClick={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
          className={`px-3.5 py-2 rounded-full text-xs font-bold transition-all flex items-center space-x-2 border shadow-xs ${
            isSidebarCollapsed 
              ? 'bg-lime text-midnight border-lime shadow-lime-sm' 
              : 'bg-midnight text-white border-midnight hover:bg-midnight-elevated'
          }`}
        >
          {isSidebarCollapsed ? <Menu className="w-4 h-4 stroke-[2.5]" /> : <X className="w-4 h-4" />}
          <span className="hidden sm:inline">
            {isSidebarCollapsed ? "Ouvrir menu" : "Masquer menu"}
          </span>
        </button>

        {/* Center-Left: Segmented Pill Switcher in Midnight Black (#0B0B0C) */}
        <div className="hidden lg:flex items-center bg-midnight p-1 rounded-full border border-midnight-border shadow-inner">
          <button
            onClick={() => setActiveTab('dashboard')}
            className={`px-4 py-2 rounded-full text-xs font-extrabold transition-all flex items-center space-x-2 ${
              activeTab === 'dashboard'
                ? 'bg-lime text-midnight shadow-lime-sm'
                : 'text-slate-300 hover:text-white'
            }`}
          >
            <LayoutDashboard className="w-3.5 h-3.5" />
            <span>Tableau de bord</span>
          </button>

          <button
            onClick={() => setActiveTab('list')}
            className={`px-4 py-2 rounded-full text-xs font-extrabold transition-all flex items-center space-x-2 ${
              activeTab === 'list'
                ? 'bg-lime text-midnight shadow-lime-sm'
                : 'text-slate-300 hover:text-white'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            <span>Afficher les locataires & baux</span>
            <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono ${
              activeTab === 'list' ? 'bg-midnight text-lime' : 'bg-midnight-card text-slate-400'
            }`}>
              {totalLocataires}
            </span>
          </button>

          <button
            onClick={() => setActiveTab('new')}
            className={`px-4 py-2 rounded-full text-xs font-extrabold transition-all flex items-center space-x-2 ${
              activeTab === 'new'
                ? 'bg-lime text-midnight shadow-lime-sm'
                : 'text-slate-300 hover:text-white'
            }`}
          >
            <UserPlus className="w-3.5 h-3.5" />
            <span>Nouveau locataire / bail</span>
          </button>
        </div>

      </div>

      {/* 2. Right: Database sync, User Profile Pill, Logout */}
      <div className="flex items-center space-x-3">
        
        {/* PostgreSQL Live Indicator */}
        <div className="hidden md:flex items-center space-x-2 px-3 py-1.5 rounded-full bg-slate-100 border border-slate-200 text-xs">
          <span className="w-2 h-2 rounded-full bg-lime animate-pulse"></span>
          <span className="font-mono text-[11px] text-slate-600 font-medium">PostgreSQL v15</span>
          <Database className="w-3.5 h-3.5 text-slate-400" />
        </div>

        {/* User Profile Pill */}
        <div className="flex items-center space-x-2.5 bg-white pl-1.5 pr-3 py-1.5 rounded-full border border-slate-200 shadow-card-soft">
          <span className="bg-lime text-midnight text-[10px] font-extrabold px-2.5 py-1 rounded-full font-mono tracking-wider">
            BAILLEUR
          </span>
          <span className="text-xs font-bold text-midnight hidden sm:inline">
            ahmed.laarif@locative.ma
          </span>
          <div className="w-7 h-7 rounded-full bg-midnight text-lime flex items-center justify-center font-extrabold text-[11px] font-mono border border-lime/30">
            AL
          </div>
        </div>

        {/* Logout Button */}
        <button
          onClick={() => alert("Session déconnectée en toute sécurité.")}
          className="p-2 rounded-full bg-slate-100 hover:bg-coral-50 hover:text-coral text-slate-500 border border-slate-200 transition-colors"
          title="Déconnexion"
        >
          <LogOut className="w-4 h-4" />
        </button>

      </div>

    </header>
  );
}
