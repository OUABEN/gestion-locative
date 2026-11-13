import React, { useState } from 'react';
import { Search, Plus, Eye, Edit3, Trash2, Home, CheckCircle2, AlertCircle, Phone, Mail, CreditCard } from 'lucide-react';

export default function RepertoireLocataires({
  locataires,
  onOpenCreate,
  onOpenDetail,
  onOpenEdit,
  onOpenDelete
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatut, setFilterStatut] = useState('ALL'); // 'ALL' | 'ACTIF' | 'SANS_BAIL'

  // Filtrage combiné en temps réel
  const filteredLocataires = locataires.filter(loc => {
    const search = searchTerm.trim().toLowerCase();
    const matchSearch =
      loc.cin.toLowerCase().includes(search) ||
      loc.nom.toLowerCase().includes(search) ||
      loc.prenom.toLowerCase().includes(search) ||
      (loc.email && loc.email.toLowerCase().includes(search)) ||
      (loc.telephone && loc.telephone.includes(search));

    if (!matchSearch) return false;

    if (filterStatut === 'ACTIF') return loc.hasBailActif;
    if (filterStatut === 'SANS_BAIL') return !loc.hasBailActif;
    return true;
  });

  return (
    <div className="bg-white rounded-xl border border-slate-200 card-shadow overflow-hidden">
      
      {/* Barre d'outils supérieure : Recherche + Filtres + Bouton Nouveau */}
      <div className="p-4 sm:p-5 border-b border-slate-100 flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between bg-slate-50/50">
        
        {/* Recherche en temps réel */}
        <div className="relative flex-1 max-w-md">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Search className="w-4 h-4" />
          </div>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Rechercher par CIN (ex: AB123456), Nom, Prénom..."
            className="w-full pl-10 pr-4 py-2 bg-white text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-navy-600 focus:border-transparent transition-all placeholder:text-slate-400"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-xs text-slate-400 hover:text-slate-600"
            >
              Effacer
            </button>
          )}
        </div>

        {/* Filtres & Bouton d'action */}
        <div className="flex items-center space-x-2.5">
          <div className="inline-flex rounded-lg border border-slate-200 bg-white p-1 text-xs font-medium text-slate-600">
            <button
              onClick={() => setFilterStatut('ALL')}
              className={`px-3 py-1 rounded-md transition-colors ${
                filterStatut === 'ALL'
                  ? 'bg-navy-700 text-white font-semibold shadow-xs'
                  : 'hover:text-slate-900'
              }`}
            >
              Tous ({locataires.length})
            </button>
            <button
              onClick={() => setFilterStatut('ACTIF')}
              className={`px-3 py-1 rounded-md transition-colors ${
                filterStatut === 'ACTIF'
                  ? 'bg-emerald-600 text-white font-semibold shadow-xs'
                  : 'hover:text-emerald-700'
              }`}
            >
              Avec bail actif
            </button>
            <button
              onClick={() => setFilterStatut('SANS_BAIL')}
              className={`px-3 py-1 rounded-md transition-colors ${
                filterStatut === 'SANS_BAIL'
                  ? 'bg-amber-600 text-white font-semibold shadow-xs'
                  : 'hover:text-amber-700'
              }`}
            >
              Sans bail
            </button>
          </div>

          <button
            onClick={onOpenCreate}
            className="inline-flex items-center space-x-1.5 px-4 py-2 bg-navy-700 hover:bg-navy-800 text-white text-sm font-semibold rounded-lg shadow-sm transition-all focus:ring-2 focus:ring-navy-600 focus:ring-offset-1"
          >
            <Plus className="w-4 h-4" />
            <span>Nouveau locataire</span>
          </button>
        </div>

      </div>

      {/* Tableau des locataires */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-100/70 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
              <th className="py-3 px-4">CIN (Clé Métier)</th>
              <th className="py-3 px-4">Locataire</th>
              <th className="py-3 px-4">Contact (Tél / Email)</th>
              <th className="py-3 px-4">Statut Bail</th>
              <th className="py-3 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-sm">
            {filteredLocataires.length === 0 ? (
              <tr>
                <td colSpan="5" className="py-12 text-center text-slate-400">
                  <div className="flex flex-col items-center justify-center">
                    <AlertCircle className="w-8 h-8 text-slate-300 mb-2" />
                    <p className="font-medium text-slate-600">Aucun locataire trouvé</p>
                    <p className="text-xs text-slate-400 mt-1">
                      Essayez de modifier votre recherche ou ajoutez une nouvelle fiche.
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              filteredLocataires.map((loc) => {
                const initiales = `${loc.prenom[0] || ''}${loc.nom[0] || ''}`.toUpperCase();
                return (
                  <tr
                    key={loc.id}
                    className="hover:bg-slate-50/80 transition-colors group"
                  >
                    {/* CIN */}
                    <td className="py-3.5 px-4 font-mono font-semibold text-navy-700 whitespace-nowrap">
                      <span className="inline-flex items-center px-2.5 py-1 rounded bg-slate-100 text-slate-800 border border-slate-200 text-xs">
                        <CreditCard className="w-3.5 h-3.5 mr-1 text-slate-500" />
                        {loc.cin}
                      </span>
                    </td>

                    {/* Nom & Prénom avec avatar */}
                    <td className="py-3.5 px-4">
                      <div className="flex items-center space-x-3">
                        <div className="w-8 h-8 rounded-full bg-navy-100 text-navy-800 font-bold text-xs flex items-center justify-center flex-shrink-0 border border-navy-200">
                          {initiales}
                        </div>
                        <div>
                          <div className="font-semibold text-slate-800 leading-tight">
                            {loc.prenom} {loc.nom}
                          </div>
                          <div className="text-[11px] text-slate-400 font-mono mt-0.5">
                            ID: {loc.id.substring(0, 8)}...
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* Coordonnées */}
                    <td className="py-3.5 px-4">
                      <div className="flex flex-col space-y-0.5 text-xs">
                        <div className="flex items-center text-slate-700">
                          <Phone className="w-3 h-3 mr-1.5 text-slate-400 flex-shrink-0" />
                          <span>{loc.telephone || 'Non renseigné'}</span>
                        </div>
                        <div className="flex items-center text-slate-500">
                          <Mail className="w-3 h-3 mr-1.5 text-slate-400 flex-shrink-0" />
                          <span className="truncate max-w-[200px]">{loc.email}</span>
                        </div>
                      </div>
                    </td>

                    {/* Statut Bail */}
                    <td className="py-3.5 px-4">
                      {loc.hasBailActif ? (
                        <div>
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5"></span>
                            Bail Actif
                          </span>
                          <div className="text-[11px] text-slate-500 mt-1 flex items-center">
                            <Home className="w-3 h-3 mr-1 text-slate-400" />
                            {loc.bailInfo?.appartement || 'Bien loué'}
                          </div>
                        </div>
                      ) : (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200">
                          <span className="w-1.5 h-1.5 rounded-full bg-amber-400 mr-1.5"></span>
                          Sans bail actif
                        </span>
                      )}
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <div className="inline-flex items-center space-x-1">
                        <button
                          onClick={() => onOpenDetail(loc)}
                          title="Consulter la fiche détaillée & flux Quittances"
                          className="p-1.5 text-slate-500 hover:text-navy-700 hover:bg-slate-100 rounded-md transition-colors"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => onOpenEdit(loc)}
                          title="Modifier les coordonnées (Email / Tél)"
                          className="p-1.5 text-slate-500 hover:text-sky-600 hover:bg-sky-50 rounded-md transition-colors"
                        >
                          <Edit3 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => onOpenDelete(loc)}
                          title="Supprimer la fiche"
                          className="p-1.5 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-md transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>

                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pied de tableau avec total */}
      <div className="px-5 py-3 bg-slate-50 border-t border-slate-100 text-xs text-slate-500 flex justify-between items-center">
        <span>
          Affichage de <span className="font-semibold text-slate-700">{filteredLocataires.length}</span> sur{' '}
          <span className="font-semibold text-slate-700">{locataires.length}</span> fiches
        </span>
        <span className="text-[11px] text-slate-400">
          Clé primaire : UUID v4 • Clé métier unique : CIN Marocain
        </span>
      </div>

    </div>
  );
}
