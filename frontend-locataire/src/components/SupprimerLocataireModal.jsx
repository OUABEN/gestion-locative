import React from 'react';
import { X, Trash2, AlertOctagon, ShieldAlert, Ban, AlertTriangle } from 'lucide-react';

export default function SupprimerLocataireModal({ isOpen, onClose, onConfirmDelete, locataire }) {
  if (!isOpen || !locataire) return null;

  const hasActiveLease = locataire.hasBailActif;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-md w-full overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        
        {/* En-tête */}
        <div className={`px-6 py-4 text-white flex items-center justify-between ${
          hasActiveLease ? 'bg-amber-700' : 'bg-red-700'
        }`}>
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-lg bg-black/20 flex items-center justify-center text-white">
              {hasActiveLease ? <Ban className="w-5 h-5" /> : <Trash2 className="w-5 h-5" />}
            </div>
            <div>
              <h2 className="font-bold text-base tracking-tight">
                {hasActiveLease ? 'Suppression refusée (F-LOC-05)' : 'Confirmation de suppression (F-LOC-05)'}
              </h2>
              <p className="text-xs text-white/80">Contrôle d'intégrité relationnelle</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-white/80 hover:text-white p-1 rounded-lg hover:bg-black/20 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Corps de la modale */}
        <div className="p-6 text-sm">
          
          {hasActiveLease ? (
            /* CAS A : BLOQUAGE CAR BAIL ACTIF */
            <div className="space-y-4">
              <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl text-amber-900 flex items-start space-x-3">
                <AlertOctagon className="w-6 h-6 text-amber-600 flex-shrink-0 mt-0.5" />
                <div className="text-xs">
                  <h4 className="font-bold text-sm text-amber-950 mb-1">
                    Suppression impossible : Un bail actif est actuellement rattaché à ce locataire
                  </h4>
                  <p className="text-amber-800 leading-relaxed">
                    Conformément aux règles d'intégrité du système de gestion locative (diagramme de classes p. 4), une fiche locataire liée à un contrat de bail actif ne peut pas être supprimée.
                  </p>
                </div>
              </div>

              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1">
                <div className="font-semibold text-slate-700">Bien concerné :</div>
                <div className="text-slate-600 font-medium">{locataire.bailInfo?.appartement} — {locataire.bailInfo?.residence}</div>
                <div className="text-slate-400 text-[11px]">Réf. Bien : {locataire.bailInfo?.bienId}</div>
              </div>

              <p className="text-xs text-slate-500 italic">
                👉 Pour supprimer ce locataire, vous devez d'abord résilier ou clôturer son bail en cours dans le module <strong>Gestion des biens</strong>.
              </p>
            </div>
          ) : (
            /* CAS B : SUPPRESSION AUTORISÉE */
            <div className="space-y-3">
              <div className="p-3.5 bg-red-50 border border-red-200 rounded-xl text-red-900 flex items-start space-x-3">
                <AlertTriangle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div className="text-xs">
                  <p className="font-semibold text-red-950">
                    Êtes-vous sûr de vouloir supprimer définitivement la fiche de ce locataire ?
                  </p>
                  <p className="text-red-700 mt-1">
                    Cette action effacera le référentiel ainsi que l'ensemble des pièces justificatives associées.
                  </p>
                </div>
              </div>

              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1">
                <div><span className="text-slate-400">Locataire : </span><span className="font-semibold text-slate-800">{locataire.prenom} {locataire.nom}</span></div>
                <div><span className="text-slate-400">CIN : </span><span className="font-mono font-semibold text-navy-800">{locataire.cin}</span></div>
                <div><span className="text-slate-400">UUID : </span><span className="font-mono text-[11px] text-slate-500">{locataire.id}</span></div>
              </div>
            </div>
          )}

        </div>

        {/* Pied */}
        <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-200 flex justify-end space-x-2.5">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-slate-300 text-slate-700 hover:bg-slate-100 rounded-lg text-xs font-semibold transition-colors"
          >
            {hasActiveLease ? 'Compris / Fermer' : 'Annuler'}
          </button>

          {!hasActiveLease && (
            <button
              onClick={() => {
                onConfirmDelete(locataire.id);
                onClose();
              }}
              className="px-5 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-semibold transition-colors shadow-sm inline-flex items-center space-x-1.5"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Confirmer la suppression</span>
            </button>
          )}
        </div>

      </div>
    </div>
  );
}
