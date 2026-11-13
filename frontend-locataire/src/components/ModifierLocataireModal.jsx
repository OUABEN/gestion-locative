import React, { useState, useEffect } from 'react';
import { X, Edit3, Lock, Check } from 'lucide-react';

export default function ModifierLocataireModal({ isOpen, onClose, onUpdate, locataire }) {
  if (!isOpen || !locataire) return null;

  const [formData, setFormData] = useState({
    email: '',
    telephone: ''
  });

  useEffect(() => {
    if (locataire) {
      setFormData({
        email: locataire.email || '',
        telephone: locataire.telephone || ''
      });
    }
  }, [locataire]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onUpdate(locataire.id, {
      email: formData.email.trim(),
      telephone: formData.telephone.trim()
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-lg w-full overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        
        {/* En-tête */}
        <div className="px-6 py-4 bg-navy-700 text-white flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-lg bg-navy-600 flex items-center justify-center text-sky-300">
              <Edit3 className="w-4 h-4" />
            </div>
            <div>
              <h2 className="font-bold text-base tracking-tight">Modifier les coordonnées (F-LOC-04)</h2>
              <p className="text-xs text-slate-300">Mise à jour des informations de contact du locataire</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-300 hover:text-white p-1 rounded-lg hover:bg-navy-600 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Formulaire */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          
          {/* Note sur les champs verrouillés */}
          <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl flex items-center space-x-2.5 text-xs text-slate-600">
            <Lock className="w-4 h-4 text-slate-400 flex-shrink-0" />
            <span>
              L'identité (Nom, Prénom) et le CIN sont <strong>strictement verrouillés</strong> afin de garantir l'intégrité du référentiel.
            </span>
          </div>

          {/* Identité en lecture seule */}
          <div className="grid grid-cols-2 gap-3 p-3 bg-slate-100/70 rounded-xl border border-slate-200 text-xs">
            <div>
              <span className="text-slate-400 block uppercase font-bold text-[10px]">Locataire</span>
              <span className="font-semibold text-slate-800">{locataire.prenom} {locataire.nom}</span>
            </div>
            <div>
              <span className="text-slate-400 block uppercase font-bold text-[10px]">CIN Enregistré</span>
              <span className="font-mono font-semibold text-navy-800">{locataire.cin}</span>
            </div>
          </div>

          {/* Champs éditables : Email & Téléphone */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
              Nouvelle adresse Email <span className="text-red-500">*</span>
            </label>
            <input
              type="email"
              required
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              className="w-full px-3.5 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-navy-600"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
              Nouveau numéro de Téléphone <span className="text-red-500">*</span>
            </label>
            <input
              type="tel"
              required
              value={formData.telephone}
              onChange={(e) => setFormData({ ...formData, telephone: e.target.value })}
              className="w-full px-3.5 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-navy-600"
            />
          </div>

          {/* Boutons d'action */}
          <div className="pt-2 flex justify-end space-x-2.5">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-slate-300 text-slate-700 hover:bg-slate-100 rounded-lg text-xs font-semibold transition-colors"
            >
              Annuler
            </button>
            <button
              type="submit"
              className="px-5 py-2 bg-navy-700 hover:bg-navy-800 text-white rounded-lg text-xs font-semibold transition-colors shadow-sm inline-flex items-center space-x-1.5"
            >
              <Check className="w-3.5 h-3.5" />
              <span>Enregistrer les modifications</span>
            </button>
          </div>

        </form>

      </div>
    </div>
  );
}
