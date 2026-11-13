import React, { useState } from 'react';
import Navbar from './components/Navbar';
import StatsCards from './components/StatsCards';
import RepertoireLocataires from './components/RepertoireLocataires';
import NouveauLocataireModal from './components/NouveauLocataireModal';
import DetailLocataireModal from './components/DetailLocataireModal';
import ModifierLocataireModal from './components/ModifierLocataireModal';
import SupprimerLocataireModal from './components/SupprimerLocataireModal';
import InteroperabiliteQuittancesView from './components/InteroperabiliteQuittancesView';
import { initialLocataires } from './data/mockLocataires';
import { CheckCircle2, ShieldCheck } from 'lucide-react';

export default function App() {
  const [locataires, setLocataires] = useState(initialLocataires);
  const [activeTab, setActiveTab] = useState('repertoire'); // 'repertoire' | 'interop'

  // Modales
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [detailLocataire, setDetailLocataire] = useState(null);
  const [editLocataire, setEditLocataire] = useState(null);
  const [deleteLocataire, setDeleteLocataire] = useState(null);

  // Toast notification
  const [toastMessage, setToastMessage] = useState('');

  const triggerToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3500);
  };

  // Liste des CIN existants pour vérification d'unicité en temps réel
  const existingCins = locataires.map(l => l.cin.toUpperCase());

  // CREATE (F-LOC-01)
  const handleSaveNewLocataire = (newLoc) => {
    const locataireWithId = {
      ...newLoc,
      id: crypto.randomUUID ? crypto.randomUUID() : `loc-${Date.now()}`,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    setLocataires([locataireWithId, ...locataires]);
    triggerToast(`Fiche de ${newLoc.prenom} ${newLoc.nom} (CIN: ${newLoc.cin}) créée avec succès !`);
  };

  // UPDATE (F-LOC-04)
  const handleUpdateLocataire = (id, updatedFields) => {
    setLocataires(locataires.map(l => {
      if (l.id === id) {
        return {
          ...l,
          ...updatedFields,
          updatedAt: new Date().toISOString()
        };
      }
      return l;
    }));
    triggerToast(`Coordonnées mises à jour avec succès.`);
  };

  // DELETE (F-LOC-05)
  const handleDeleteLocataire = (id) => {
    const target = locataires.find(l => l.id === id);
    setLocataires(locataires.filter(l => l.id !== id));
    triggerToast(`Fiche de ${target?.prenom} ${target?.nom} supprimée du référentiel.`);
  };

  return (
    <div className="min-h-screen bg-surface flex flex-col">
      
      {/* Barre de navigation supérieure */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        locatairesCount={locataires.length}
      />

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-5 right-5 z-50 bg-navy-900 text-white px-4 py-3 rounded-xl shadow-2xl border border-navy-700 flex items-center space-x-2 text-xs animate-in slide-in-from-bottom-5">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />
          <span className="font-medium">{toastMessage}</span>
        </div>
      )}

      {/* Contenu principal */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 w-full flex-1">
        
        {/* Cartes d'indicateurs de synthèse */}
        <StatsCards locataires={locataires} />

        {/* Vue principale sélectionnée */}
        {activeTab === 'repertoire' ? (
          <RepertoireLocataires
            locataires={locataires}
            onOpenCreate={() => setIsCreateOpen(true)}
            onOpenDetail={(loc) => setDetailLocataire(loc)}
            onOpenEdit={(loc) => setEditLocataire(loc)}
            onOpenDelete={(loc) => setDeleteLocataire(loc)}
          />
        ) : (
          <InteroperabiliteQuittancesView locataires={locataires} />
        )}

      </main>

      {/* Pied de page académique */}
      <footer className="bg-white border-t border-slate-200 py-4 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>Master d'Excellence Web Services — Projet Sujet n° 16 (Groupe 16)</span>
          <span className="font-medium text-navy-700">Module 02 : Gestion des locataires — LAARIF Ahmed</span>
          <span className="text-[11px] text-slate-400">Microservice Node.js / Express • Architecture REST JSON</span>
        </div>
      </footer>

      {/* MODALES DES VUES FONCTIONNELLES */}

      {/* F-LOC-01 : Création avec contrôle d'unicité CIN */}
      <NouveauLocataireModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSave={handleSaveNewLocataire}
        existingCins={existingCins}
      />

      {/* F-LOC-03 : Détail & Interopérabilité Quittances */}
      <DetailLocataireModal
        isOpen={Boolean(detailLocataire)}
        onClose={() => setDetailLocataire(null)}
        locataire={detailLocataire}
      />

      {/* F-LOC-04 : Modification sécurisée coordonnées */}
      <ModifierLocataireModal
        isOpen={Boolean(editLocataire)}
        onClose={() => setEditLocataire(null)}
        onUpdate={handleUpdateLocataire}
        locataire={editLocataire}
      />

      {/* F-LOC-05 : Suppression sécurisée avec blocage si bail actif */}
      <SupprimerLocataireModal
        isOpen={Boolean(deleteLocataire)}
        onClose={() => setDeleteLocataire(null)}
        onConfirmDelete={handleDeleteLocataire}
        locataire={deleteLocataire}
      />

    </div>
  );
}
