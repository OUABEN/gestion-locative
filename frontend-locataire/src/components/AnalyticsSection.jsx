import React, { useState } from 'react';
import { TrendingUp, BarChart3, PieChart, ArrowUpRight, Calendar, Sparkles } from 'lucide-react';
import { monthlyRevenueData, cityDistributionData } from '../data/locativeData';

export default function AnalyticsSection() {
  const [selectedPeriod, setSelectedPeriod] = useState('2026');

  // SVG Area Chart Calculations
  const maxLoyer = Math.max(...monthlyRevenueData.map(d => d.loyers));
  const minLoyer = 250000;
  const svgWidth = 640;
  const svgHeight = 220;
  const paddingX = 40;
  const paddingY = 30;

  const points = monthlyRevenueData.map((d, i) => {
    const x = paddingX + (i * (svgWidth - 2 * paddingX)) / (monthlyRevenueData.length - 1);
    const normalizedY = (d.loyers - minLoyer) / (maxLoyer - minLoyer);
    const y = svgHeight - paddingY - normalizedY * (svgHeight - 2 * paddingY);
    return { x, y, ...d };
  });

  const pathD = points.reduce((acc, p, i) => {
    return i === 0 ? `M ${p.x} ${p.y}` : `${acc} L ${p.x} ${p.y}`;
  }, '');

  const areaD = `${pathD} L ${points[points.length - 1].x} ${svgHeight - paddingY} L ${points[0].x} ${svgHeight - paddingY} Z`;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
      
      {/* 1. Primary 2-Column Wide Midnight Black Chart Card (#0B0B0C) */}
      <div className="lg:col-span-2 rounded-card bg-midnight p-6 border border-midnight-border shadow-midnight-card relative overflow-hidden flex flex-col justify-between">
        
        {/* Subtle lime glow in background */}
        <div className="absolute top-0 right-0 w-80 h-80 bg-lime/5 rounded-full blur-3xl pointer-events-none"></div>

        {/* Card Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6 relative z-10">
          <div>
            <div className="flex items-center space-x-2">
              <span className="w-2.5 h-2.5 rounded-full bg-lime shadow-lime-sm"></span>
              <h3 className="text-white font-extrabold text-base tracking-tight">
                Flux Mensuel des Loyers & Règlements
              </h3>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Évolution des loyers sous contrat actif et taux d'encaissement (DH)
            </p>
          </div>

          <div className="flex items-center space-x-2">
            <span className="text-[11px] font-mono text-lime bg-midnight-elevated px-3 py-1 rounded-full border border-midnight-border">
              +12.4% vs N-1
            </span>
            <div className="bg-midnight-elevated rounded-button p-1 border border-midnight-border flex text-xs">
              <button
                onClick={() => setSelectedPeriod('2026')}
                className={`px-3 py-1 rounded-lg font-bold transition-all ${
                  selectedPeriod === '2026' ? 'bg-lime text-midnight' : 'text-slate-400 hover:text-white'
                }`}
              >
                Année 2026
              </button>
            </div>
          </div>
        </div>

        {/* Monthly Summary KPI Banner */}
        <div className="grid grid-cols-3 gap-3 p-3.5 rounded-xl bg-midnight-elevated border border-midnight-border mb-4 text-xs font-mono relative z-10">
          <div>
            <span className="text-slate-400 block text-[10px] uppercase">Loyer Global / Mois</span>
            <span className="text-base font-black text-lime">312 000 DH</span>
          </div>
          <div>
            <span className="text-slate-400 block text-[10px] uppercase">Règlements Confirmés</span>
            <span className="text-base font-black text-white">312 000 DH</span>
          </div>
          <div>
            <span className="text-slate-400 block text-[10px] uppercase">Taux de Recouvrement</span>
            <span className="text-base font-black text-emerald-400">100 %</span>
          </div>
        </div>

        {/* SVG Area Chart with Lime Glowing Stroke */}
        <div className="relative w-full h-[220px] overflow-hidden select-none">
          <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="w-full h-full overflow-visible">
            <defs>
              <linearGradient id="limeGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#C6F432" stopOpacity="0.35" />
                <stop offset="100%" stopColor="#C6F432" stopOpacity="0.0" />
              </linearGradient>
            </defs>

            {/* Horizontal Grid Lines */}
            {[0.25, 0.5, 0.75, 1].map((ratio, idx) => {
              const y = svgHeight - paddingY - ratio * (svgHeight - 2 * paddingY);
              return (
                <line
                  key={idx}
                  x1={paddingX}
                  y1={y}
                  x2={svgWidth - paddingX}
                  y2={y}
                  stroke="#26262C"
                  strokeDasharray="4 4"
                />
              );
            })}

            {/* Area Fill */}
            <path d={areaD} fill="url(#limeGradient)" />

            {/* Line Stroke with Lime Glow */}
            <path
              d={pathD}
              fill="none"
              stroke="#C6F432"
              strokeWidth="3.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="drop-shadow-[0_0_12px_rgba(198,244,50,0.6)]"
            />

            {/* Data Points */}
            {points.map((p, i) => (
              <g key={i} className="cursor-pointer group">
                <circle
                  cx={p.x}
                  cy={p.y}
                  r="5"
                  className="fill-midnight stroke-lime stroke-[3] group-hover:scale-125 transition-all"
                />
                {/* X Axis Label */}
                <text
                  x={p.x}
                  y={svgHeight - 10}
                  textAnchor="middle"
                  fill="#71717A"
                  fontSize="10"
                  fontFamily="Inter"
                  fontWeight="600"
                >
                  {p.month}
                </text>
              </g>
            ))}
          </svg>
        </div>

        {/* Footer info */}
        <div className="mt-3 pt-3 border-t border-midnight-border/70 flex items-center justify-between text-[11px] text-slate-400 font-mono">
          <span>Source : Module Loyers & Règlements (Ayoub)</span>
          <span className="text-lime font-bold">Synchronisation Quittances OK</span>
        </div>

      </div>

      {/* 2. 1-Column Pure White Donut Chart Card (#FFFFFF) */}
      <div className="rounded-card bg-white p-6 border border-slate-200/90 shadow-card-soft flex flex-col justify-between">
        
        <div>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-midnight font-extrabold text-base tracking-tight">
              Répartition par Ville
            </h3>
            <span className="bg-slate-100 text-slate-700 text-[10px] font-bold px-2 py-0.5 rounded-full font-mono">
              Portefeuille
            </span>
          </div>

          <p className="text-xs text-slate-500 mb-6">
            Concentration des biens immobiliers et baux loués au Maroc
          </p>

          {/* Donut Chart Visualization */}
          <div className="relative w-44 h-44 mx-auto my-2 flex items-center justify-center">
            <svg viewBox="0 0 100 100" className="w-full h-full transform -rotate-90">
              {/* Casablanca (50%) - Lime Green */}
              <circle
                cx="50"
                cy="50"
                r="38"
                fill="none"
                stroke="#C6F432"
                strokeWidth="14"
                strokeDasharray="119.38 119.38"
                strokeDashoffset="0"
                className="transition-all hover:opacity-90 cursor-pointer"
              />
              {/* Rabat (25%) - Midnight Black */}
              <circle
                cx="50"
                cy="50"
                r="38"
                fill="none"
                stroke="#0B0B0C"
                strokeWidth="14"
                strokeDasharray="59.69 179.07"
                strokeDashoffset="-119.38"
                className="transition-all hover:opacity-90 cursor-pointer"
              />
              {/* Marrakech (17%) - Slate Blue */}
              <circle
                cx="50"
                cy="50"
                r="38"
                fill="none"
                stroke="#5F92CA"
                strokeWidth="14"
                strokeDasharray="40.59 198.17"
                strokeDashoffset="-179.07"
                className="transition-all hover:opacity-90 cursor-pointer"
              />
              {/* Tanger (8%) - Charcoal */}
              <circle
                cx="50"
                cy="50"
                r="38"
                fill="none"
                stroke="#26262C"
                strokeWidth="14"
                strokeDasharray="19.1 219.66"
                strokeDashoffset="-219.66"
                className="transition-all hover:opacity-90 cursor-pointer"
              />
            </svg>

            {/* Inner Content */}
            <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
              <span className="text-2xl font-black text-midnight font-sans">48</span>
              <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Baux Totaux</span>
            </div>
          </div>
        </div>

        {/* City Legend Breakdown */}
        <div className="space-y-2 mt-4 pt-4 border-t border-slate-100 text-xs">
          {cityDistributionData.map((item, idx) => (
            <div key={idx} className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <span
                  className="w-3 h-3 rounded-full flex-shrink-0"
                  style={{ backgroundColor: item.color }}
                ></span>
                <span className="font-semibold text-slate-700">{item.ville}</span>
              </div>
              <div className="flex items-center space-x-2 font-mono">
                <span className="text-slate-500">{item.count} biens</span>
                <span className="font-bold text-midnight bg-slate-100 px-1.5 py-0.5 rounded text-[11px]">
                  {item.percent}%
                </span>
              </div>
            </div>
          ))}
        </div>

      </div>

    </div>
  );
}
