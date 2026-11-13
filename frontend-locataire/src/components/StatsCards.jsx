import React from 'react';
import { Users, FileCheck2, UserCheck, ShieldAlert } from 'lucide-react';

export default function StatsCards({ locataires }) {
  const total = locataires.length;
  const bauxActifs = locataires.filter(l => l.hasBailActif).length;
  const sansBail = total - bauxActifs;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      
      {/* Total Locataires */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 card-shadow flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Référentiel Fiches</p>
          <h3 className="text-2xl font-bold text-navy-700 mt-1">{total}</h3>
          <p className="text-[11px] text-slate-400 mt-0.5">Locataires enregistrés</p>
        </div>
        <div className="w-12 h-12 rounded-xl bg-navy-50 flex items-center justify-center text-navy-700 border border-navy-100">
          <Users className="w-6 h-6" />
        </div>
      </div>

      {/* Baux actifs */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 card-shadow flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-emerald-600">Baux en cours</p>
          <h3 className="text-2xl font-bold text-emerald-700 mt-1">{bauxActifs}</h3>
          <p className="text-[11px] text-slate-400 mt-0.5">Biens occupés rattachés</p>
        </div>
        <div className="w-12 h-12 rounded-xl bg-emerald-50 flex items-center justify-center text-emerald-600 border border-emerald-100">
          <FileCheck2 className="w-6 h-6" />
        </div>
      </div>

      {/* Sans bail */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 card-shadow flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-amber-600">Sans bail actif</p>
          <h3 className="text-2xl font-bold text-amber-700 mt-1">{sansBail}</h3>
          <p className="text-[11px] text-slate-400 mt-0.5">Candidatures en attente</p>
        </div>
        <div className="w-12 h-12 rounded-xl bg-amber-50 flex items-center justify-center text-amber-600 border border-amber-100">
          <UserCheck className="w-6 h-6" />
        </div>
      </div>

      {/* Règle CIN */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 card-shadow flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-sky-600">Intégrité Clé Métier</p>
          <h3 className="text-2xl font-bold text-sky-700 mt-1">100 %</h3>
          <p className="text-[11px] text-slate-400 mt-0.5">Unicité absolue CIN vérifiée</p>
        </div>
        <div className="w-12 h-12 rounded-xl bg-sky-50 flex items-center justify-center text-sky-600 border border-sky-100">
          <ShieldAlert className="w-6 h-6" />
        </div>
      </div>

    </div>
  );
}
