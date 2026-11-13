import React from 'react';
import { 
  Users, 
  FileCheck2, 
  FileX, 
  TrendingUp, 
  ShieldCheck, 
  Coins, 
  ArrowUpRight 
} from 'lucide-react';

export default function KpiGrid({ locataires }) {
  const total = locataires.length;
  const bauxActifs = locataires.filter(l => l.statutBail === 'ACTIF').length;
  const bauxTermines = locataires.filter(l => l.statutBail === 'TERMINE').length;
  
  // Totaux calculés en Dirhams
  const totalLoyers = locataires
    .filter(l => l.statutBail === 'ACTIF')
    .reduce((acc, curr) => acc + (curr.loyerMensuel || 0), 0);

  const totalDepots = locataires
    .filter(l => l.statutBail === 'ACTIF')
    .reduce((acc, curr) => acc + (curr.depotGarantie || 0), 0);

  const kpis = [
    // 1. Total Locataires (Pure White Card)
    {
      title: "Total Locataires",
      value: `${total}`,
      unit: "fiches",
      subtitle: "Référentiel centralisé certifié",
      icon: Users,
      isDark: false,
      progress: 100,
      badge: "+14% vs N-1",
      badgeType: "positive"
    },
    // 2. Baux Actifs (Midnight Black Card)
    {
      title: "Baux Actifs",
      value: `${bauxActifs}`,
      unit: "en cours",
      subtitle: "Contrats valides avec loyers perçus",
      icon: FileCheck2,
      isDark: true,
      progress: (bauxActifs / Math.max(1, total)) * 100,
      badge: "Priorité #1",
      badgeType: "lime"
    },
    // 3. Baux Terminés (Pure White Card)
    {
      title: "Baux Terminés",
      value: `${bauxTermines}`,
      unit: "archivés",
      subtitle: "Clôturés avec état des lieux signé",
      icon: FileX,
      isDark: false,
      progress: (bauxTermines / Math.max(1, total)) * 100,
      badge: "Conformes",
      badgeType: "neutral"
    },
    // 4. Taux de Rétention (Midnight Black Card)
    {
      title: "Taux de Rétention",
      value: "94.8",
      unit: "%",
      subtitle: "Renouvellement tacite des baux",
      icon: TrendingUp,
      isDark: true,
      progress: 94.8,
      badge: "+3.2%",
      badgeType: "lime"
    },
    // 5. Dépôts de Garantie Cumulés (Pure White Card)
    {
      title: "Dépôts de Garantie",
      value: totalDepots.toLocaleString('fr-FR'),
      unit: "DH",
      subtitle: "Séquestre et cautions sécurisées",
      icon: ShieldCheck,
      isDark: false,
      progress: 88,
      badge: "Garanti 100%",
      badgeType: "positive"
    },
    // 6. Loyers sous Contrat (Midnight Black Card)
    {
      title: "Loyers sous Contrat",
      value: totalLoyers.toLocaleString('fr-FR'),
      unit: "DH / mois",
      subtitle: "Volume d'encaissement récurrent",
      icon: Coins,
      isDark: true,
      progress: 92,
      badge: "Flux Actif",
      badgeType: "lime"
    }
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 mb-8">
      {kpis.map((kpi, idx) => {
        const Icon = kpi.icon;

        return (
          <div
            key={idx}
            className={`rounded-card p-6 border transition-all duration-200 group relative overflow-hidden ${
              kpi.isDark
                ? 'bg-midnight text-white border-midnight-border shadow-midnight-card hover:border-lime/40'
                : 'bg-white text-midnight border-slate-200/90 shadow-card-soft hover:shadow-md hover:border-slate-300'
            }`}
          >
            {/* Top row: Icon box & Badge */}
            <div className="flex items-center justify-between mb-4">
              {/* 44x44px Lime Green icon box */}
              <div className="w-[44px] h-[44px] rounded-[14px] bg-lime flex items-center justify-center text-midnight shadow-lime-sm flex-shrink-0">
                <Icon className="w-5 h-5 stroke-[2.2]" />
              </div>

              {/* Badge */}
              <span
                className={`text-[10px] font-extrabold px-2.5 py-1 rounded-full font-mono uppercase tracking-wide flex items-center space-x-1 ${
                  kpi.badgeType === 'lime'
                    ? 'bg-lime text-midnight'
                    : kpi.badgeType === 'positive'
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-slate-100 text-slate-700'
                }`}
              >
                <span>{kpi.badge}</span>
                {kpi.badgeType === 'positive' && <ArrowUpRight className="w-3 h-3 ml-0.5" />}
              </span>
            </div>

            {/* Label */}
            <p className={`text-xs font-bold uppercase tracking-wider ${
              kpi.isDark ? 'text-slate-400' : 'text-slate-500'
            }`}>
              {kpi.title}
            </p>

            {/* Large Figure (30px bold) in DH */}
            <div className="flex items-baseline space-x-2 mt-1.5">
              <span className={`text-[30px] font-black tracking-tight leading-none tabular-nums font-sans ${
                kpi.isDark ? 'text-white' : 'text-midnight'
              }`}>
                {kpi.value}
              </span>
              <span className={`text-xs font-mono font-extrabold ${
                kpi.isDark ? 'text-lime' : 'text-slate-500'
              }`}>
                {kpi.unit}
              </span>
            </div>

            <p className={`text-[11px] mt-2 truncate ${
              kpi.isDark ? 'text-slate-400' : 'text-slate-500'
            }`}>
              {kpi.subtitle}
            </p>

            {/* Horizontal Lime Progress Bar */}
            <div className={`w-full h-1.5 rounded-full mt-4 overflow-hidden ${
              kpi.isDark ? 'bg-midnight-elevated' : 'bg-slate-100'
            }`}>
              <div
                className="h-full rounded-full bg-lime transition-all duration-1000 shadow-lime-sm"
                style={{ width: `${Math.min(100, Math.max(10, kpi.progress))}%` }}
              ></div>
            </div>

          </div>
        );
      })}
    </div>
  );
}
