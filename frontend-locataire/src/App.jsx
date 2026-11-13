import React, { useState } from 'react';
import Sidebar from './components/Sidebar';
import Topbar from './components/Topbar';
import HeroBanner from './components/HeroBanner';
import KpiGrid from './components/KpiGrid';
import AnalyticsSection from './components/AnalyticsSection';
import TableView from './components/TableView';
import AddTenantLeaseView from './components/AddTenantLeaseView';
import QuittanceModal from './components/QuittanceModal';
import ModifierLocataireModal from './components/ModifierLocataireModal';
import SupprimerLocataireModal from './components/SupprimerLocataireModal';
import DetailLocataireModal from './components/DetailLocataireModal';
import { initialLocatairesEtBaux } from './data/locativeData';
import { CheckCircle2, AlertTriangle, ShieldAlert } from 'lucide-react';

export default function App() {
  const [locataires, setLocataires] = useState(initialLocatairesEtBaux);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  
  // 3 Quick-Choice Tabs: "dashboard" | "list" | "new"
  const [activeTab, setActiveTab] = useState('dashboard');

  // Modals state
  const [selectedQuittance, setSelectedQuittance] = useState(null);
  const [selectedDetail, setSelectedDetail] = useState(null);
  const [selectedEdit, setSelectedEdit] = useState(null);
  const [selectedDelete, setSelectedDelete] = useState(null);
  const [toastMessage, setToastMessage] = useState('');

  const triggerToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 4000);
  };

  const existingCins = locataires.map(l => l.cin.toUpperCase());
  const bauxActifs = locataires.filter(l => l.statutBail === 'ACTIF').length;

  // Enregistrer un nouveau bail / locataire
  const handleSaveNewLease = (newRecord) => {
    setLocataires([newRecord, ...locataires]);
    setActiveTab('list');
    triggerToast(`Contrat de bail créé avec succès pour ${newRecord.prenom} ${newRecord.nom} (CIN: ${newRecord.cin}) !`);
  };

  // Mettre à jour les coordonnées
  const handleUpdateCoordinates = (id, updatedFields) => {
    setLocataires(locataires.map(l => {
      if (l.id === id) {
        return {
          ...l,
          ...updatedFields
        };
      }
      return l;
    }));
    triggerToast(`Coordonnées mises à jour avec succès.`);
  };

  // Supprimer un locataire (uniquement si bail non actif)
  const handleDeleteRecord = (id) => {
    const target = locataires.find(l => l.id === id);
    if (target?.statutBail === 'ACTIF') {
      alert("Suppression impossible : Ce locataire possède un bail actif rattaché.");
      return;
    }
    setLocataires(locataires.filter(l => l.id !== id));
    triggerToast(`Fiche de ${target?.prenom} ${target?.nom} retirée du système.`);
  };

  return (
    <div className="min-h-screen bg-canvas text-midnight flex flex-col font-sans antialiased">
      
      {/* 1. Left Collapsible Sidebar */}
      <Sidebar
        isCollapsed={isSidebarCollapsed}
        setIsCollapsed={setIsSidebarCollapsed}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
      />

      {/* 2. Main Wrapper (Shifted when sidebar is expanded) */}
      <div 
        className={`flex-1 flex flex-col transition-all duration-300 ${
          isSidebarCollapsed ? 'pl-[82px]' : 'pl-[82px] lg:pl-[280px]'
        }`}
      >
        {/* Sticky Topbar */}
        <Topbar
          isSidebarCollapsed={isSidebarCollapsed}
          setIsSidebarCollapsed={setIsSidebarCollapsed}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          totalLocataires={locataires.length}
        />

        {/* Workspace Content */}
        <main className="p-6 sm:p-8 max-w-[1600px] w-full mx-auto flex-1">
          
          {/* Top Hero Banner */}
          <HeroBanner
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            totalLocataires={locataires.length}
            bauxActifs={bauxActifs}
          />

          {/* TAB 1: Tableau de bord (Overview avec KPIs + Charts + Récents) */}
          {activeTab === 'dashboard' && (
            <div className="space-y-8 animate-in fade-in duration-300">
              {/* 6 KPIs Grid */}
              <KpiGrid locataires={locataires} />

              {/* Analytics Section (Midnight Chart + Pure White Donut) */}
              <AnalyticsSection />

              {/* Aperçu rapide des derniers baux sous contrat */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-lime shadow-lime-sm"></span>
                    <h3 className="font-extrabold text-base text-midnight">
                      Derniers Contrats de Baux Enregistrés
                    </h3>
                  </div>
                  <button
                    onClick={() => setActiveTab('list')}
                    className="text-xs font-bold text-midnight hover:text-black underline underline-offset-4"
                  >
                    Voir tout le répertoire ({locataires.length}) →
                  </button>
                </div>

                <TableView
                  locataires={locataires.slice(0, 4)}
                  onOpenCreate={() => setActiveTab('new')}
                  onOpenDetail={(loc) => setSelectedDetail(loc)}
                  onOpenQuittance={(loc) => setSelectedQuittance(loc)}
                  onOpenEdit={(loc) => setSelectedEdit(loc)}
                  onOpenDelete={(loc) => setSelectedDelete(loc)}
                />
              </div>
            </div>
          )}

          {/* TAB 2: Afficher les locataires & baux (Full Catalogue Table) */}
          {activeTab === 'list' && (
            <div className="space-y-6 animate-in fade-in duration-300">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl sm:text-2xl font-black text-midnight tracking-tight">
                    Répertoire Officiel des Locataires & Baux
                  </h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Consultation des contrats, recherche par CIN marocain et passerelle documentaire.
                  </p>
                </div>
              </div>

              <TableView
                locataires={locataires}
                onOpenCreate={() => setActiveTab('new')}
                onOpenDetail={(loc) => setSelectedDetail(loc)}
                onOpenQuittance={(loc) => setSelectedQuittance(loc)}
                onOpenEdit={(loc) => setSelectedEdit(loc)}
                onOpenDelete={(loc) => setSelectedDelete(loc)}
              />
            </div>
          )}

          {/* TAB 3: Nouveau locataire / bail (Formulaire d'enregistrement) */}
          {activeTab === 'new' && (
            <div className="animate-in fade-in duration-300">
              <AddTenantLeaseView
                onSaveNewLease={handleSaveNewLease}
                existingCins={existingCins}
                onCancel={() => setActiveTab('list')}
              />
            </div>
          )}

        </main>

        {/* Footer */}
        <footer className="border-t border-slate-200 py-5 px-8 text-xs text-slate-500 bg-white">
          <div className="max-w-[1600px] mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="flex items-center space-x-2">
              <span className="font-extrabold text-midnight">LOCATIVE.</span>
              <span>— Plateforme de Gestion Locative Marocaine</span>
            </div>
            <div className="font-mono text-[11px] text-slate-400">
              Module 02 · LAARIF Ahmed · Sujet n° 16 · Groupe 16
            </div>
            <div className="flex items-center space-x-2">
              <span className="w-2 h-2 rounded-full bg-lime"></span>
              <span className="font-mono font-bold text-midnight text-[11px]">v2.0 Midnight & Lime Edition</span>
            </div>
          </div>
        </footer>

      </div>

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-midnight text-white px-5 py-3.5 rounded-button shadow-2xl border border-midnight-border flex items-center space-x-3 text-xs animate-in slide-in-from-bottom-5">
          <CheckCircle2 className="w-4 h-4 text-lime flex-shrink-0" />
          <span className="font-bold">{toastMessage}</span>
        </div>
      )}

      {/* MODALS */}
      <QuittanceModal
        isOpen={Boolean(selectedQuittance)}
        onClose={() => setSelectedQuittance(null)}
        locataire={selectedQuittance}
      />

      <DetailLocataireModal
        isOpen={Boolean(selectedDetail)}
        onClose={() => setSelectedDetail(null)}
        locataire={selectedDetail}
      />

      <ModifierLocataireModal
        isOpen={Boolean(selectedEdit)}
        onClose={() => setSelectedEdit(null)}
        onUpdate={handleUpdateCoordinates}
        locataire={selectedEdit}
      />

      <SupprimerLocataireModal
        isOpen={Boolean(selectedDelete)}
        onClose={() => setSelectedDelete(null)}
        onConfirmDelete={handleDeleteRecord}
        locataire={selectedDelete}
      />

    </div>
  );
}
