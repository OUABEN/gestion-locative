import React, { useState } from 'react';
import { 
  Search, 
  Eye, 
  Edit3, 
  Trash2, 
  FileText, 
  Home, 
  Phone, 
  Mail, 
  MapPin, 
  Calendar, 
  Coins, 
  ShieldCheck, 
  Filter, 
  Plus, 
  ArrowUpDown 
} from 'lucide-react';

export default function TableView({
  locataires,
  onOpenCreate,
  onOpenDetail,
  onOpenQuittance,
  onOpenEdit,
  onOpenDelete
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatut, setFilterStatut] = useState('ALL'); // 'ALL' | 'ACTIF' | 'TERMINE'
  const [filterVille, setFilterVille] = useState('ALL'); // 'ALL' | 'Casablanca' | 'Rabat' | 'Marrakech'

  // Filtrage combiné en temps réel
  const filteredData = locataires.filter(item => {
    const s = searchTerm.toLowerCase().trim();
    const matchSearch =
      item.cin.toLowerCase().includes(s) ||
      item.nom.toLowerCase().includes(s) ||
      item.prenom.toLowerCase().includes(s) ||
      (item.bien?.titre && item.bien.titre.toLowerCase().includes(s)) ||
      (item.bien?.ville && item.bien.ville.toLowerCase().includes(s)) ||
      (item.email && item.email.toLowerCase().includes(s));

    if (!matchSearch) return false;

    if (filterStatut !== 'ALL' && item.statutBail !== filterStatut) return false;
    if (filterVille !== 'ALL' && item.bien?.ville !== filterVille) return false;

    return true;
  });

  return (
    <div className="rounded-card bg-white border border-slate-200/90 shadow-card-soft overflow-hidden select-none">
      
      {/* 1. Header Toolbar : Search + Filter Pills + Action CTA */}
      <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex flex-col lg:flex-row gap-4 items-stretch lg:items-center justify-between">
        
        {/* Search Bar */}
        <div className="relative flex-1 max-w-lg">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Search className="w-4 h-4" />
          </div>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Rechercher par CIN (ex: AB123456), nom, bien ou ville..."
            className="w-full pl-10 pr-4 py-2.5 bg-white text-xs border border-slate-200 rounded-button focus:outline-none focus:ring-2 focus:ring-midnight focus:border-transparent transition-all placeholder:text-slate-400 shadow-2xs"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-xs text-slate-400 hover:text-midnight font-bold"
            >
              ✕
            </button>
          )}
        </div>

        {/* Pill Filters & New Lease Button */}
        <div className="flex flex-wrap items-center gap-2.5">
          
          {/* Status Pills */}
          <div className="inline-flex rounded-full bg-slate-200/80 p-1 text-xs font-bold text-slate-600">
            <button
              onClick={() => setFilterStatut('ALL')}
              className={`px-3 py-1 rounded-full transition-all ${
                filterStatut === 'ALL'
                  ? 'bg-midnight text-white shadow-xs'
                  : 'hover:text-midnight'
              }`}
            >
              Tous ({locataires.length})
            </button>
            <button
              onClick={() => setFilterStatut('ACTIF')}
              className={`px-3 py-1 rounded-full transition-all flex items-center space-x-1 ${
                filterStatut === 'ACTIF'
                  ? 'bg-lime text-midnight font-black shadow-lime-sm'
                  : 'hover:text-midnight'
              }`}
            >
              <span>ACTIF</span>
            </button>
            <button
              onClick={() => setFilterStatut('TERMINE')}
              className={`px-3 py-1 rounded-full transition-all ${
                filterStatut === 'TERMINE'
                  ? 'bg-midnight text-white font-bold'
                  : 'hover:text-midnight'
              }`}
            >
              TERMINÉ
            </button>
          </div>

          {/* City Filter Dropdown */}
          <select
            value={filterVille}
            onChange={(e) => setFilterVille(e.target.value)}
            className="px-3 py-2 bg-white border border-slate-200 rounded-button text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-midnight shadow-2xs"
          >
            <option value="ALL">Toutes les villes</option>
            <option value="Casablanca">Casablanca</option>
            <option value="Rabat">Rabat</option>
            <option value="Marrakech">Marrakech</option>
            <option value="Tanger">Tanger</option>
          </select>

          {/* Button Nouveau */}
          <button
            onClick={onOpenCreate}
            className="px-4 py-2 bg-lime hover:bg-lime-hover text-midnight text-xs font-extrabold rounded-button shadow-lime-sm hover:shadow-lime-glow transition-all flex items-center space-x-1.5"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            <span>Nouveau contrat</span>
          </button>

        </div>

      </div>

      {/* 2. Modern Table with 8 Strict Columns */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-xs">
          <thead>
            <tr className="bg-slate-100/80 border-b border-slate-200/80 text-[10px] font-extrabold text-slate-500 uppercase tracking-wider">
              <th className="py-3.5 px-4">Nom & Prénom</th>
              <th className="py-3.5 px-4">CIN Marocain</th>
              <th className="py-3.5 px-4">Contact (Tél & Email)</th>
              <th className="py-3.5 px-4">Bien Rattaché & Ville</th>
              <th className="py-3.5 px-4 text-right">Loyer Mensuel</th>
              <th className="py-3.5 px-4 text-right">Dépôt Garantie</th>
              <th className="py-3.5 px-4">Période du Bail</th>
              <th className="py-3.5 px-4 text-center">Statut</th>
              <th className="py-3.5 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-slate-700">
            {filteredData.length === 0 ? (
              <tr>
                <td colSpan="9" className="py-12 text-center text-slate-400">
                  <div className="flex flex-col items-center justify-center space-y-2">
                    <Search className="w-6 h-6 text-slate-300" />
                    <p className="font-semibold text-slate-600">Aucun locataire ou bail ne correspond aux critères.</p>
                    <p className="text-[11px] text-slate-400">Essayez de réinitialiser vos filtres de recherche.</p>
                  </div>
                </td>
              </tr>
            ) : (
              filteredData.map((item) => {
                const isActif = item.statutBail === 'ACTIF';
                const initiales = `${item.prenom[0] || ''}${item.nom[0] || ''}`.toUpperCase();

                return (
                  <tr
                    key={item.id}
                    className="hover:bg-slate-50/80 transition-colors group"
                  >
                    {/* 1. Nom & Prénom */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="flex items-center space-x-3">
                        <div className="w-8 h-8 rounded-full bg-midnight text-lime font-mono font-bold text-xs flex items-center justify-center flex-shrink-0 border border-lime/30">
                          {initiales}
                        </div>
                        <div>
                          <div className="font-extrabold text-midnight text-xs group-hover:text-black">
                            {item.prenom} {item.nom}
                          </div>
                          <div className="text-[10px] text-slate-400 font-mono">
                            ID: {item.id.substring(0, 11)}...
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* 2. CIN */}
                    <td className="py-3.5 px-4 whitespace-nowrap font-mono font-bold">
                      <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-slate-100 text-midnight border border-slate-200 text-xs">
                        {item.cin}
                      </span>
                    </td>

                    {/* 3. Téléphone & Email */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="space-y-0.5">
                        <div className="flex items-center text-slate-800 font-medium">
                          <Phone className="w-3 h-3 mr-1.5 text-slate-400 flex-shrink-0" />
                          <span>{item.telephone}</span>
                        </div>
                        <div className="flex items-center text-slate-500 text-[11px]">
                          <Mail className="w-3 h-3 mr-1.5 text-slate-400 flex-shrink-0" />
                          <span className="truncate max-w-[160px]">{item.email}</span>
                        </div>
                      </div>
                    </td>

                    {/* 4. Bien rattaché (Adresse & Ville) */}
                    <td className="py-3.5 px-4">
                      <div className="space-y-0.5 max-w-[200px]">
                        <div className="font-semibold text-slate-800 truncate flex items-center">
                          <Home className="w-3 h-3 mr-1 text-slate-400 flex-shrink-0" />
                          <span className="truncate">{item.bien?.titre}</span>
                        </div>
                        <div className="text-[11px] text-slate-500 flex items-center truncate">
                          <MapPin className="w-2.5 h-2.5 mr-1 text-slate-400 flex-shrink-0" />
                          <span className="truncate">{item.bien?.ville} • {item.bien?.adresse}</span>
                        </div>
                      </div>
                    </td>

                    {/* 5. Montant du loyer (DH) */}
                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <div className="font-black text-midnight text-xs tabular-nums font-mono">
                        {item.loyerMensuel?.toLocaleString('fr-FR')} DH
                      </div>
                      <div className="text-[10px] text-slate-400 font-mono">par mois</div>
                    </td>

                    {/* 6. Dépôt de garantie (DH) */}
                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <div className="font-bold text-slate-700 text-xs tabular-nums font-mono">
                        {item.depotGarantie?.toLocaleString('fr-FR')} DH
                      </div>
                      <div className="text-[10px] text-emerald-600 font-semibold">2 mois de caution</div>
                    </td>

                    {/* 7. Dates (Début / Fin) */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="text-[11px] font-mono text-slate-600 flex items-center">
                        <Calendar className="w-3 h-3 mr-1 text-slate-400" />
                        <span>{item.dateDebut} → {item.dateFin}</span>
                      </div>
                    </td>

                    {/* 8. Statut du bail (ACTIF / TERMINE) */}
                    <td className="py-3.5 px-4 text-center whitespace-nowrap">
                      {isActif ? (
                        <span className="inline-flex items-center px-3 py-1 rounded-full text-[11px] font-black bg-lime text-midnight shadow-lime-sm font-mono tracking-wider">
                          <span className="w-1.5 h-1.5 rounded-full bg-midnight mr-1.5"></span>
                          ACTIF
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-3 py-1 rounded-full text-[11px] font-bold bg-midnight text-white border border-midnight-border font-mono tracking-wider">
                          <span className="w-1.5 h-1.5 rounded-full bg-lime mr-1.5"></span>
                          TERMINE
                        </span>
                      )}
                    </td>

                    {/* 9. Actions */}
                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <div className="inline-flex items-center space-x-1">
                        <button
                          onClick={() => onOpenDetail(item)}
                          title="Consulter la fiche complète"
                          className="p-1.5 rounded-lg text-slate-500 hover:text-midnight hover:bg-slate-100 transition-colors"
                        >
                          <Eye className="w-4 h-4" />
                        </button>

                        <button
                          onClick={() => onOpenQuittance(item)}
                          title="Interopérabilité : Flux Quittance JSON"
                          className="p-1.5 rounded-lg text-lime-dark hover:text-midnight hover:bg-lime/20 transition-colors"
                        >
                          <FileText className="w-4 h-4" />
                        </button>

                        <button
                          onClick={() => onOpenEdit(item)}
                          title="Modifier les coordonnées"
                          className="p-1.5 rounded-lg text-slate-500 hover:text-sky-600 hover:bg-sky-50 transition-colors"
                        >
                          <Edit3 className="w-4 h-4" />
                        </button>

                        <button
                          onClick={() => onOpenDelete(item)}
                          title={isActif ? "Suppression bloquée (Bail actif)" : "Supprimer"}
                          className={`p-1.5 rounded-lg transition-colors ${
                            isActif 
                              ? 'text-slate-300 hover:text-amber-600 hover:bg-amber-50 cursor-pointer' 
                              : 'text-slate-500 hover:text-coral hover:bg-coral-50'
                          }`}
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

      {/* 3. Table Footer */}
      <div className="px-5 py-3.5 bg-slate-50 border-t border-slate-100 text-xs text-slate-500 flex flex-col sm:flex-row justify-between items-center gap-2">
        <span className="font-mono">
          Affichage de <strong className="text-midnight">{filteredData.length}</strong> sur <strong className="text-midnight">{locataires.length}</strong> baux sous contrat
        </span>
        <div className="flex items-center space-x-3 text-[11px] font-mono">
          <span className="flex items-center"><span className="w-2 h-2 rounded-full bg-lime mr-1"></span> ACTIF</span>
          <span className="flex items-center"><span className="w-2 h-2 rounded-full bg-midnight mr-1"></span> TERMINÉ</span>
          <span className="text-slate-400">| Devise : Dirham Marocain (DH)</span>
        </div>
      </div>

    </div>
  );
}
